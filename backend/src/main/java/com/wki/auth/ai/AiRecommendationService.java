package com.wki.auth.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.*;

/**
 * Builds personalised study recommendations for the skills an employee is lacking.
 *
 * 1. If an Anthropic API key is configured (app.ai.api-key or the ANTHROPIC_API_KEY environment variable)
 *    the assessment result is sent to Claude, which writes the coaching plan.
 * 2. If there is no key, or the call fails for any reason, a built-in rule-based engine produces the same
 *    structure, and the result is labelled source = "RULES" so the screen is honest about where it came from.
 *
 * Only the role name, skill names, scores and weak topics are sent. Never name, email or ids.
 * Course titles and reference links always come from this server, never from the AI, so they cannot be invented.
 */
@Service
public class AiRecommendationService {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
    private final String apiKey;
    private final String model;
    private final String apiUrl;
    private final boolean enabled;
    private final boolean openAiStyle;   // true = OpenAI-compatible API (Groq, Gemini, OpenRouter...), false = Anthropic

    public AiRecommendationService(@Value("${app.ai.api-key:}") String apiKey,
                                   @Value("${app.ai.model:claude-sonnet-5-5}") String model,
                                   @Value("${app.ai.api-url:}") String apiUrl,
                                   @Value("${app.ai.provider:anthropic}") String provider,
                                   @Value("${app.ai.enabled:true}") boolean enabled) {
        this.apiKey = apiKey == null ? "" : apiKey.trim();
        this.model = model;
        this.enabled = enabled;
        this.openAiStyle = "openai".equalsIgnoreCase(provider == null ? "" : provider.trim());
        String url = apiUrl == null ? "" : apiUrl.trim();
        this.apiUrl = !url.isEmpty() ? url : (openAiStyle ? "" : "https://api.anthropic.com/v1/messages");
    }

    /**
     * @param roleName    e.g. "Java Developer"
     * @param overallPct  overall score out of 100
     * @param skills      one map per role skill: skillName, weightPct, marksEarned, skillPct, requiredLevel, level,
     *                    gap (Boolean), weakTopics (List&lt;String&gt;), courses (List&lt;Map&gt; title + hours)
     */
    public Map<String, Object> generate(String roleName, double overallPct, List<Map<String, Object>> skills) {
        List<Map<String, Object>> gaps = new ArrayList<>();
        List<String> strengths = new ArrayList<>();
        for (Map<String, Object> s : skills) {
            if (Boolean.TRUE.equals(s.get("gap"))) gaps.add(s); else strengths.add(String.valueOf(s.get("skillName")));
        }
        gaps.sort(Comparator.comparingDouble(a -> -marksLost(a)));

        Map<String, Object> result = rules(roleName, overallPct, gaps, strengths);
        if (gaps.isEmpty()) return result;               // nothing lacking: no need to call the AI

        if (!enabled || apiKey.isEmpty()) {
            result.put("note", "Built-in recommendations are shown. Add an Anthropic API key (app.ai.api-key) to get AI-written coaching.");
            return result;
        }
        try {
            JsonNode ai = callClaude(roleName, overallPct, gaps, strengths);
            return overlay(result, ai, gaps);
        } catch (Exception e) {
            System.err.println("AiRecommendationService: AI call failed, using built-in recommendations: " + e.getMessage());
            result.put("note", "The AI service could not be reached, so built-in recommendations are shown. You can try again later.");
            return result;
        }
    }

    // ------------------------------------------------------------------ plain chat completion (used by the doubt chat)
    public boolean configured() { return enabled && !apiKey.isEmpty() && !apiUrl.isEmpty(); }

    /** Sends a conversation to Claude and returns the reply text. Throws if the service is unavailable. */
    public String complete(String system, List<Map<String, String>> messages, int maxTokens) throws Exception {
        if (!configured()) throw new IllegalStateException("AI is not configured");
        String out = send(system, messages, maxTokens).trim();
        if (out.isEmpty()) throw new IllegalStateException("AI reply was empty");
        return out;
    }

