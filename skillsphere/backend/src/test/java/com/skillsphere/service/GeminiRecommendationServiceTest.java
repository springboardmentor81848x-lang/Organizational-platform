package com.skillsphere.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.skillsphere.dto.RecommendationRequest;
import com.skillsphere.dto.RecommendationResponse;
import com.skillsphere.entity.Course;
import com.skillsphere.entity.Skill;
import com.skillsphere.entity.SkillCategory;
import com.skillsphere.repository.CourseRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GeminiRecommendationServiceTest {

    @Mock
    private CourseRepository courseRepository;

    private ObjectMapper mapper;
    private GeminiRecommendationService recommendationService;

    private Course javaCourse;
    private Course sqlCourse;

    @BeforeEach
    void setUp() {
        mapper = new ObjectMapper();
        recommendationService = new GeminiRecommendationService(mapper, courseRepository, "", "gemini-2.5-flash", 10);

        SkillCategory cat = new SkillCategory("Technical", "Technical skills");
        ReflectionTestUtils.setField(cat, "id", 1L);

        Skill javaSkill = new Skill(cat, "Java", "Java Programming");
        ReflectionTestUtils.setField(javaSkill, "id", 1L);

        Skill sqlSkill = new Skill(cat, "SQL", "Relational Databases");
        ReflectionTestUtils.setField(sqlSkill, "id", 2L);

        javaCourse = new Course("Enterprise Java 17 Masterclass", "Core Java", javaSkill, "INTERMEDIATE", 40, "Coursera", "https://coursera.org/java");
        ReflectionTestUtils.setField(javaCourse, "id", 10L);

        sqlCourse = new Course("SQL for Data Science", "SQL basics", sqlSkill, "BEGINNER", 20, "Coursera", "https://coursera.org/sql");
        ReflectionTestUtils.setField(sqlCourse, "id", 20L);
    }

    @Test
    @DisplayName("recommend with no API key uses catalogue-backed fallback prioritizing actual skill gaps")
    void testFallback_UsesCatalogueAndPrioritizesGaps() {
        when(courseRepository.findAll()).thenReturn(List.of(javaCourse, sqlCourse));

        RecommendationRequest req = new RecommendationRequest(
                "Backend Engineer",
                "Intermediate",
                "Engineering",
                60,
                List.of(
                        new RecommendationRequest.SkillResult("Java", 2, 5, 2, 4), // Gap = 2
                        new RecommendationRequest.SkillResult("SQL", 4, 5, 4, 4)   // Gap = 0
                )
        );

        RecommendationResponse response = recommendationService.recommend("user-test-1", req);

        assertNotNull(response);
        assertNotNull(response.recommendations());
        assertFalse(response.recommendations().isEmpty());

        // Java has gap=2, so it should be prioritized
        RecommendationResponse.Course first = response.recommendations().get(0);
        assertEquals(10L, first.id());
        assertEquals("Enterprise Java 17 Masterclass", first.title());
        assertEquals("High", first.priority());
        assertEquals("https://coursera.org/java", first.courseUrl());
    }
}
