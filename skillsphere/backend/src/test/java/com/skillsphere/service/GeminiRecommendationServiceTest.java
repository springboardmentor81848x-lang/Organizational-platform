package com.skillsphere.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.skillsphere.dto.CourseDto;
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

import java.net.http.HttpClient;
import java.net.http.HttpHeaders;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GeminiRecommendationServiceTest {

    @Mock
    private CourseRepository courseRepository;

    @Mock
    private HttpClient httpClient;

    @Mock
    private HttpResponse<String> httpResponse;

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
    @DisplayName("getActiveCatalogue safely maps courses to DTOs isolating lazy proxies")
    void testGetActiveCatalogue_IsolatesLazyProxies() {
        when(courseRepository.findAllActiveWithSkill()).thenReturn(List.of(javaCourse, sqlCourse));

        List<CourseDto> dtos = recommendationService.getActiveCatalogue();

        assertNotNull(dtos);
        assertEquals(2, dtos.size());
        assertEquals("Enterprise Java 17 Masterclass", dtos.get(0).title());
        assertEquals("Java", dtos.get(0).skillName());
        assertEquals("SQL for Data Science", dtos.get(1).title());
        assertEquals("SQL", dtos.get(1).skillName());
    }

    @Test
    @DisplayName("recommend with no API key uses catalogue-backed fallback prioritizing actual skill gaps")
    void testFallback_UsesCatalogueAndPrioritizesGaps() {
        when(courseRepository.findAllActiveWithSkill()).thenReturn(List.of(javaCourse, sqlCourse));

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

    @Test
    @DisplayName("recommend with empty course repository returns controlled empty response")
    void testFallback_WhenCourseRepositoryIsEmpty() {
        when(courseRepository.findAllActiveWithSkill()).thenReturn(Collections.emptyList());
        when(courseRepository.findAllWithSkill()).thenReturn(Collections.emptyList());

        RecommendationRequest req = new RecommendationRequest(
                "Software Engineer",
                "Entry",
                "Engineering",
                70,
                List.of(new RecommendationRequest.SkillResult("Java", 2, 5, 2, 4))
        );

        RecommendationResponse response = recommendationService.recommend("user-test-empty", req);

        assertNotNull(response);
        assertTrue(response.recommendations().isEmpty());
        assertEquals("rules", response.source());
        assertTrue(response.summary().contains("No learning catalogue courses"));
        assertEquals("Course catalogue is currently empty.", response.notice());
    }

    @Test
    @DisplayName("recommend serves cached response on rapid calls within interval window")
    void testRateLimitThrottling_ServesCachedResponseGracefully() {
        when(courseRepository.findAllActiveWithSkill()).thenReturn(List.of(javaCourse));

        RecommendationRequest req = new RecommendationRequest(
                "Backend Developer",
                "Junior",
                "IT",
                65,
                List.of(new RecommendationRequest.SkillResult("Java", 2, 5, 2, 4))
        );

        RecommendationResponse first = recommendationService.recommend("user-throttle-test", req);
        assertNotNull(first);

        // Immediate subsequent call within MIN_INTERVAL_MS should return cached without re-querying repository
        RecommendationResponse second = recommendationService.recommend("user-throttle-test", req);
        assertNotNull(second);
        assertSame(first, second);
        verify(courseRepository, times(1)).findAllActiveWithSkill();
    }

    @Test
    @DisplayName("recommend falls back to catalogue when Gemini returns HTTP 429 rate limit")
    void testGemini_WhenUpstreamReturns429_FallsBackToCatalogue() throws Exception {
        GeminiRecommendationService serviceWithKey = new GeminiRecommendationService(
                mapper, courseRepository, "dummy-gemini-key", "gemini-2.5-flash", 10
        );
        ReflectionTestUtils.setField(serviceWithKey, "http", httpClient);

        when(courseRepository.findAllActiveWithSkill()).thenReturn(List.of(javaCourse));

        HttpHeaders headers = HttpHeaders.of(Map.of("Retry-After", List.of("30")), (k, v) -> true);
        when(httpResponse.statusCode()).thenReturn(429);
        when(httpResponse.headers()).thenReturn(headers);
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(httpResponse);

        RecommendationRequest req = new RecommendationRequest(
                "Backend Developer",
                "Junior",
                "IT",
                65,
                List.of(new RecommendationRequest.SkillResult("Java", 2, 5, 2, 4))
        );

        RecommendationResponse response = serviceWithKey.recommend("user-gemini-429", req);

        assertNotNull(response);
        assertEquals("rules", response.source());
        assertTrue(response.notice().contains("Gemini rate limit reached"));
        assertFalse(response.recommendations().isEmpty());
        assertEquals("Enterprise Java 17 Masterclass", response.recommendations().get(0).title());
    }

    @Test
    @DisplayName("recommend falls back to catalogue when Gemini returns HTTP 500 error")
    void testGemini_WhenUpstreamReturns500_FallsBackToCatalogue() throws Exception {
        GeminiRecommendationService serviceWithKey = new GeminiRecommendationService(
                mapper, courseRepository, "dummy-gemini-key", "gemini-2.5-flash", 10
        );
        ReflectionTestUtils.setField(serviceWithKey, "http", httpClient);

        when(courseRepository.findAllActiveWithSkill()).thenReturn(List.of(javaCourse));

        when(httpResponse.statusCode()).thenReturn(500);
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(httpResponse);

        RecommendationRequest req = new RecommendationRequest(
                "Backend Developer",
                "Junior",
                "IT",
                65,
                List.of(new RecommendationRequest.SkillResult("Java", 2, 5, 2, 4))
        );

        RecommendationResponse response = serviceWithKey.recommend("user-gemini-500", req);

        assertNotNull(response);
        assertEquals("rules", response.source());
        assertTrue(response.notice().contains("AI service temporarily unavailable"));
        assertFalse(response.recommendations().isEmpty());
    }

    @Test
    @DisplayName("recommend falls back to catalogue when Gemini request times out")
    void testGemini_WhenTimeoutOccurs_FallsBackToCatalogue() throws Exception {
        GeminiRecommendationService serviceWithKey = new GeminiRecommendationService(
                mapper, courseRepository, "dummy-gemini-key", "gemini-2.5-flash", 10
        );
        ReflectionTestUtils.setField(serviceWithKey, "http", httpClient);

        when(courseRepository.findAllActiveWithSkill()).thenReturn(List.of(javaCourse));

        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenThrow(new HttpTimeoutException("Request timed out"));

        RecommendationRequest req = new RecommendationRequest(
                "Backend Developer",
                "Junior",
                "IT",
                65,
                List.of(new RecommendationRequest.SkillResult("Java", 2, 5, 2, 4))
        );

        RecommendationResponse response = serviceWithKey.recommend("user-gemini-timeout", req);

        assertNotNull(response);
        assertEquals("rules", response.source());
        assertTrue(response.notice().contains("timed out"));
        assertFalse(response.recommendations().isEmpty());
    }

    @Test
    @DisplayName("recommend isolates cache entries per employee so User B never accesses User A's cache")
    void testDataIsolation_EmployeeCannotRetrieveAnotherEmployeesCachedRecommendations() {
        when(courseRepository.findAllActiveWithSkill()).thenReturn(List.of(javaCourse));

        RecommendationRequest req = new RecommendationRequest(
                "Backend Developer",
                "Junior",
                "IT",
                65,
                List.of(new RecommendationRequest.SkillResult("Java", 2, 5, 2, 4))
        );

        RecommendationResponse userA = recommendationService.recommend("userA@skillsphere.com", req);
        RecommendationResponse userB = recommendationService.recommend("userB@skillsphere.com", req);

        assertNotNull(userA);
        assertNotNull(userB);
        assertNotSame(userA, userB);
        verify(courseRepository, times(2)).findAllActiveWithSkill();
    }

    @Test
    @DisplayName("recommend bypasses cache when employee submits new assessment results or changed skill gaps")
    void testCacheBypass_WhenEmployeeSubmitsNewAssessmentOrSkillGaps() {
        when(courseRepository.findAllActiveWithSkill()).thenReturn(List.of(javaCourse, sqlCourse));

        RecommendationRequest req1 = new RecommendationRequest(
                "Backend Developer",
                "Junior",
                "IT",
                60,
                List.of(new RecommendationRequest.SkillResult("Java", 2, 5, 2, 4))
        );

        RecommendationRequest req2 = new RecommendationRequest(
                "Backend Developer",
                "Junior",
                "IT",
                85,
                List.of(
                        new RecommendationRequest.SkillResult("Java", 4, 5, 4, 4),
                        new RecommendationRequest.SkillResult("SQL", 2, 5, 2, 4)
                )
        );

        RecommendationResponse first = recommendationService.recommend("user-repeat@skillsphere.com", req1);
        RecommendationResponse second = recommendationService.recommend("user-repeat@skillsphere.com", req2);

        assertNotNull(first);
        assertNotNull(second);
        assertNotSame(first, second);
        verify(courseRepository, times(2)).findAllActiveWithSkill();
    }

    @Test
    @DisplayName("recommend returns parsed AI recommendations when Gemini succeeds")
    void testGemini_WhenSuccess_ReturnsAiRecommendations() throws Exception {
        GeminiRecommendationService serviceWithKey = new GeminiRecommendationService(
                mapper, courseRepository, "dummy-gemini-key", "gemini-2.5-flash", 10
        );
        ReflectionTestUtils.setField(serviceWithKey, "http", httpClient);

        when(courseRepository.findAllActiveWithSkill()).thenReturn(List.of(javaCourse));

        String geminiJson = """
                {
                  "candidates": [
                    {
                      "content": {
                        "parts": [
                          {
                            "text": "{\\"summary\\": \\"Focus on mastering advanced Java.\\", \\"recommendations\\": [{\\"id\\": 10, \\"title\\": \\"Enterprise Java 17 Masterclass\\", \\"skill\\": \\"Java\\", \\"level\\": \\"Advanced\\", \\"duration\\": \\"40 hours\\", \\"priority\\": \\"High\\", \\"provider\\": \\"Coursera\\", \\"reason\\": \\"Recommended for closing your Java gap.\\", \\"topics\\": [\\"Concurrency\\", \\"Streams\\"], \\"courseUrl\\": \\"https://coursera.org/java\\", \\"thumbnailUrl\\": null}]}"
                          }
                        ]
                      }
                    }
                  ]
                }
                """;

        when(httpResponse.statusCode()).thenReturn(200);
        when(httpResponse.body()).thenReturn(geminiJson);
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(httpResponse);

        RecommendationRequest req = new RecommendationRequest(
                "Backend Developer",
                "Junior",
                "IT",
                65,
                List.of(new RecommendationRequest.SkillResult("Java", 2, 5, 2, 4))
        );

        RecommendationResponse response = serviceWithKey.recommend("user-gemini-success", req);

        assertNotNull(response);
        assertEquals("gemini", response.source());
        assertEquals("Focus on mastering advanced Java.", response.summary());
        assertEquals(1, response.recommendations().size());
        assertEquals("Enterprise Java 17 Masterclass", response.recommendations().get(0).title());
    }
}
