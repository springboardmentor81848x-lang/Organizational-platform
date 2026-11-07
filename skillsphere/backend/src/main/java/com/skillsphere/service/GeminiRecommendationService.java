package com.skillsphere.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.skillsphere.dto.RecommendationRequest;
import com.skillsphere.dto.RecommendationRequest.SkillResult;
import com.skillsphere.dto.RecommendationResponse;
import com.skillsphere.dto.RecommendationResponse.Course;
import com.skillsphere.entity.Skill;
import com.skillsphere.exception.ApiException;
import com.skillsphere.repository.CourseRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Asks Google Gemini to select and rank real courses from the integrated database catalogue
 * based on an employee's assessment results and skill gaps.
 * All course recommendations are validated against the PostgreSQL course repository.
 * If Gemini is not configured or fails, a catalogue-backed rule-based fallback is used.
 */
@Service
public class GeminiRecommendationService {

    private static final Logger log = LoggerFactory.getLogger(GeminiRecommendationService.class);
    private static final String ENDPOINT = "https://generativelanguage.googleapis.com/v1beta/models/%s:generateContent";
    private static final long MIN_INTERVAL_MS = 4_000;
    private static final Set<String> PRIORITIES = Set.of("High", "Medium", "Low");

    private final ObjectMapper mapper;
    private final CourseRepository courseRepository;
    private final HttpClient http;
    private final String apiKey;
    private final String model;
    private final Duration timeout;
    private final Map<String, Long> lastCall = new ConcurrentHashMap<>();

    public GeminiRecommendationService(ObjectMapper mapper,
                                       CourseRepository courseRepository,
                                       @Value("${app.gemini.api-key:}") String apiKey,
                                       @Value("${app.gemini.model:gemini-2.5-flash}") String model,
                                       @Value("${app.gemini.timeout-seconds:45}") long timeoutSeconds) {
        this.mapper = mapper;
        this.courseRepository = courseRepository;
        this.apiKey = apiKey == null ? "" : apiKey.trim();
        this.model = model;
        this.timeout = Duration.ofSeconds(timeoutSeconds);
        this.http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
    }

    public RecommendationResponse recommend(String userKey, RecommendationRequest req) {
        throttle(userKey);
        List<com.skillsphere.entity.Course> allCourses = courseRepository.findAll();

        if (apiKey.isEmpty()) {
            log.warn("GEMINI_API_KEY is not set; using catalogue-backed standard recommendations.");
            return fallback(req, allCourses, "Gemini is not configured on the server, so verified catalogue recommendations are shown.");
        }
        try {
            return callGemini(req, allCourses);
        } catch (Exception e) {
            log.warn("Gemini request failed ({}); using catalogue-backed standard recommendations.", e.getMessage());
            return fallback(req, allCourses, "The AI service is temporarily unavailable, so verified catalogue recommendations are shown.");
        }
    }

    /* ---------------------------------------------------------------- Gemini */

    private RecommendationResponse callGemini(RecommendationRequest req, List<com.skillsphere.entity.Course> allCourses) throws Exception {
        ObjectNode body = mapper.createObjectNode();
        body.putObject("systemInstruction").putArray("parts").addObject().put("text", SYSTEM_PROMPT);
        body.putArray("contents").addObject().put("role", "user")
                .putArray("parts").addObject().put("text", buildUserPayload(req, allCourses));
        ObjectNode gen = body.putObject("generationConfig");
        gen.put("responseMimeType", "application/json");
        gen.set("responseSchema", schema());

        HttpRequest request = HttpRequest.newBuilder(URI.create(ENDPOINT.formatted(model)))
                .timeout(timeout)
                .header("Content-Type", "application/json")
                .header("x-goog-api-key", apiKey)
                .POST(HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(body)))
                .build();

