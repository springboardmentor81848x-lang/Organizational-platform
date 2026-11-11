package com.skillsphere.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.skillsphere.dto.CourseDto;
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
import org.springframework.transaction.annotation.Transactional;

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
 * If Gemini is not configured, rate-limited, or fails, a catalogue-backed rule-based fallback is used.
 */
@Service
public class GeminiRecommendationService {

    private static final Logger log = LoggerFactory.getLogger(GeminiRecommendationService.class);
    private static final String ENDPOINT = "https://generativelanguage.googleapis.com/v1beta/models/%s:generateContent";
    private static final long MIN_INTERVAL_MS = 4_000;
    private static final int MAX_CACHE_ENTRIES = 1_000;
    private static final long CACHE_TTL_MS = 10 * 60 * 1000L; // 10 minutes
    private static final Set<String> PRIORITIES = Set.of("High", "Medium", "Low");

    private record CacheEntry(RecommendationResponse response, RecommendationRequest request, long timestamp) {}

    private final ObjectMapper mapper;
    private final CourseRepository courseRepository;
    private final HttpClient http;
    private final String apiKey;
    private final String model;
    private final Duration timeout;
    private final Map<String, CacheEntry> recentResponses = new ConcurrentHashMap<>();

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

    /**
     * Safely loads active course catalogue entries and projects them into immutable DTOs
     * within a read-only transaction, eliminating Hibernate LazyInitializationException on detached proxies
     * and avoiding N+1 queries by eagerly fetching skill relationships.
     */
    @Transactional(readOnly = true)
    public List<CourseDto> getActiveCatalogue() {
        List<com.skillsphere.entity.Course> courses;
        try {
            courses = courseRepository.findAllActiveWithSkill();
            if (courses == null || courses.isEmpty()) {
                courses = courseRepository.findAllWithSkill();
            }
        } catch (Exception e) {
            log.warn("findAllActiveWithSkill failed ({}); falling back to findAllWithSkill.", e.getMessage());
            try {
                courses = courseRepository.findAllWithSkill();
            } catch (Exception ex) {
                log.error("Course repository query failed: {}", ex.getMessage());
                courses = Collections.emptyList();
            }
        }
        if (courses == null || courses.isEmpty()) return Collections.emptyList();
        return courses.stream()
                .map(this::toCourseDto)
                .toList();
    }

    private CourseDto toCourseDto(com.skillsphere.entity.Course c) {
        return new CourseDto(
                c.getId(),
                c.getTitle(),
                c.getDescription(),
                c.getSkill() != null ? c.getSkill().getId() : null,
                c.getSkill() != null ? c.getSkill().getName() : "General",
                c.getLevel(),
                c.getDurationHours(),
                c.getProvider(),
                c.getCourseUrl(),
                c.getThumbnailUrl(),
                null
        );
    }

    public RecommendationResponse recommend(String userKey, RecommendationRequest req) {
        long now = System.currentTimeMillis();
        CacheEntry cached = recentResponses.get(userKey);

        // Deduplication & rapid-request handling: if called within MIN_INTERVAL_MS and cached entry
        // exists with identical request inputs, serve the cached response immediately to prevent
        // duplicate external AI calls and client-side rate limit errors.
        // If the request inputs changed (new assessment, changed skill gaps, or profile update),
        // bypass the cache to compute fresh, accurate recommendations.
        if (cached != null && now - cached.timestamp() < MIN_INTERVAL_MS && Objects.equals(cached.request(), req)) {
            log.debug("Serving recent recommendation response for user {} within interval window", userKey);
            return cached.response();
        }

        // Fetch catalogue safely inside transaction boundary
        List<CourseDto> allCourses = getActiveCatalogue();

        if (allCourses.isEmpty()) {
            log.warn("Learning catalogue is empty; returning controlled empty response.");
            return new RecommendationResponse(
                    "No learning catalogue courses are currently available in the platform.",
                    Collections.emptyList(),
                    "rules",
                    "Course catalogue is currently empty."
            );
        }

        RecommendationResponse response;
        if (apiKey.isEmpty()) {
            log.warn("GEMINI_API_KEY is not set; using catalogue-backed standard recommendations.");
            response = fallback(req, allCourses, "Gemini is not configured on the server, so verified catalogue recommendations are shown.");
        } else {
            try {
                response = callGemini(req, allCourses);
            } catch (Exception e) {
                log.warn("Gemini request failed ({}); using catalogue-backed standard recommendations.", e.getMessage());
                String notice = (e.getMessage() != null && e.getMessage().contains("429"))
                        ? "Gemini rate limit reached; verified catalogue recommendations are displayed."
                        : "The AI service is temporarily unavailable, so verified catalogue recommendations are shown.";
                response = fallback(req, allCourses, notice);
            }
        }

        if (response != null) {
            evictExpiredCacheEntries(now);
            recentResponses.put(userKey, new CacheEntry(response, req, now));
        }
        return response;
    }