    /** One HTTP call to the configured provider. Returns the reply text. */
    private String send(String system, List<Map<String, String>> messages, int maxTokens) throws Exception {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", model);
        body.put("max_tokens", maxTokens);
        HttpRequest.Builder rb = HttpRequest.newBuilder(URI.create(apiUrl))
                .timeout(Duration.ofSeconds(60))
                .header("content-type", "application/json");
        if (openAiStyle) {
            List<Map<String, String>> all = new ArrayList<>();
            all.add(Map.of("role", "system", "content", system));
            all.addAll(messages);
            body.put("messages", all);
            rb.header("Authorization", "Bearer " + apiKey);
        } else {
            body.put("system", system);
            body.put("messages", messages);
            rb.header("x-api-key", apiKey).header("anthropic-version", "2023-06-01");
        }
        HttpRequest req = rb.POST(HttpRequest.BodyPublishers.ofString(MAPPER.writeValueAsString(body))).build();
        HttpResponse<String> res = http.send(req, HttpResponse.BodyHandlers.ofString());
        if (res.statusCode() / 100 != 2) {
            String b = res.body() == null ? "" : res.body().replaceAll("\\s+", " ");
            if (b.length() > 300) b = b.substring(0, 300) + "...";
            throw new IllegalStateException("AI service returned HTTP " + res.statusCode() + " | url=" + apiUrl + " | model=" + model +
                    " | provider=" + (openAiStyle ? "openai" : "anthropic") + " | reply=" + b);
        }
        JsonNode root = MAPPER.readTree(res.body());
        if (openAiStyle) return root.path("choices").path(0).path("message").path("content").asText("");
        StringBuilder text = new StringBuilder();
        for (JsonNode block : root.path("content"))
            if ("text".equals(block.path("type").asText())) text.append(block.path("text").asText());
        return text.toString();
    }

    // ------------------------------------------------------------------ AI call
    private JsonNode callClaude(String roleName, double overall, List<Map<String, Object>> gaps, List<String> strengths) throws Exception {
        List<Map<String, Object>> gapInfo = new ArrayList<>();
        for (Map<String, Object> g : gaps) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("skillName", g.get("skillName"));
            m.put("marksEarned", g.get("marksEarned"));
            m.put("marksAvailable", g.get("weightPct"));
            m.put("skillScorePercent", g.get("skillPct"));
            m.put("weakTopics", g.get("weakTopics"));
            gapInfo.add(m);
        }
        Map<String, Object> input = new LinkedHashMap<>();
        input.put("targetRole", roleName);
        input.put("overallScorePercent", overall);
        input.put("lackingSkills", gapInfo);
        input.put("strongSkills", strengths);

        String system = "You are a career-development coach inside a corporate skills platform. " +
                "Reply with ONE JSON object and nothing else (no markdown, no code fences). " +
                "Never include URLs. Be specific, practical and encouraging. Keep every string under 300 characters.";
        String user = "An employee preparing for the role \"" + roleName + "\" took a role assessment worth 100 marks. " +
                "Each skill carries a share of those marks. Here is the result:\n" + MAPPER.writeValueAsString(input) + "\n\n" +
                "For EVERY lacking skill, write a recommendation. The weakTopics are the topics they answered wrongly, so focus on them. " +
                "Order skills by marks lost (most first). Return exactly this JSON shape:\n" +
                "{\"summary\": string (2-3 sentences about overall performance for this role),\n" +
                " \"skills\": [{\"skillName\": string (exactly as given),\n" +
                "   \"priority\": \"high\"|\"medium\"|\"low\",\n" +
                "   \"why\": string (why this matters for the role and what the score shows),\n" +
                "   \"focusTopics\": [string, max 5],\n" +
                "   \"studyPlan\": [string, 3-5 ordered steps, each doable in a week or less],\n" +
                "   \"practice\": [string, 2-3 hands-on exercises or mini projects],\n" +
                "   \"estimatedHours\": integer}],\n" +
                " \"nextSteps\": [string, 3 short actions for this week]}";