        HttpResponse<String> res = http.send(request, HttpResponse.BodyHandlers.ofString());
        if (res.statusCode() != 200) {
            String snippet = res.body() == null ? "" : res.body().substring(0, Math.min(300, res.body().length()));
            throw new IllegalStateException("HTTP " + res.statusCode() + " " + snippet);
        }
        JsonNode root = mapper.readTree(res.body());
        String text = root.path("candidates").path(0).path("content").path("parts").path(0).path("text").asText("");
        if (text.isBlank()) throw new IllegalStateException("empty response");
        return parse(stripFences(text), allCourses);
    }

    private String buildUserPayload(RecommendationRequest req, List<com.skillsphere.entity.Course> allCourses) throws Exception {
        ObjectNode n = mapper.createObjectNode();
        n.put("jobRole", clean(req.jobRole(), "Not specified"));
        n.put("experienceLevel", clean(req.experience(), "Not specified"));
        n.put("department", clean(req.department(), "Not specified"));
        n.put("overallScorePercent", req.overallScore());

        ArrayNode arr = n.putArray("skillResults");
        for (SkillResult s : req.skillResults()) {
            ObjectNode o = arr.addObject();
            o.put("skill", s.skill());
            o.put("questionsCorrect", s.correct());
            o.put("questionsTotal", s.total());
            o.put("currentLevelOutOf5", s.level());
            o.put("requiredLevelOutOf5", s.required());
            o.put("gap", Math.max(0, s.required() - s.level()));
        }

        ArrayNode catalogueArr = n.putArray("availableCourseCatalogue");
        for (com.skillsphere.entity.Course c : allCourses) {
            ObjectNode co = catalogueArr.addObject();
            co.put("id", c.getId());
            co.put("title", c.getTitle());
            co.put("skill", c.getSkill() != null ? c.getSkill().getName() : "General");
            co.put("level", c.getLevel());
            co.put("durationHours", c.getDurationHours());
            co.put("provider", c.getProvider());
        }

        return "Employee assessment data and verified platform course catalogue:\n"
                + mapper.writerWithDefaultPrettyPrinter().writeValueAsString(n);
    }

    private RecommendationResponse parse(String json, List<com.skillsphere.entity.Course> allCourses) throws Exception {
        JsonNode root = mapper.readTree(json);
        Map<Long, com.skillsphere.entity.Course> courseMap = allCourses.stream()
                .collect(Collectors.toMap(com.skillsphere.entity.Course::getId, c -> c));

        List<Course> courses = new ArrayList<>();
        Set<Long> seenIds = new HashSet<>();

        for (JsonNode cNode : root.path("recommendations")) {
            long courseId = cNode.path("id").asLong(-1);
            com.skillsphere.entity.Course entity = courseMap.get(courseId);

            // Fallback match by exact title if AI omitted or mismatched ID
            if (entity == null) {
                String title = cNode.path("title").asText("");
                entity = allCourses.stream()
                        .filter(c -> c.getTitle().equalsIgnoreCase(title.trim()))
                        .findFirst()
                        .orElse(null);
            }

            if (entity == null || seenIds.contains(entity.getId())) {
                continue;
            }
            seenIds.add(entity.getId());

            String priority = cNode.path("priority").asText("Medium");
            if (!PRIORITIES.contains(priority)) priority = "Medium";

            List<String> topics = new ArrayList<>();
            for (JsonNode t : cNode.path("topics")) {
                if (topics.size() < 5 && !t.asText("").isBlank()) topics.add(cut(t.asText(), 60));
            }
            if (topics.isEmpty()) {
                topics.add(entity.getSkill() != null ? entity.getSkill().getName() : "Core Skills");
            }

            String skillName = entity.getSkill() != null ? entity.getSkill().getName() : "General";
            String duration = entity.getDurationHours() + " hours";
            String reason = cut(cNode.path("reason").asText("Selected from catalogue to address your skill development needs."), 400);
            String searchQuery = entity.getTitle() + " " + entity.getProvider();

            courses.add(new Course(
                    entity.getId(),
                    entity.getTitle(),
                    skillName,
                    entity.getLevel(),
                    duration,
                    priority,
                    entity.getProvider(),
                    reason,
                    topics,
                    searchQuery,
                    entity.getCourseUrl()
            ));

            if (courses.size() == 6) break;
        }

        if (courses.isEmpty()) {
            throw new IllegalStateException("no verified catalogue courses matched AI recommendations");
        }

        return new RecommendationResponse(cut(root.path("summary").asText(""), 600), courses, "gemini", null);
    }

    private ObjectNode schema() {
        ObjectNode root = mapper.createObjectNode().put("type", "OBJECT");
        ObjectNode props = root.putObject("properties");
        props.putObject("summary").put("type", "STRING");
        ObjectNode item = props.putObject("recommendations").put("type", "ARRAY")
                .putObject("items").put("type", "OBJECT");
        ObjectNode ip = item.putObject("properties");
        ip.putObject("id").put("type", "INTEGER");
        ip.putObject("title").put("type", "STRING");
        ip.putObject("priority").put("type", "STRING").putArray("enum").add("High").add("Medium").add("Low");
        ip.putObject("reason").put("type", "STRING");
        ip.putObject("topics").put("type", "ARRAY").putObject("items").put("type", "STRING");

        ArrayNode req = item.putArray("required");
        req.add("id").add("priority").add("reason");

        root.putArray("required").add("summary").add("recommendations");
        return root;
    }

    private static final String SYSTEM_PROMPT = """
            You are the expert learning advisor inside SkillSphere, an Organizational Knowledge Gap Intelligence Platform.
            You receive:
            1. An employee's technical assessment results and skill gaps.
            2. The platform's verified course catalogue (NPTEL, SWAYAM, Coursera) covering all skills.

            CRITICAL RULES:
            - You MUST select courses ONLY from the provided "availableCourseCatalogue".
            - Return the exact "id" of each chosen course. DO NOT invent courses or courses not present in the catalogue.
            - Select 3 to 5 courses, ordered by priority (High, Medium, Low). Focus first on skills with the largest gaps.
            - If there are no gaps, recommend 2 to 3 Intermediate or Advanced courses as stretch goals.
            - "reason" must be 1-2 personalized sentences explaining why this course resolves their specific assessment gap.
            - "topics" is 2 to 4 key topics the course covers.
            - "summary" is 2 sentences summarizing the employee's strengths and the primary learning priority.
            """;

    /* -------------------------------------------------------------- Fallback */

    private RecommendationResponse fallback(RecommendationRequest req, List<com.skillsphere.entity.Course> allCourses, String notice) {
        List<Course> out = new ArrayList<>();
        List<SkillResult> sorted = new ArrayList<>(req.skillResults());
        sorted.sort((a, b) -> Integer.compare(b.required() - b.level(), a.required() - a.level()));

        Set<Long> pickedIds = new HashSet<>();

        for (SkillResult s : sorted) {
            int gap = s.required() - s.level();
            if (gap <= 0) continue;

            String targetSkill = s.skill();
            String desiredLevel = s.level() <= 2 ? "Beginner" : (s.level() == 3 ? "Intermediate" : "Advanced");

            // Look for matching course in catalogue
            com.skillsphere.entity.Course match = allCourses.stream()
                    .filter(c -> !pickedIds.contains(c.getId()))
                    .filter(c -> c.getSkill() != null && c.getSkill().getName().equalsIgnoreCase(targetSkill))
                    .filter(c -> c.getLevel().equalsIgnoreCase(desiredLevel))
                    .findFirst()
                    .orElse(null);

            if (match == null) {
                // Any course for this skill
                match = allCourses.stream()
                        .filter(c -> !pickedIds.contains(c.getId()))
                        .filter(c -> c.getSkill() != null && c.getSkill().getName().equalsIgnoreCase(targetSkill))
                        .findFirst()
                        .orElse(null);
            }

            if (match != null) {
                pickedIds.add(match.getId());
                String priority = gap >= 2 ? "High" : "Medium";
                String reason = "You scored " + s.correct() + " of " + s.total() + " in " + s.skill()
                        + " (Level " + s.level() + "/5), which is below the target level " + s.required()
                        + " for your role. This course bridges your knowledge gap.";

                out.add(new Course(
                        match.getId(),
                        match.getTitle(),
                        match.getSkill() != null ? match.getSkill().getName() : s.skill(),
                        match.getLevel(),
                        match.getDurationHours() + " hours",
                        priority,
                        match.getProvider(),
                        reason,
                        List.of(s.skill(), match.getLevel() + " Concepts"),
                        match.getTitle() + " " + match.getProvider(),
                        match.getCourseUrl()
                ));
            }
            if (out.size() == 5) break;
        }

        // If no gaps or no matches found, pick 2-3 stretch courses from catalogue
        if (out.isEmpty() && !allCourses.isEmpty()) {
            for (com.skillsphere.entity.Course c : allCourses) {
                if (c.getLevel().equalsIgnoreCase("Intermediate") || c.getLevel().equalsIgnoreCase("Advanced")) {
                    out.add(new Course(
                            c.getId(),
                            c.getTitle(),
                            c.getSkill() != null ? c.getSkill().getName() : "Core Skill",
                            c.getLevel(),
                            c.getDurationHours() + " hours",
                            "Low",
                            c.getProvider(),
                            "Your assessment demonstrates strong competence. This stretch course will deepen your mastery.",
                            List.of(c.getLevel() + " Mastery"),
                            c.getTitle() + " " + c.getProvider(),
                            c.getCourseUrl()
                    ));
                    if (out.size() == 3) break;
                }
            }
        }

        String summary = (out.isEmpty() || out.get(0).priority().equals("Low"))
                ? "Great work — you met the target level in every assessed skill."
                : "Focus first on " + out.get(0).skill() + ", where your gap is largest.";

        return new RecommendationResponse(summary, out, "rules", notice);
    }

    /* --------------------------------------------------------------- Helpers */

    private void throttle(String userKey) {
        long now = System.currentTimeMillis();
        Long prev = lastCall.put(userKey, now);
        if (prev != null && now - prev < MIN_INTERVAL_MS) {
            throw new ApiException(HttpStatus.TOO_MANY_REQUESTS, "Please wait a few seconds before requesting again.");
        }
    }

    private static String stripFences(String s) {
        String t = s.trim();
        if (t.startsWith("```")) {
            t = t.replaceFirst("^```(?:json)?\\s*", "").replaceFirst("\\s*```$", "");
        }
        return t;
    }

    private static String clean(String v, String dflt) {
        return v == null || v.isBlank() ? dflt : v.replaceAll("[\\r\\n]+", " ").trim();
    }

    private static String cut(String v, int max) {
        String t = v == null ? "" : v.trim();
        return t.length() > max ? t.substring(0, max) : t;
    }
}
