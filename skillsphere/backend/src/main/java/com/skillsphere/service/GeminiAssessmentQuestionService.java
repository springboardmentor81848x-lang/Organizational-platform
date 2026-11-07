package com.skillsphere.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.skillsphere.entity.Skill;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.*;

/**
 * Generates dynamic, tailored skill assessment questions using Google Gemini API.
 * The API key is kept secure on the backend and never logged or exposed.
 * If Gemini fails, times out, or returns invalid questions after retries,
 * it throws GeminiGenerationException so AssessmentService can activate the database fallback.
 */
@Service
public class GeminiAssessmentQuestionService {

    private static final Logger log = LoggerFactory.getLogger(GeminiAssessmentQuestionService.class);
    private static final String ENDPOINT = "https://generativelanguage.googleapis.com/v1beta/models/%s:generateContent";
    private static final Set<String> ALLOWED_DIFFICULTIES = Set.of("EASY", "MEDIUM", "HARD");

    private final ObjectMapper mapper;
    private final HttpClient http;
    private final String apiKey;
    private final String model;
    private final Duration timeout;

    public GeminiAssessmentQuestionService(ObjectMapper mapper,
                                           @Value("${app.gemini.api-key:}") String apiKey,
                                           @Value("${app.gemini.model:gemini-3.5-flash}") String model,
                                           @Value("${app.gemini.timeout-seconds:45}") long timeoutSeconds) {
        this.mapper = mapper;
        this.apiKey = apiKey == null ? "" : apiKey.trim();
        this.model = model;
        this.timeout = Duration.ofSeconds(timeoutSeconds);
        this.http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
    }

    public boolean isConfigured() {
        return !apiKey.isEmpty();
    }

