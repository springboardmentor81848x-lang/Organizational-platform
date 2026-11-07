package com.skillsphere.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.skillsphere.entity.Skill;
import com.skillsphere.entity.SkillCategory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class GeminiAssessmentQuestionServiceTest {

    private ObjectMapper objectMapper;
    private GeminiAssessmentQuestionService service;
    private Skill skill;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        service = new GeminiAssessmentQuestionService(objectMapper, "mock-api-key", "gemini-3.5-flash", 10);
        SkillCategory category = new SkillCategory("Technical", "Technical skills");
        skill = new Skill(category, "Java", "Core Java Programming");
    }

    @Test
    @DisplayName("isConfigured returns false when API key is blank")
    void testIsConfigured_BlankKey_ReturnsFalse() {
        GeminiAssessmentQuestionService unconfigured = new GeminiAssessmentQuestionService(objectMapper, "", "gemini-3.5-flash", 10);
        assertFalse(unconfigured.isConfigured());

        assertThrows(GeminiAssessmentQuestionService.GeminiUnavailableException.class, () ->
                unconfigured.generateQuestions(skill, "Intermediate", "Beginner", 5, List.of()));
    }

    @Test
    @DisplayName("isConfigured returns true when API key is present")
    void testIsConfigured_ValidKey_ReturnsTrue() {
        assertTrue(service.isConfigured());
    }

    @Test
    @DisplayName("normalizeText normalizes case, removes punctuation and collapses whitespace")
    void testNormalizeText() {
        String input = "What is a HashMap, and how does it work in Java???";
        String normalized = GeminiAssessmentQuestionService.normalizeText(input);
        assertEquals("what is a hashmap and how does it work in java", normalized);
    }

    @Test
    @DisplayName("isDuplicateOfPrevious detects identical and highly similar paraphrased questions")
    void testIsDuplicateOfPrevious() {
        List<String> previous = List.of(
                "Which collection guarantees uniqueness of elements in Java?",
                "Explain the internal implementation of HashMap in Java"
        );

        // Identical
        assertTrue(GeminiAssessmentQuestionService.isDuplicateOfPrevious(
                GeminiAssessmentQuestionService.normalizeText("Which collection guarantees uniqueness of elements in Java?"),
                previous
        ));

        // Paraphrased with high token overlap
        assertTrue(GeminiAssessmentQuestionService.isDuplicateOfPrevious(
                GeminiAssessmentQuestionService.normalizeText("Which Java collection guarantees the uniqueness of elements?"),
                previous
        ));

        // Completely different question
        assertFalse(GeminiAssessmentQuestionService.isDuplicateOfPrevious(
                GeminiAssessmentQuestionService.normalizeText("What is a volatile keyword in multi-threading?"),
                previous
        ));
    }

    @Test
    @DisplayName("validateAndFilterQuestions accepts valid questions and rejects defective ones")
    void testValidateAndFilterQuestions() {
        List<GeminiAssessmentQuestionService.GeneratedQuestion> candidates = List.of(
                // Valid question
                new GeminiAssessmentQuestionService.GeneratedQuestion(
                        "What is the difference between Comparable and Comparator?",
                        "Collections",
                        "MEDIUM",
                        List.of("Option 1", "Option 2", "Option 3", "Option 4"),
                        1,
                        "Comparable provides natural ordering while Comparator provides custom ordering.",
                        1
                ),
                // Invalid: only 3 options
                new GeminiAssessmentQuestionService.GeneratedQuestion(
                        "What is a functional interface in Java?",
                        "Lambdas",
                        "EASY",
                        List.of("Option 1", "Option 2", "Option 3"),
                        0,
                        "Explanation",
                        1
                ),
                // Invalid: duplicate options
                new GeminiAssessmentQuestionService.GeneratedQuestion(
                        "Which loop construct checks condition at the end?",
                        "Syntax",
                        "EASY",
                        List.of("do-while", "do-while", "for", "while"),
                        0,
                        "Explanation",
                        1
                ),
                // Invalid: invalid correctAnswerIndex
                new GeminiAssessmentQuestionService.GeneratedQuestion(
                        "What does JVM stand for in Java?",
                        "Architecture",
                        "EASY",
                        List.of("Java Virtual Machine", "Java Very Much", "Just Virtual Machine", "None"),
                        5,
                        "Explanation",
                        1
                ),
                // Valid second question
                new GeminiAssessmentQuestionService.GeneratedQuestion(
                        "Which keyword prevents method overriding in a subclass?",
                        "OOP",
                        "EASY",
                        List.of("static", "final", "abstract", "synchronized"),
                        1,
                        "The final keyword prevents method overriding.",
                        1
                )
        );

        List<GeminiAssessmentQuestionService.GeneratedQuestion> validated =
                service.validateAndFilterQuestions(candidates, skill, 5, List.of());

        assertEquals(2, validated.size());
        assertEquals("What is the difference between Comparable and Comparator?", validated.get(0).questionText());
        assertEquals("Which keyword prevents method overriding in a subclass?", validated.get(1).questionText());
    }

    @Test
    @DisplayName("validateAndFilterQuestions rejects questions that duplicate recent history")
    void testValidateAndFilterQuestions_DuplicateHistory() {
        List<String> previous = List.of("What is the difference between Comparable and Comparator?");

        List<GeminiAssessmentQuestionService.GeneratedQuestion> candidates = List.of(
                new GeminiAssessmentQuestionService.GeneratedQuestion(
                        "What is the difference between Comparable and Comparator?",
                        "Collections",
                        "MEDIUM",
                        List.of("Option A", "Option B", "Option C", "Option D"),
                        0,
                        "Explanation",
                        1
                ),
                new GeminiAssessmentQuestionService.GeneratedQuestion(
                        "What does the final keyword mean on a class?",
                        "OOP",
                        "EASY",
                        List.of("Cannot be extended", "Cannot be instantiated", "Cannot have methods", "None"),
                        0,
                        "Explanation",
                        1
                )
        );

        List<GeminiAssessmentQuestionService.GeneratedQuestion> validated =
                service.validateAndFilterQuestions(candidates, skill, 5, previous);

        assertEquals(1, validated.size());
        assertEquals("What does the final keyword mean on a class?", validated.get(0).questionText());
    }
}
