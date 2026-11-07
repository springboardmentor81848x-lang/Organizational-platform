package com.skillsphere.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.skillsphere.config.SecurityConfig;
import com.skillsphere.dto.DepartmentHeadDtos.*;
import com.skillsphere.entity.Role;
import com.skillsphere.entity.User;
import com.skillsphere.exception.ApiException;
import com.skillsphere.repository.UserRepository;
import com.skillsphere.security.JsonErrorWriter;
import com.skillsphere.service.DepartmentHeadService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(DepartmentHeadController.class)
@Import({SecurityConfig.class, JsonErrorWriter.class})
@TestPropertySource(properties = {
        "app.cors.allowed-origins=http://localhost:5173"
})
class DepartmentHeadControllerSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private DepartmentHeadService departmentHeadService;

    @MockBean
    private UserRepository userRepository;

    @MockBean
    private UserDetailsService userDetailsService;

    private User deptHeadUser;
    private User adminUser;

    @BeforeEach
    void setUp() {
        deptHeadUser = new User();
        deptHeadUser.setFullName("David Director");
        deptHeadUser.setEmail("head@skillsphere.com");
        deptHeadUser.setRole(Role.DEPARTMENT_HEAD);
        ReflectionTestUtils.setField(deptHeadUser, "id", 10L);

        adminUser = new User();
        adminUser.setFullName("SkillSphere Administrator");
        adminUser.setEmail("admin@skillsphere.com");
        adminUser.setRole(Role.ADMIN);
        ReflectionTestUtils.setField(adminUser, "id", 1L);

        when(userRepository.findByEmail("head@skillsphere.com")).thenReturn(Optional.of(deptHeadUser));
        when(userRepository.findByEmail("admin@skillsphere.com")).thenReturn(Optional.of(adminUser));
    }

    @Test
    @DisplayName("GET /api/department-head/overview returns 401 when unauthenticated")
    void testOverview_Unauthenticated_ReturnsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/department-head/overview"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "emp@skillsphere.com", roles = {"EMPLOYEE"})
    @DisplayName("GET /api/department-head/overview returns 403 for EMPLOYEE role")
    void testOverview_EmployeeRole_ReturnsForbidden() throws Exception {
        mockMvc.perform(get("/api/department-head/overview"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "manager@skillsphere.com", roles = {"MANAGER"})
    @DisplayName("GET /api/department-head/overview returns 403 for MANAGER role")
    void testOverview_ManagerRole_ReturnsForbidden() throws Exception {
        mockMvc.perform(get("/api/department-head/overview"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "mentor@skillsphere.com", roles = {"MENTOR"})
    @DisplayName("GET /api/department-head/overview returns 403 for MENTOR role")
    void testOverview_MentorRole_ReturnsForbidden() throws Exception {
        mockMvc.perform(get("/api/department-head/overview"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "hr@skillsphere.com", roles = {"HR_SPECIALIST"})
    @DisplayName("GET /api/department-head/overview returns 403 for HR_SPECIALIST role")
    void testOverview_HrSpecialistRole_ReturnsForbidden() throws Exception {
        mockMvc.perform(get("/api/department-head/overview"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "head@skillsphere.com", roles = {"DEPARTMENT_HEAD"})
    @DisplayName("GET /api/department-head/overview returns 200 for authorized DEPARTMENT_HEAD")
    void testOverview_DepartmentHeadRole_ReturnsOk() throws Exception {
        DepartmentHeadOverviewDto dto = new DepartmentHeadOverviewDto(
                1L, "Engineering", "ENG", "David Director", "head@skillsphere.com",
                25, 3, 3, 20, 5, 80.0, 2, 8, 2, 45, 78.0, 4,
                List.of(), List.of(), List.of()
        );
        when(departmentHeadService.getDashboardOverview(eq("head@skillsphere.com"), any())).thenReturn(dto);

        mockMvc.perform(get("/api/department-head/overview"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.departmentName").value("Engineering"))
                .andExpect(jsonPath("$.skillCoveragePercentage").value(80.0));
    }

    @Test
    @WithMockUser(username = "admin@skillsphere.com", roles = {"ADMIN"})
    @DisplayName("GET /api/department-head/overview returns 200 for ADMIN role")
    void testOverview_AdminRole_ReturnsOk() throws Exception {
        DepartmentHeadOverviewDto dto = new DepartmentHeadOverviewDto(
                1L, "Engineering", "ENG", "David Director", "head@skillsphere.com",
                25, 3, 3, 20, 5, 80.0, 2, 8, 2, 45, 78.0, 4,
                List.of(), List.of(), List.of()
        );
        when(departmentHeadService.getDashboardOverview(eq("admin@skillsphere.com"), any())).thenReturn(dto);

        mockMvc.perform(get("/api/department-head/overview"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "head@skillsphere.com", roles = {"DEPARTMENT_HEAD"})
    @DisplayName("Cross-department IDOR prevention: requesting unauthorized department ID returns 403 FORBIDDEN")
    void testCrossDepartmentIdor_ReturnsForbidden() throws Exception {
        when(departmentHeadService.getDashboardOverview(eq("head@skillsphere.com"), eq(999L)))
                .thenThrow(new ApiException(HttpStatus.FORBIDDEN, "Access denied: You are not authorized to access department 999"));

        mockMvc.perform(get("/api/department-head/overview").param("departmentId", "999"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "head@skillsphere.com", roles = {"DEPARTMENT_HEAD"})
    @DisplayName("POST /api/department-head/announcements creates department announcement with CSRF protection")
    void testCreateAnnouncement_Success() throws Exception {
        CreateDeptAnnouncementRequest req = new CreateDeptAnnouncementRequest(
                "Sprint Milestone Reminder", "Review Q4 deliverables", "HIGH"
        );
        DeptAnnouncementDto dto = new DeptAnnouncementDto(
                1L, 10L, "David Director", "Sprint Milestone Reminder",
                "Review Q4 deliverables", "HIGH", Instant.now(), 0, 25, false
        );
        when(departmentHeadService.createDepartmentAnnouncement(eq("head@skillsphere.com"), any(), any()))
                .thenReturn(dto);

        mockMvc.perform(post("/api/department-head/announcements")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("Sprint Milestone Reminder"));
    }

    @Test
    @WithMockUser(username = "head@skillsphere.com", roles = {"DEPARTMENT_HEAD"})
    @DisplayName("POST /api/department-head/announcements without CSRF token returns 403")
    void testCreateAnnouncement_NoCsrf_ReturnsForbidden() throws Exception {
        CreateDeptAnnouncementRequest req = new CreateDeptAnnouncementRequest(
                "Test Title", "Test Content", "NORMAL"
        );

        mockMvc.perform(post("/api/department-head/announcements")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isForbidden());
    }
}
