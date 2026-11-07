package com.skillsphere.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.skillsphere.config.SecurityConfig;
import com.skillsphere.dto.MentorDtos.*;
import com.skillsphere.dto.MentorshipRequestResponseDto;
import com.skillsphere.dto.RespondMentorshipRequestDto;
import com.skillsphere.security.JsonErrorWriter;
import com.skillsphere.service.MentorDashboardService;
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
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(MentorDashboardController.class)
@Import({SecurityConfig.class, JsonErrorWriter.class})
@TestPropertySource(properties = {
        "app.cors.allowed-origins=http://localhost:5173"
})
class MentorDashboardControllerSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private MentorDashboardService mentorDashboardService;

    @MockBean
    private UserDetailsService userDetailsService;

    @Test
    @DisplayName("GET /api/mentor/overview returns 401 when unauthenticated")
    void testOverview_Unauthenticated_ReturnsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/mentor/overview"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("GET /api/mentor/overview returns 403 for EMPLOYEE role")
    @WithMockUser(username = "emp@skillsphere.com", roles = "EMPLOYEE")
    void testOverview_Employee_ReturnsForbidden() throws Exception {
        mockMvc.perform(get("/api/mentor/overview"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /api/mentor/overview returns 403 for MANAGER role")
    @WithMockUser(username = "mgr@skillsphere.com", roles = "MANAGER")
    void testOverview_Manager_ReturnsForbidden() throws Exception {
        mockMvc.perform(get("/api/mentor/overview"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /api/mentor/overview returns 200 for MENTOR role")
    @WithMockUser(username = "mentor@skillsphere.com", roles = "MENTOR")
    void testOverview_Mentor_ReturnsOk() throws Exception {
        MentorProfileDto profile = new MentorProfileDto(
                1L, 4L, "Elena Rostova", "mentor@skillsphere.com",
                "Senior Staff Engineer", "Bio", "Engineering", "Principal",
                "Expert", "Java, Architecture", List.of("Java", "Architecture"),
                5, 2, true, "VIRTUAL", List.of()
        );

        MentorDashboardOverviewDto overview = new MentorDashboardOverviewDto(
                profile, 2, 1, 5, 2, 75.0, 4, 3,
                List.of(), List.of(), List.of(), List.of()
        );

        when(mentorDashboardService.getOverview("mentor@skillsphere.com")).thenReturn(overview);

        mockMvc.perform(get("/api/mentor/overview"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.activeMenteesCount").value(2))
                .andExpect(jsonPath("$.goalCompletionRate").value(75.0));
    }

    @Test
    @DisplayName("GET /api/mentor/overview returns 200 for ADMIN role")
    @WithMockUser(username = "admin@skillsphere.com", roles = "ADMIN")
    void testOverview_Admin_ReturnsOk() throws Exception {
        MentorProfileDto profile = new MentorProfileDto(
                1L, 1L, "Admin User", "admin@skillsphere.com",
                "Platform Admin", "Bio", "IT", "Admin",
                "Expert", "Leadership", List.of("Leadership"),
                10, 0, true, "VIRTUAL", List.of()
        );
        MentorDashboardOverviewDto overview = new MentorDashboardOverviewDto(
                profile, 0, 0, 0, 0, 0.0, 0, 0,
                List.of(), List.of(), List.of(), List.of()
        );

        when(mentorDashboardService.getOverview("admin@skillsphere.com")).thenReturn(overview);

        mockMvc.perform(get("/api/mentor/overview"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("POST /api/mentor/goals requires CSRF token")
    @WithMockUser(username = "mentor@skillsphere.com", roles = "MENTOR")
    void testCreateGoal_WithoutCsrf_ReturnsForbidden() throws Exception {
        CreateMentorGoalRequest req = new CreateMentorGoalRequest(
                1L, "Master Spring Security", "Complete CSRF tutorial",
                LocalDate.now().plusDays(14), "HIGH", null
        );

        mockMvc.perform(post("/api/mentor/goals")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("POST /api/mentor/goals succeeds with CSRF and MENTOR role")
    @WithMockUser(username = "mentor@skillsphere.com", roles = "MENTOR")
    void testCreateGoal_WithCsrf_ReturnsCreated() throws Exception {
        CreateMentorGoalRequest req = new CreateMentorGoalRequest(
                1L, "Master Spring Security", "Complete CSRF tutorial",
                LocalDate.now().plusDays(14), "HIGH", null
        );

        MentorGoalDetailDto dto = new MentorGoalDetailDto(
                10L, 1L, 2L, "Jane Doe", "Master Spring Security",
                "Complete CSRF tutorial", LocalDate.now().plusDays(14),
                "IN_PROGRESS", "HIGH", null, null, Instant.now(), List.of()
        );

        when(mentorDashboardService.createGoal(eq("mentor@skillsphere.com"), any(CreateMentorGoalRequest.class)))
                .thenReturn(dto);

        mockMvc.perform(post("/api/mentor/goals")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.title").value("Master Spring Security"));
    }

    @Test
    @DisplayName("POST /api/mentor/sessions creates session with MENTOR role")
    @WithMockUser(username = "mentor@skillsphere.com", roles = "MENTOR")
    void testCreateSession_WithCsrf_ReturnsCreated() throws Exception {
        CreateMentorSessionRequest req = new CreateMentorSessionRequest(
                1L, "Architecture Sync", "Review microservices design",
                Instant.now().plusSeconds(3600), 45, "https://meet.google.com/test"
        );

        MentorSessionSummaryDto dto = new MentorSessionSummaryDto(
                20L, 1L, 2L, "Jane Doe", "Architecture Sync",
                "Review microservices design", Instant.now().plusSeconds(3600),
                45, "https://meet.google.com/test", "SCHEDULED", null, null
        );

        when(mentorDashboardService.createSession(eq("mentor@skillsphere.com"), any(CreateMentorSessionRequest.class)))
                .thenReturn(dto);

        mockMvc.perform(post("/api/mentor/sessions")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(20))
                .andExpect(jsonPath("$.title").value("Architecture Sync"));
    }

    @Test
    @DisplayName("PUT /api/mentor/requests/{id}/respond succeeds for MENTOR role")
    @WithMockUser(username = "mentor@skillsphere.com", roles = "MENTOR")
    void testRespondRequest_WithCsrf_ReturnsOk() throws Exception {
        RespondMentorshipRequestDto req = new RespondMentorshipRequestDto("ACCEPTED", "Welcome!");
        MentorshipRequestResponseDto res = new MentorshipRequestResponseDto(
                5L, 4L, "Elena", "elena@skillsphere.com", "Principal Engineer",
                2L, "Jane", "jane@skillsphere.com", "Developer", "Help me",
                "ACCEPTED", Instant.now(), Instant.now(), "Welcome!"
        );

        when(mentorDashboardService.respondToRequest(eq("mentor@skillsphere.com"), eq(5L), any(RespondMentorshipRequestDto.class)))
                .thenReturn(res);

        mockMvc.perform(put("/api/mentor/requests/5/respond")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACCEPTED"));
    }

    @Test
    @DisplayName("GET /api/mentor/mentees/assignments/{id}/dossier returns 403 for EMPLOYEE role")
    @WithMockUser(username = "emp@skillsphere.com", roles = "EMPLOYEE")
    void testDossier_Employee_ReturnsForbidden() throws Exception {
        mockMvc.perform(get("/api/mentor/mentees/assignments/1/dossier"))
                .andExpect(status().isForbidden());
    }
}