    private void evictExpiredCacheEntries(long now) {
        if (recentResponses.size() >= MAX_CACHE_ENTRIES) {
            recentResponses.entrySet().removeIf(e -> now - e.getValue().timestamp() > CACHE_TTL_MS);
            if (recentResponses.size() >= MAX_CACHE_ENTRIES) {
                recentResponses.keySet().stream().findFirst().ifPresent(recentResponses::remove);
            }
        }
    }

    /* ---------------------------------------------------------------- Gemini */

    private RecommendationResponse callGemini(RecommendationRequest req, List<CourseDto> allCourses) throws Exception {
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

        HttpResponse<String> res;
        try {
            res = http.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (java.net.http.HttpTimeoutException e) {
            log.warn("Gemini request timed out after {}s", timeout.toSeconds());
            return fallback(req, allCourses, "AI recommendation service timed out; catalogue recommendations are provided.");
        } catch (java.io.IOException e) {
            log.warn("Gemini network connectivity error: {}", e.getMessage());
            return fallback(req, allCourses, "AI recommendation service could not be reached; catalogue recommendations are provided.");
        }

        if (res.statusCode() == 429) {
            Optional<String> retryAfter = res.headers().firstValue("Retry-After");
            log.warn("Gemini API rate limit reached (HTTP 429){}. Falling back to catalogue recommendations.",
                    retryAfter.map(s -> " [Retry-After: " + s + "s]").orElse(""));
            return fallback(req, allCourses, "Gemini rate limit reached; verified catalogue recommendations are displayed.");
        }
        if (res.statusCode() >= 500) {
            log.warn("Gemini upstream service error (HTTP {}). Falling back to catalogue recommendations.", res.statusCode());
            return fallback(req, allCourses, "AI service temporarily unavailable; verified catalogue recommendations are displayed.");
        }
        if (res.statusCode() != 200) {
            String snippet = res.body() == null ? "" : res.body().substring(0, Math.min(300, res.body().length()));
            log.warn("Gemini returned HTTP {}: {}", res.statusCode(), snippet);
            return fallback(req, allCourses, "AI service returned status " + res.statusCode() + "; verified catalogue recommendations are displayed.");
        }

        JsonNode root = mapper.readTree(res.body());
        String text = root.path("candidates").path(0).path("content").path("parts").path(0).path("text").asText("");
        if (text.isBlank()) throw new IllegalStateException("empty response from Gemini");
        return parse(stripFences(text), allCourses);
    }

    private String buildUserPayload(RecommendationRequest req, List<CourseDto> allCourses) throws Exception {
        ObjectNode n = mapper.createObjectNode();
        n.put("jobRole", clean(req.jobRole(), "Not specified"));
        n.put("experienceLevel", clean(req.experience(), "Not specified"));
        n.put("department", clean(req.department(), "Not specified"));
        n.put("overallScorePercent", req.overallScore());

        ArrayNode arr = n.putArray("skillResults");
        if (req.skillResults() != null) {
            for (SkillResult s : req.skillResults()) {
                ObjectNode o = arr.addObject();
                o.put("skill", s.skill() != null ? s.skill() : "General");
                o.put("questionsCorrect", s.correct());
                o.put("questionsTotal", s.total());
                o.put("currentLevelOutOf5", s.level());
                o.put("requiredLevelOutOf5", s.required());
                o.put("gap", Math.max(0, s.required() - s.level()));
            }
        }

        ArrayNode catalogueArr = n.putArray("availableCourseCatalogue");
        for (CourseDto c : allCourses) {
            ObjectNode co = catalogueArr.addObject();
            co.put("id", c.id());
            co.put("title", c.title());
            co.put("skill", c.skillName() != null ? c.skillName() : "General");
            co.put("level", c.level() != null ? c.level() : "Intermediate");
            co.put("durationHours", c.durationHours());
            co.put("provider", c.provider() != null ? c.provider() : "SkillSphere Academy");
        }

        return "Employee assessment data and verified platform course catalogue:\n"
                + mapper.writerWithDefaultPrettyPrinter().writeValueAsString(n);
    }

    private RecommendationResponse parse(String json, List<CourseDto> allCourses) throws Exception {
        JsonNode root = mapper.readTree(json);
        Map<Long, CourseDto> courseMap = allCourses.stream()
                .collect(Collectors.toMap(CourseDto::id, c -> c, (a, b) -> a));

        List<Course> courses = new ArrayList<>();
        Set<Long> seenIds = new HashSet<>();

        for (JsonNode cNode : root.path("recommendations")) {
            long courseId = cNode.path("id").asLong(-1);
            CourseDto entity = courseMap.get(courseId);

            // Fallback match by exact title if AI omitted or mismatched ID
            if (entity == null) {
                String title = cNode.path("title").asText("");
                entity = allCourses.stream()
                        .filter(c -> c.title() != null && c.title().equalsIgnoreCase(title.trim()))
                        .findFirst()
                        .orElse(null);
            }

            if (entity == null || seenIds.contains(entity.id())) {
                continue;
            }
            seenIds.add(entity.id());

            String priority = cNode.path("priority").asText("Medium");
            if (!PRIORITIES.contains(priority)) priority = "Medium";

            List<String> topics = new ArrayList<>();
            for (JsonNode t : cNode.path("topics")) {
                if (topics.size() < 5 && !t.asText("").isBlank()) topics.add(cut(t.asText(), 60));
            }
            if (topics.isEmpty()) {
                topics.add(entity.skillName() != null ? entity.skillName() : "Core Skills");
            }

            String skillName = entity.skillName() != null ? entity.skillName() : "General";
            String duration = entity.durationHours() + " hours";
            String reason = cut(cNode.path("reason").asText("Selected from catalogue to address your skill development needs."), 400);
            String searchQuery = entity.title() + " " + (entity.provider() != null ? entity.provider() : "");

            courses.add(new Course(
                    entity.id(),
                    entity.title(),
                    skillName,
                    entity.level(),
                    duration,
                    priority,
                    entity.provider(),
                    reason,
                    topics,
                    searchQuery,
                    entity.courseUrl()
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

    private RecommendationResponse fallback(RecommendationRequest req, List<CourseDto> allCourses, String notice) {
        if (allCourses == null || allCourses.isEmpty()) {
            return new RecommendationResponse(
                    "No learning catalogue courses are currently available in the platform.",
                    Collections.emptyList(),
                    "rules",
                    notice
            );
        }

        List<Course> out = new ArrayList<>();
        List<SkillResult> skillResults = req.skillResults() == null ? Collections.emptyList() : req.skillResults();
        List<SkillResult> sorted = new ArrayList<>(skillResults);
        sorted.sort((a, b) -> Integer.compare(b.required() - b.level(), a.required() - a.level()));

        Set<Long> pickedIds = new HashSet<>();

        for (SkillResult s : sorted) {
            int gap = s.required() - s.level();
            if (gap <= 0) continue;

            String targetSkill = s.skill();
            if (targetSkill == null || targetSkill.isBlank()) continue;

            String desiredLevel = s.level() <= 2 ? "Beginner" : (s.level() == 3 ? "Intermediate" : "Advanced");

            // Look for matching course in catalogue
            CourseDto match = allCourses.stream()
                    .filter(c -> !pickedIds.contains(c.id()))
                    .filter(c -> c.skillName() != null && c.skillName().equalsIgnoreCase(targetSkill))
                    .filter(c -> c.level() != null && c.level().equalsIgnoreCase(desiredLevel))
                    .findFirst()
                    .orElse(null);

            if (match == null) {
                // Any course for this skill
                match = allCourses.stream()
                        .filter(c -> !pickedIds.contains(c.id()))
                        .filter(c -> c.skillName() != null && c.skillName().equalsIgnoreCase(targetSkill))
                        .findFirst()
                        .orElse(null);
            }

            if (match != null) {
                pickedIds.add(match.id());
                String priority = gap >= 2 ? "High" : "Medium";
                String reason = "You scored " + s.correct() + " of " + s.total() + " in " + s.skill()
                        + " (Level " + s.level() + "/5), which is below the target level " + s.required()
                        + " for your role. This course bridges your knowledge gap.";

                out.add(new Course(
                        match.id(),
                        match.title(),
                        match.skillName() != null ? match.skillName() : s.skill(),
                        match.level(),
                        match.durationHours() + " hours",
                        priority,
                        match.provider(),
                        reason,
                        List.of(s.skill(), (match.level() != null ? match.level() : "Core") + " Concepts"),
                        match.title() + " " + (match.provider() != null ? match.provider() : ""),
                        match.courseUrl()
                ));
            }
            if (out.size() == 5) break;
        }

        // If no gaps or no matches found, pick 2-3 stretch courses from catalogue
        if (out.isEmpty()) {
            for (CourseDto c : allCourses) {
                if (c.level() != null && (c.level().equalsIgnoreCase("Intermediate") || c.level().equalsIgnoreCase("Advanced"))) {
                    out.add(new Course(
                            c.id(),
                            c.title(),
                            c.skillName() != null ? c.skillName() : "Core Skill",
                            c.level(),
                            c.durationHours() + " hours",
                            "Low",
                            c.provider(),
                            "Your assessment demonstrates strong competence. This stretch course will deepen your mastery.",
                            List.of((c.level() != null ? c.level() : "Advanced") + " Mastery"),
                            c.title() + " " + (c.provider() != null ? c.provider() : ""),
                            c.courseUrl()
                    ));
                    if (out.size() == 3) break;
                }
            }
        }

        // If still empty (e.g. only beginner courses exist), take first 2-3 courses
        if (out.isEmpty()) {
            for (CourseDto c : allCourses) {
                out.add(new Course(
                        c.id(),
                        c.title(),
                        c.skillName() != null ? c.skillName() : "Core Skill",
                        c.level() != null ? c.level() : "Beginner",
                        c.durationHours() + " hours",
                        "Medium",
                        c.provider(),
                        "Standard catalogue recommendation for your role.",
                        List.of("Foundation Concepts"),
                        c.title() + " " + (c.provider() != null ? c.provider() : ""),
                        c.courseUrl()
                ));
                if (out.size() == 3) break;
            }
        }

        String summary = (out.isEmpty() || out.get(0).priority().equals("Low"))
                ? "Great work — you met the target level in every assessed skill."
                : "Focus first on " + out.get(0).skill() + ", where your gap is largest.";

        return new RecommendationResponse(summary, out, "rules", notice);
    }

    /* --------------------------------------------------------------- Helpers */

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