    public List<GeneratedQuestion> generateQuestions(Skill skill,
                                                     String targetLevel,
                                                     String currentLevel,
                                                     int questionCount,
                                                     List<String> previousQuestionTexts) {
        if (!isConfigured()) {
            throw new GeminiUnavailableException("GEMINI_API_KEY is not configured.");
        }

        int maxAttempts = 2;
        Exception lastException = null;

        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                log.info("Requesting {} assessment questions from Gemini for skill '{}' at level '{}' (attempt {}/{})",
                        questionCount, skill.getName(), targetLevel, attempt, maxAttempts);
                List<GeneratedQuestion> questions = callGemini(skill, targetLevel, currentLevel, questionCount, previousQuestionTexts);
                List<GeneratedQuestion> validated = validateAndFilterQuestions(questions, skill, questionCount, previousQuestionTexts);

                if (validated.size() >= Math.max(3, (int) Math.ceil(questionCount * 0.7))) {
                    log.info("Successfully generated and validated {} questions from Gemini for skill '{}'",
                            validated.size(), skill.getName());
                    return validated;
                } else {
                    throw new IllegalStateException("Gemini returned insufficient valid questions (" + validated.size() + "/" + questionCount + ")");
                }
            } catch (Exception e) {
                lastException = e;
                log.warn("Gemini question generation attempt {} failed for skill '{}': {}", attempt, skill.getName(), e.getMessage());
            }
        }

        throw new GeminiGenerationException("Gemini question generation failed after " + maxAttempts + " attempts: "
                + (lastException != null ? lastException.getMessage() : "Unknown error"), lastException);
    }

    private List<GeneratedQuestion> callGemini(Skill skill,
                                               String targetLevel,
                                               String currentLevel,
                                               int questionCount,
                                               List<String> previousQuestionTexts) throws Exception {
        ObjectNode body = mapper.createObjectNode();
        body.putObject("systemInstruction").putArray("parts").addObject().put("text", SYSTEM_INSTRUCTION);

        String prompt = buildPrompt(skill, targetLevel, currentLevel, questionCount, previousQuestionTexts);
        body.putArray("contents").addObject().put("role", "user")
                .putArray("parts").addObject().put("text", prompt);

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
            String snippet = res.body() == null ? "" : res.body().substring(0, Math.min(200, res.body().length()));
            throw new IllegalStateException("HTTP " + res.statusCode() + " " + snippet);
        }

        JsonNode root = mapper.readTree(res.body());
        String text = root.path("candidates").path(0).path("content").path("parts").path(0).path("text").asText("");
        if (text.isBlank()) {
            throw new IllegalStateException("Empty response from Gemini");
        }

        return parseQuestions(stripFences(text));
    }

    private String buildPrompt(Skill skill, String targetLevel, String currentLevel, int count, List<String> previousQuestionTexts) {
        StringBuilder sb = new StringBuilder();
        sb.append("Generate ").append(count).append(" professional multiple-choice assessment questions.\n");
        sb.append("Target Skill: ").append(skill.getName()).append("\n");
        if (skill.getCategory() != null) {
            sb.append("Category: ").append(skill.getCategory().getName()).append("\n");
        }
        sb.append("Target Assessment Level: ").append(targetLevel != null ? targetLevel : "Intermediate").append("\n");
        if (currentLevel != null && !currentLevel.isBlank()) {
            sb.append("Current Employee Proficiency: ").append(currentLevel).append("\n");
        }

        int easyCount = (int) Math.round(count * 0.3);
        int hardCount = (int) Math.round(count * 0.3);
        int mediumCount = Math.max(1, count - easyCount - hardCount);

        sb.append("Difficulty Distribution: ").append(easyCount).append(" EASY, ")
                .append(mediumCount).append(" MEDIUM, ")
                .append(hardCount).append(" HARD.\n");

        if (previousQuestionTexts != null && !previousQuestionTexts.isEmpty()) {
            sb.append("\nIMPORTANT - AVOID DUPLICATES:\n");
            sb.append("The employee recently completed attempts containing the following questions. DO NOT reuse or rephrase these questions. Generate distinctly different questions testing different subtopics or scenarios:\n");
            int limit = Math.min(15, previousQuestionTexts.size());
            for (int i = 0; i < limit; i++) {
                sb.append("- ").append(previousQuestionTexts.get(i).trim()).append("\n");
            }
        }

        return sb.toString();
    }

    private List<GeneratedQuestion> parseQuestions(String json) throws Exception {
        JsonNode root = mapper.readTree(json);
        JsonNode questionsArray = root.path("questions");
        if (!questionsArray.isArray() || questionsArray.isEmpty()) {
            throw new IllegalStateException("JSON does not contain a 'questions' array");
        }

        List<GeneratedQuestion> result = new ArrayList<>();
        for (JsonNode qNode : questionsArray) {
            String questionText = qNode.path("questionText").asText("").trim();
            String subtopic = qNode.path("subtopic").asText("General").trim();
            String difficulty = qNode.path("difficulty").asText("MEDIUM").trim().toUpperCase();
            String explanation = qNode.path("explanation").asText("").trim();
            int correctAnswerIndex = qNode.path("correctAnswerIndex").asInt(-1);

            List<String> options = new ArrayList<>();
            JsonNode optNode = qNode.path("options");
            if (optNode.isArray()) {
                for (JsonNode o : optNode) {
                    options.add(o.asText("").trim());
                }
            }

            // Fallback: check if correctAnswer was supplied as a string instead of index
            if (correctAnswerIndex < 0 || correctAnswerIndex >= options.size()) {
                String correctAnswerStr = qNode.path("correctAnswer").asText("").trim();
                if (!correctAnswerStr.isEmpty()) {
                    for (int i = 0; i < options.size(); i++) {
                        if (options.get(i).equalsIgnoreCase(correctAnswerStr)) {
                            correctAnswerIndex = i;
                            break;
                        }
                    }
                }
            }

            result.add(new GeneratedQuestion(
                    questionText,
                    subtopic,
                    difficulty,
                    options,
                    correctAnswerIndex,
                    explanation,
                    1
            ));
        }

        return result;
    }

    public List<GeneratedQuestion> validateAndFilterQuestions(List<GeneratedQuestion> candidateQuestions,
                                                             Skill skill,
                                                             int desiredCount,
                                                             List<String> previousQuestionTexts) {
        List<GeneratedQuestion> validated = new ArrayList<>();
        Set<String> seenNormalizedTexts = new HashSet<>();

        for (GeneratedQuestion q : candidateQuestions) {
            if (q.questionText() == null || q.questionText().length() < 10) {
                log.debug("Skipping question with blank or too short text: {}", q.questionText());
                continue;
            }

            if (q.options() == null || q.options().size() != 4) {
                log.debug("Skipping question without exactly 4 options: {}", q.questionText());
                continue;
            }

            // Ensure all 4 options are distinct and non-empty
            Set<String> distinctOptions = new HashSet<>();
            boolean hasEmptyOption = false;
            for (String opt : q.options()) {
                if (opt == null || opt.isBlank()) {
                    hasEmptyOption = true;
                    break;
                }
                distinctOptions.add(opt.toLowerCase().trim());
            }
            if (hasEmptyOption || distinctOptions.size() != 4) {
                log.debug("Skipping question with duplicate or empty options: {}", q.questionText());
                continue;
            }

            if (q.correctAnswerIndex() < 0 || q.correctAnswerIndex() > 3) {
                log.debug("Skipping question with invalid correct answer index ({}): {}", q.correctAnswerIndex(), q.questionText());
                continue;
            }

            String diff = q.difficulty() != null ? q.difficulty().toUpperCase() : "MEDIUM";
            if (!ALLOWED_DIFFICULTIES.contains(diff)) {
                diff = "MEDIUM";
            }

            String norm = normalizeText(q.questionText());
            if (seenNormalizedTexts.contains(norm)) {
                log.debug("Skipping within-set duplicate question: {}", q.questionText());
                continue;
            }

            // Check against previous questions history
            if (isDuplicateOfPrevious(norm, previousQuestionTexts)) {
                log.info("Detected duplicate of previous attempt question, skipping: {}", q.questionText());
                continue;
            }

            seenNormalizedTexts.add(norm);
            validated.add(new GeneratedQuestion(
                    q.questionText(),
                    q.subtopic() != null && !q.subtopic().isBlank() ? q.subtopic() : skill.getName(),
                    diff,
                    q.options(),
                    q.correctAnswerIndex(),
                    q.explanation() != null && !q.explanation().isBlank() ? q.explanation() : "Evaluated based on standard best practices.",
                    q.points() > 0 ? q.points() : 1
            ));

            if (validated.size() == desiredCount) {
                break;
            }
        }

        return validated;
    }

    public static String normalizeText(String text) {
        if (text == null) return "";
        return text.toLowerCase()
                .replaceAll("[^a-z0-9\\s]", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }

    public static boolean isDuplicateOfPrevious(String normalizedCandidate, List<String> previousQuestionTexts) {
        if (previousQuestionTexts == null || previousQuestionTexts.isEmpty() || normalizedCandidate.isEmpty()) {
            return false;
        }

        Set<String> candidateTokens = tokenize(normalizedCandidate);
        if (candidateTokens.isEmpty()) return false;

        for (String prev : previousQuestionTexts) {
            String normPrev = normalizeText(prev);
            if (normPrev.equals(normalizedCandidate)) {
                return true;
            }

            // Jaccard token similarity check
            Set<String> prevTokens = tokenize(normPrev);
            double similarity = computeJaccardSimilarity(candidateTokens, prevTokens);
            if (similarity >= 0.70) {
                return true;
            }
        }

        return false;
    }

    private static Set<String> tokenize(String norm) {
        Set<String> stopWords = Set.of(
                "what", "which", "is", "the", "a", "an", "of", "in", "to", "for",
                "and", "with", "how", "does", "do", "when", "by", "from", "on", "are"
        );
        String[] words = norm.split(" ");
        Set<String> tokens = new HashSet<>();
        for (String w : words) {
            if (w.length() > 2 && !stopWords.contains(w)) {
                tokens.add(w);
            }
        }
        return tokens;
    }

    private static double computeJaccardSimilarity(Set<String> s1, Set<String> s2) {
        if (s1.isEmpty() || s2.isEmpty()) return 0.0;
        Set<String> intersection = new HashSet<>(s1);
        intersection.retainAll(s2);
        Set<String> union = new HashSet<>(s1);
        union.addAll(s2);
        return (double) intersection.size() / union.size();
    }

    private ObjectNode schema() {
        ObjectNode root = mapper.createObjectNode().put("type", "OBJECT");
        ObjectNode props = root.putObject("properties");
        props.putObject("skill").put("type", "STRING");
        props.putObject("level").put("type", "STRING");

        ObjectNode qItem = props.putObject("questions").put("type", "ARRAY")
                .putObject("items").put("type", "OBJECT");
        ObjectNode qProps = qItem.putObject("properties");

        qProps.putObject("questionText").put("type", "STRING");
        qProps.putObject("subtopic").put("type", "STRING");
        qProps.putObject("difficulty").put("type", "STRING").putArray("enum").add("EASY").add("MEDIUM").add("HARD");
        qProps.putObject("options").put("type", "ARRAY").putObject("items").put("type", "STRING");
        qProps.putObject("correctAnswerIndex").put("type", "INTEGER");
        qProps.putObject("correctAnswer").put("type", "STRING");
        qProps.putObject("explanation").put("type", "STRING");

        ArrayNode requiredQ = qItem.putArray("required");
        requiredQ.add("questionText").add("subtopic").add("difficulty").add("options").add("correctAnswerIndex").add("explanation");

        root.putArray("required").add("skill").add("questions");
        return root;
    }

    private static final String SYSTEM_INSTRUCTION = """
            You are the Chief Technical Assessor inside SkillSphere, an enterprise skill gap intelligence platform.
            Your role is to author rigorous, high-quality, practical multiple-choice skill assessment questions.
            Rules:
            - Create objective, unambiguous multiple-choice questions testing practical and conceptual mastery.
            - Each question MUST have exactly 4 distinct options.
            - Exactly ONE option must be clearly correct. The other three must be plausible distractors that represent realistic misunderstandings.
            - Never use options like "All of the above" or "None of the above".
            - 'correctAnswerIndex' must be the 0-based index (0, 1, 2, or 3) of the correct option in 'options'.
            - 'explanation' must be a concise explanation (2-3 sentences) detailing why the correct option is right and why the distractors are wrong.
            - 'difficulty' must strictly be one of: 'EASY', 'MEDIUM', 'HARD'.
            - 'subtopic' is a 1-3 word identifier of the concept tested (e.g. Streams, Indexing, Goroutines, CSS Grid).
            - Output valid JSON strictly conforming to the requested schema. Never output markdown outside the JSON.
            """;

    private static String stripFences(String s) {
        String t = s.trim();
        if (t.startsWith("```")) {
            t = t.replaceFirst("^```(?:json)?\\s*", "").replaceFirst("\\s*```$", "");
        }
        return t;
    }

    public record GeneratedQuestion(
            String questionText,
            String subtopic,
            String difficulty,
            List<String> options,
            int correctAnswerIndex,
            String explanation,
            int points
    ) {}

    public static class GeminiUnavailableException extends RuntimeException {
        public GeminiUnavailableException(String message) {
            super(message);
        }
    }

    public static class GeminiGenerationException extends RuntimeException {
        public GeminiGenerationException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
