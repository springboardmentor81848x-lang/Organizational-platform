package com.skillsphere.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.skillsphere.config.SecurityConfig;
import com.skillsphere.dto.HrDtos.*;
import com.skillsphere.entity.Role;
import com.skillsphere.entity.User;
import com.skillsphere.repository.UserRepository;
import com.skillsphere.security.JsonErrorWriter;
import com.skillsphere.service.HrDashboardService;
import org.junit.jupiter.api.BeforeEach;
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
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(HrDashboardController.class)
@Import({SecurityConfig.class, JsonErrorWriter.class})
@TestPropertySource(properties = {
        "app.cors.allowed-origins=http://localhost:5173"
})
class HrDashboardControllerSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private HrDashboardService hrDashboardService;

    @MockBean
    private UserRepository userRepository;

    @MockBean
    private UserDetailsService userDetailsService;

    private User hrUser;
    private User managerUser;

    @BeforeEach
    void setUp() {
        hrUser = new User();
        hrUser.setFullName("Helen HR");
        hrUser.setEmail("hr@skillsphere.com");
        hrUser.setRole(Role.HR_SPECIALIST);
        org.springframework.test.util.ReflectionTestUtils.setField(hrUser, "id", 1L);

        managerUser = new User();
        managerUser.setFullName("Mary Manager");
        managerUser.setEmail("manager@skillsphere.com");
        managerUser.setRole(Role.MANAGER);
        org.springframework.test.util.ReflectionTestUtils.setField(managerUser, "id", 2L);

        when(userRepository.findByEmail("hr@skillsphere.com")).thenReturn(Optional.of(hrUser));
        when(userRepository.findByEmail("manager@skillsphere.com")).thenReturn(Optional.of(managerUser));
    }

    @Test
    @DisplayName("GET /api/hr/overview returns 401 when unauthenticated")
    void testOverview_Unauthenticated_ReturnsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/hr/overview"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "emp@skillsphere.com", roles = {"EMPLOYEE"})
    @DisplayName("GET /api/hr/overview returns 403 for EMPLOYEE role")
    void testOverview_EmployeeRole_ReturnsForbidden() throws Exception {
        mockMvc.perform(get("/api/hr/overview"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "mentor@skillsphere.com", roles = {"MENTOR"})
    @DisplayName("GET /api/hr/overview returns 403 for MENTOR role")
    void testOverview_MentorRole_ReturnsForbidden() throws Exception {
        mockMvc.perform(get("/api/hr/overview"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "manager@skillsphere.com", roles = {"MANAGER"})
    @DisplayName("GET /api/hr/overview returns 403 for MANAGER role")
    void testOverview_ManagerRole_ReturnsForbidden() throws Exception {
        mockMvc.perform(get("/api/hr/overview"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "hr@skillsphere.com", roles = {"HR_SPECIALIST"})
    @DisplayName("GET /api/hr/overview returns 200 for HR_SPECIALIST role")
    void testOverview_HrSpecialistRole_ReturnsOk() throws Exception {
        HrDashboardOverviewDto dto = new HrDashboardOverviewDto(
                50, 40, 10, 80.0, 5, 12, 4, 25, 120, 75.5,
                List.of(), List.of(), List.of()
        );
        when(hrDashboardService.getOverview()).thenReturn(dto);

        mockMvc.perform(get("/api/hr/overview"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalEmployees").value(50))
                .andExpect(jsonPath("$.skillCoveragePercentage").value(80.0));
    }

    @Test
    @WithMockUser(username = "admin@skillsphere.com", roles = {"ADMIN"})
    @DisplayName("GET /api/hr/overview returns 200 for ADMIN role")
    void testOverview_AdminRole_ReturnsOk() throws Exception {
        HrDashboardOverviewDto dto = new HrDashboardOverviewDto(
                50, 40, 10, 80.0, 5, 12, 4, 25, 120, 75.5,
                List.of(), List.of(), List.of()
        );
        when(hrDashboardService.getOverview()).thenReturn(dto);

        mockMvc.perform(get("/api/hr/overview"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "hr@skillsphere.com", roles = {"HR_SPECIALIST"})
    @DisplayName("POST /api/hr/competencies creates competency successfully")
    void testCreateCompetency_HrSpecialist_ReturnsCreated() throws Exception {
        CreateCompetencyRequest req = new CreateCompetencyRequest("Cloud Architecture", 1L, "Design AWS/GCP systems");
        HrCompetencyDto res = new HrCompetencyDto(
                10L, "Cloud Architecture", "Engineering", 1L, "Design AWS/GCP systems",
                true, 0, 0, 0, Instant.now()
        );
        when(hrDashboardService.createCompetency(any())).thenReturn(res);

        mockMvc.perform(post("/api/hr/competencies")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Cloud Architecture"));
    }

    @Test
    @WithMockUser(username = "manager@skillsphere.com", roles = {"MANAGER"})
    @DisplayName("GET /api/hr/leadership-reports/shared returns 200 for MANAGER role")
    void testSharedLeadershipReports_ManagerRole_ReturnsOk() throws Exception {
        LeadershipReportShareDto share = new LeadershipReportShareDto(
                1L, "WORKFORCE_READINESS", "Q3 Workforce Readiness",
                hrUser.getId(), "Helen HR", "MANAGER", null, "All Departments",
                "Review team gaps", "{}", Instant.now()
        );
        when(hrDashboardService.getSharedLeadershipReports(eq("MANAGER"), any())).thenReturn(List.of(share));

        mockMvc.perform(get("/api/hr/leadership-reports/shared"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].title").value("Q3 Workforce Readiness"));
    }

    @Test
    @WithMockUser(username = "emp@skillsphere.com", roles = {"EMPLOYEE"})
    @DisplayName("GET /api/hr/leadership-reports/shared returns 403 for EMPLOYEE role")
    void testSharedLeadershipReports_EmployeeRole_ReturnsForbidden() throws Exception {
        mockMvc.perform(get("/api/hr/leadership-reports/shared"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "hr@skillsphere.com", roles = {"HR_SPECIALIST"})
    @DisplayName("POST /api/hr/announcements creates announcement successfully")
    void testCreateAnnouncement_HrSpecialist_ReturnsCreated() throws Exception {
        CreateAnnouncementRequest req = new CreateAnnouncementRequest(
                "Annual Assessment Cycle",
                "Please complete baseline evaluations",
                "ORGANIZATION", null, null, "HIGH"
        );
        HrAnnouncementDto res = new HrAnnouncementDto(
                1L, hrUser.getId(), "Helen HR", "ORGANIZATION", null, "All Departments",
                null, "Annual Assessment Cycle", "Please complete baseline evaluations",
                "HIGH", Instant.now(), 0, 50, false
        );
        when(hrDashboardService.createAnnouncement(any(), anyLong())).thenReturn(res);

        mockMvc.perform(post("/api/hr/announcements")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("Annual Assessment Cycle"));
    }
}