        String t = send(system, List.of(Map.of("role", "user", "content", user)), 3000);
        int a = t.indexOf('{'), b = t.lastIndexOf('}');
        if (a < 0 || b <= a) throw new IllegalStateException("AI reply was not JSON");
        return MAPPER.readTree(t.substring(a, b + 1));
    }

    /** Keeps the rule-based structure and replaces the text fields with the AI's, but only where the AI gave valid values. */
    private Map<String, Object> overlay(Map<String, Object> base, JsonNode ai, List<Map<String, Object>> gaps) {
        String summary = clean(ai.path("summary").asText(""), 600);
        if (!summary.isEmpty()) base.put("summary", summary);

        Map<String, JsonNode> bySkill = new HashMap<>();
        for (JsonNode s : ai.path("skills")) bySkill.put(s.path("skillName").asText("").trim().toLowerCase(Locale.ROOT), s);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> out = (List<Map<String, Object>>) base.get("skills");
        int used = 0;
        for (Map<String, Object> row : out) {
            JsonNode s = bySkill.get(String.valueOf(row.get("skillName")).toLowerCase(Locale.ROOT));
            if (s == null) continue;
            String pr = s.path("priority").asText("").toLowerCase(Locale.ROOT);
            if (List.of("high", "medium", "low").contains(pr)) row.put("priority", pr);
            String why = clean(s.path("why").asText(""), 400);
            if (!why.isEmpty()) row.put("why", why);
            List<String> topics = strings(s.path("focusTopics"), 5, 120);
            if (!topics.isEmpty()) row.put("focusTopics", topics);
            List<String> plan = strings(s.path("studyPlan"), 6, 300);
            if (!plan.isEmpty()) row.put("studyPlan", plan);
            List<String> practice = strings(s.path("practice"), 4, 300);
            if (!practice.isEmpty()) row.put("practice", practice);
            int hrs = s.path("estimatedHours").asInt(0);
            if (hrs >= 1 && hrs <= 300) row.put("estimatedHours", hrs);
            used++;
        }
        List<String> next = strings(ai.path("nextSteps"), 5, 250);
        if (!next.isEmpty()) base.put("nextSteps", next);
        base.put("source", used > 0 ? "AI" : "RULES");
        if (used == 0) base.put("note", "The AI reply could not be used, so built-in recommendations are shown.");
        return base;
    }

    // ------------------------------------------------------------------ rule-based engine
    private Map<String, Object> rules(String roleName, double overall, List<Map<String, Object>> gaps, List<String> strengths) {
        List<Map<String, Object>> rows = new ArrayList<>();
        for (Map<String, Object> g : gaps) {
            String name = String.valueOf(g.get("skillName"));
            double pct = num(g.get("skillPct"));
            @SuppressWarnings("unchecked") List<String> weak = (List<String>) g.getOrDefault("weakTopics", List.of());
            @SuppressWarnings("unchecked") List<Map<String, Object>> courses = (List<Map<String, Object>>) g.getOrDefault("courses", List.of());

            List<String> topics = weak.isEmpty() ? List.of("Core concepts of " + name) : weak.stream().distinct().limit(5).toList();
            List<String> plan = new ArrayList<>();
            plan.add("Revisit the fundamentals of " + name + ", starting with: " + String.join(", ", topics) + ".");
            int hours = 0;
            if (!courses.isEmpty()) {
                Map<String, Object> c = courses.get(0);
                plan.add("Complete the course \"" + c.get("title") + "\" (" + c.get("hours") + " hours) from your recommended training.");
                for (Map<String, Object> cc : courses) hours += (int) num(cc.get("hours"));
                hours = Math.min(hours, 40);
            }
            plan.add("Write short practice exercises on each weak topic and check your answers against the explanations in your assessment review.");
            plan.add("Apply " + name + " in a small task from your current work or a mini project.");
            plan.add("Retake the " + roleName + " assessment in 2 to 3 weeks to measure improvement.");
            if (hours == 0) hours = pct < 35 ? 16 : 10;

            Map<String, Object> m = new LinkedHashMap<>();
            m.put("skillName", name);
            m.put("skillPct", g.get("skillPct"));
            m.put("marksEarned", g.get("marksEarned"));
            m.put("weightPct", g.get("weightPct"));
            m.put("priority", pct < 35 ? "high" : pct < 55 ? "medium" : "low");
            m.put("why", "You earned " + fmt(num(g.get("marksEarned"))) + " of " + fmt(num(g.get("weightPct"))) + " marks (" + fmt(pct) +
                    "%) in " + name + ", which is below the level needed for a " + roleName + ". This skill carries " + fmt(num(g.get("weightPct"))) + "% of the role.");
            m.put("focusTopics", topics);
            m.put("studyPlan", plan);
            m.put("practice", List.of("Solve 5 small " + name + " problems on your weakest topic.", "Explain one concept from " + name + " to a teammate in your own words."));
            m.put("estimatedHours", hours);
            m.put("courses", courses.stream().map(c -> String.valueOf(c.get("title"))).toList());
            m.put("links", linksFor(name));
            rows.add(m);
        }

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("source", "RULES");
        out.put("roleName", roleName);
        out.put("overallPct", overall);
        if (rows.isEmpty()) {
            out.put("summary", "You scored " + fmt(overall) + "% on the " + roleName + " assessment and meet the target on every skill. Keep growing by taking on stretch work and mentoring others.");
            out.put("nextSteps", List.of("Share your knowledge in a knowledge session.", "Pick an advanced course in your strongest skill.", "Retake the assessment in a few months to confirm your level."));
        } else {
            List<String> names = rows.stream().map(r -> String.valueOf(r.get("skillName"))).toList();
            out.put("summary", "You scored " + fmt(overall) + "% on the " + roleName + " assessment. The skills costing you the most marks are " +
                    String.join(", ", names.subList(0, Math.min(3, names.size()))) + ". Focus on these first.");
            out.put("nextSteps", List.of("Enroll in the recommended course for " + names.get(0) + " this week.",
                    "Review every question you missed and read its explanation.",
                    "Block 3 to 4 hours a week for learning and retake the assessment in 2 to 3 weeks."));
        }
        out.put("skills", rows);
        out.put("strengths", strengths);
        return out;
    }

    /** Official documentation roots, curated here so the AI can never make up a link. */
    private static List<Map<String, String>> linksFor(String skill) {
        Map<String, String[]> m = new HashMap<>();
        m.put("Java", new String[]{"Dev.java learning hub", "https://dev.java/learn/"});
        m.put("Spring Boot", new String[]{"Spring guides", "https://spring.io/guides"});
        m.put("SQL & Databases", new String[]{"MySQL reference manual", "https://dev.mysql.com/doc/"});
        m.put("JavaScript", new String[]{"MDN JavaScript guide", "https://developer.mozilla.org/docs/Web/JavaScript"});
        m.put("HTML & CSS", new String[]{"MDN Learn web development", "https://developer.mozilla.org/docs/Learn"});
        m.put("React", new String[]{"React documentation", "https://react.dev/learn"});
        m.put("Python", new String[]{"Python tutorial", "https://docs.python.org/3/tutorial/"});
        m.put("Git & Version Control", new String[]{"Pro Git book", "https://git-scm.com/book"});
        m.put("Docker & DevOps", new String[]{"Docker get started", "https://docs.docker.com/get-started/"});
        String[] l = m.get(skill);
        return l == null ? List.of() : List.of(Map.of("title", l[0], "url", l[1]));
    }

    // ------------------------------------------------------------------ small helpers
    private static double marksLost(Map<String, Object> s) { return num(s.get("weightPct")) - num(s.get("marksEarned")); }
    private static double num(Object o) { return o instanceof Number n ? n.doubleValue() : 0; }
    private static String fmt(double d) { return d == Math.rint(d) ? String.valueOf((long) d) : String.format(Locale.ROOT, "%.1f", d); }
    private static String clean(String s, int max) {
        String t = s == null ? "" : s.replaceAll("https?://\\S+", "").replaceAll("\\s+", " ").trim();
        return t.length() > max ? t.substring(0, max - 1) + "…" : t;
    }
    private static List<String> strings(JsonNode arr, int maxItems, int maxLen) {
        List<String> out = new ArrayList<>();
        if (arr == null || !arr.isArray()) return out;
        for (JsonNode n : arr) {
            String s = clean(n.asText(""), maxLen);
            if (!s.isEmpty()) out.add(s);
            if (out.size() >= maxItems) break;
        }
        return out;
    }
}
