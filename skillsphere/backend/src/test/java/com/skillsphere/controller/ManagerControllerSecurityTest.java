package com.skillsphere.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.skillsphere.config.SecurityConfig;
import com.skillsphere.dto.*;
import com.skillsphere.security.JsonErrorWriter;
import com.skillsphere.service.ManagerService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ManagerController.class)
@Import({SecurityConfig.class, JsonErrorWriter.class})
@TestPropertySource(properties = {
        "app.cors.allowed-origins=http://localhost:5173"
})
class ManagerControllerSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ManagerService managerService;

    @MockBean
    private UserDetailsService userDetailsService;

    @Test
    @DisplayName("GET /api/manager/overview returns 401 when unauthenticated")
    void testOverview_Unauthenticated_ReturnsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/manager/overview"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("GET /api/manager/overview returns 403 for EMPLOYEE role")
    @WithMockUser(username = "emp@skillsphere.com", roles = "EMPLOYEE")
    void testOverview_Employee_ReturnsForbidden() throws Exception {
        mockMvc.perform(get("/api/manager/overview"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /api/manager/overview returns 403 for MENTOR role")
    @WithMockUser(username = "mentor@skillsphere.com", roles = "MENTOR")
    void testOverview_Mentor_ReturnsForbidden() throws Exception {
        mockMvc.perform(get("/api/manager/overview"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /api/manager/overview returns 200 for MANAGER role")
    @WithMockUser(username = "manager@skillsphere.com", roles = "MANAGER")
    void testOverview_Manager_ReturnsOk() throws Exception {
        ManagerDashboardOverviewDto dto = new ManagerDashboardOverviewDto(
                1, 5, 12, 85.0,
                3, 1, 1, 1, 0,
                1, 4, 2, 50.0,
                2, Map.of("Beginner", 2, "Intermediate", 3),
                List.of(), List.of(), List.of()
        );
        when(managerService.getDashboardOverview(eq("manager@skillsphere.com"), any())).thenReturn(dto);

        mockMvc.perform(get("/api/manager/overview"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /api/manager/overview returns 200 for ADMIN role")
    @WithMockUser(username = "admin@skillsphere.com", roles = "ADMIN")
    void testOverview_Admin_ReturnsOk() throws Exception {
        ManagerDashboardOverviewDto dto = new ManagerDashboardOverviewDto(
                2, 10, 20, 90.0,
                2, 0, 1, 1, 0,
                0, 5, 4, 80.0,
                3, Map.of("Advanced", 4),
                List.of(), List.of(), List.of()
        );
        when(managerService.getDashboardOverview(eq("admin@skillsphere.com"), any())).thenReturn(dto);

        mockMvc.perform(get("/api/manager/overview"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("POST /api/manager/reassessments returns 403 without CSRF token")
    @WithMockUser(username = "manager@skillsphere.com", roles = "MANAGER")
    void testCreateReassessment_WithoutCsrf_ReturnsForbidden() throws Exception {
        CreateReassessmentRequest req = new CreateReassessmentRequest(2L, 5L, "Performance review gap");

        mockMvc.perform(post("/api/manager/reassessments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("POST /api/manager/reassessments returns 400 when validation fails")
    @WithMockUser(username = "manager@skillsphere.com", roles = "MANAGER")
    void testCreateReassessment_InvalidPayload_ReturnsBadRequest() throws Exception {
        // null employeeId, blank reason
        CreateReassessmentRequest req = new CreateReassessmentRequest(null, 5L, "");

        mockMvc.perform(post("/api/manager/reassessments")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /api/manager/reassessments returns 201 on valid request")
    @WithMockUser(username = "manager@skillsphere.com", roles = "MANAGER")
    void testCreateReassessment_Valid_ReturnsCreated() throws Exception {
        CreateReassessmentRequest req = new CreateReassessmentRequest(2L, 5L, "Need verification after project");
        ReassessmentRequestDto res = new ReassessmentRequestDto(
                1L, 10L, "Bob Manager", 2L, "Alice Employee", "alice@example.com",
                5L, "Java", "Backend", "Need verification after project",
                "PENDING", Instant.now(), Instant.now()
        );
        when(managerService.createReassessmentRequest(eq("manager@skillsphere.com"), any(CreateReassessmentRequest.class)))
                .thenReturn(res);

        mockMvc.perform(post("/api/manager/reassessments")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.status").value("PENDING"));
    }

    @Test
    @DisplayName("POST /api/manager/course-recommendations returns 201 on valid request")
    @WithMockUser(username = "manager@skillsphere.com", roles = "MANAGER")
    void testRecommendCourse_Valid_ReturnsCreated() throws Exception {
        CreateManagerCourseRecommendationRequest req = new CreateManagerCourseRecommendationRequest(2L, 10L, 5L, "Great course for Spring Boot");
        ManagerCourseRecommendationDto res = new ManagerCourseRecommendationDto(
                1L, 10L, "Bob Manager", 2L, "Alice Employee",
                10L, "Spring Boot Advanced", "Intermediate", "SkillSphere Academy", "https://course",
                5L, "Spring Boot", "Great course for Spring Boot",
                "RECOMMENDED", Instant.now(), Instant.now()
        );
        when(managerService.recommendCourse(eq("manager@skillsphere.com"), any(CreateManagerCourseRecommendationRequest.class)))
                .thenReturn(res);

        mockMvc.perform(post("/api/manager/course-recommendations")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.courseTitle").value("Spring Boot Advanced"));
    }

    @Test
    @DisplayName("GET /api/manager/reports/export returns text/csv with attachment header")
    @WithMockUser(username = "manager@skillsphere.com", roles = "MANAGER")
    void testExportReportCsv_ReturnsCsv() throws Exception {
        String csvContent = "Employee,Skill,Gap Size\nAlice,Java,2\n";
        when(managerService.getReportCsv(eq("manager@skillsphere.com"), eq("TEAM_SKILL_GAP_SUMMARY"), any()))
                .thenReturn(csvContent);

        mockMvc.perform(get("/api/manager/reports/export?reportType=TEAM_SKILL_GAP_SUMMARY"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", org.hamcrest.Matchers.containsString("attachment; filename=\"team_skill_gap_summary_")))
                .andExpect(content().contentType("text/csv;charset=UTF-8"))
                .andExpect(content().string(csvContent));
    }
}
