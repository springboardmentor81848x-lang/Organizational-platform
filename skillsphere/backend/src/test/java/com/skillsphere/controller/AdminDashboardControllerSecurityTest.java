package com.skillsphere.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.skillsphere.config.SecurityConfig;
import com.skillsphere.dto.AdminDashboardDtos.*;
import com.skillsphere.entity.AccountStatus;
import com.skillsphere.entity.Role;
import com.skillsphere.entity.User;
import com.skillsphere.repository.UserRepository;
import com.skillsphere.security.JsonErrorWriter;
import com.skillsphere.service.AdminDashboardService;
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
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AdminDashboardController.class)
@Import({SecurityConfig.class, JsonErrorWriter.class})
@TestPropertySource(properties = {
        "app.cors.allowed-origins=http://localhost:5173"
})
class AdminDashboardControllerSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AdminDashboardService adminDashboardService;

    @MockBean
    private UserRepository userRepository;

    @MockBean
    private UserDetailsService userDetailsService;

    private User adminUser;

    @BeforeEach
    void setUp() {
        adminUser = new User();
        ReflectionTestUtils.setField(adminUser, "id", 1L);
        adminUser.setFullName("Platform Administrator");
        adminUser.setEmail("admin@skillsphere.com");
        adminUser.setRole(Role.ADMIN);
        adminUser.setAccountStatus(AccountStatus.ACTIVE);

        when(userRepository.findByEmail("admin@skillsphere.com")).thenReturn(Optional.of(adminUser));
    }

    @Test
    @DisplayName("GET /api/admin/overview: 401 when unauthenticated")
    void testGetOverview_Unauthenticated_Returns401() throws Exception {
        mockMvc.perform(get("/api/admin/overview"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "emp@skillsphere.com", roles = {"EMPLOYEE"})
    @DisplayName("GET /api/admin/overview: 403 Forbidden for EMPLOYEE")
    void testGetOverview_Employee_Returns403() throws Exception {
        mockMvc.perform(get("/api/admin/overview"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "mgr@skillsphere.com", roles = {"MANAGER"})
    @DisplayName("GET /api/admin/overview: 403 Forbidden for MANAGER")
    void testGetOverview_Manager_Returns403() throws Exception {
        mockMvc.perform(get("/api/admin/overview"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "mentor@skillsphere.com", roles = {"MENTOR"})
    @DisplayName("GET /api/admin/overview: 403 Forbidden for MENTOR")
    void testGetOverview_Mentor_Returns403() throws Exception {
        mockMvc.perform(get("/api/admin/overview"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "depthead@skillsphere.com", roles = {"DEPARTMENT_HEAD"})
    @DisplayName("GET /api/admin/overview: 403 Forbidden for DEPARTMENT_HEAD")
    void testGetOverview_DeptHead_Returns403() throws Exception {
        mockMvc.perform(get("/api/admin/overview"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "admin@skillsphere.com", roles = {"ADMIN"})
    @DisplayName("GET /api/admin/overview: 200 OK for ADMIN")
    void testGetOverview_Admin_Returns200() throws Exception {
        AdminSystemHealthDto health = new AdminSystemHealthDto("HEALTHY", "CONNECTED", "PostgreSQL 18.4", 56L, 9L, "READY", "None", true, "100MB");
        AdminDashboardOverviewDto overview = new AdminDashboardOverviewDto(
                10L, 8L, 2L, Map.of("EMPLOYEE", 6L, "ADMIN", 2L), 3L, 5L, 20L, 10L, 1L, 50L,
                health, Collections.emptyList(), Collections.emptyList(), Collections.emptyList()
        );
        when(adminDashboardService.getDashboardOverview()).thenReturn(overview);

        mockMvc.perform(get("/api/admin/overview"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalUsers").value(10))
                .andExpect(jsonPath("$.systemHealth.databaseStatus").value("CONNECTED"));
    }

    @Test
    @WithMockUser(username = "admin@skillsphere.com", roles = {"ADMIN"})
    @DisplayName("POST /api/admin/users/detailed: 403 Forbidden without CSRF token")
    void testCreateUser_MissingCsrf_Returns403() throws Exception {
        CreateUserAdminRequest req = new CreateUserAdminRequest(
                "Jane Doe", "jane.doe@skillsphere.com", "EMP-900", Role.EMPLOYEE, "Engineering", "SecurePass123!"
        );

        mockMvc.perform(post("/api/admin/users/detailed")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "admin@skillsphere.com", roles = {"ADMIN"})
    @DisplayName("POST /api/admin/users/detailed: 201 Created with CSRF")
    void testCreateUser_WithCsrf_Returns201() throws Exception {
        CreateUserAdminRequest req = new CreateUserAdminRequest(
                "Jane Doe", "jane.doe@skillsphere.com", "EMP-900", Role.EMPLOYEE, "Engineering", "SecurePass123!"
        );
        AdminUserDetailDto res = new AdminUserDetailDto(
                900L, "Jane Doe", "jane.doe@skillsphere.com", "EMP-900", Role.EMPLOYEE, null, AccountStatus.ACTIVE,
                "Engineering", "Employee", Instant.now(), Instant.now()
        );
        when(adminDashboardService.createUser(any(CreateUserAdminRequest.class), anyString(), anyString())).thenReturn(res);

        mockMvc.perform(post("/api/admin/users/detailed")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value("jane.doe@skillsphere.com"))
                .andExpect(jsonPath("$.role").value("EMPLOYEE"));
    }

    @Test
    @WithMockUser(username = "admin@skillsphere.com", roles = {"ADMIN"})
    @DisplayName("GET /api/admin/communication-settings: 200 OK for ADMIN")
    void testGetCommunicationSettings_Admin_Returns200() throws Exception {
        CommunicationSettingsDto dto = new CommunicationSettingsDto(
                10, "pdf,png,jpg", 365, true, 25L, 150L, "Privacy Notice"
        );
        when(adminDashboardService.getCommunicationSettings()).thenReturn(dto);

        mockMvc.perform(get("/api/admin/communication-settings"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.maxUploadSizeMb").value(10))
                .andExpect(jsonPath("$.totalConversationsCount").value(25));
    }

    @Test
    @WithMockUser(username = "admin@skillsphere.com", roles = {"ADMIN"})
    @DisplayName("GET /api/admin/security/overview: 200 OK for ADMIN")
    void testGetSecurityOverview_Admin_Returns200() throws Exception {
        AdminSecurityOverviewDto dto = new AdminSecurityOverviewDto(
                List.of("locked@skillsphere.com"), 2, 10L, 1L, Collections.emptyList()
        );
        when(adminDashboardService.getSecurityOverview()).thenReturn(dto);

        mockMvc.perform(get("/api/admin/security/overview"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currentlyLockedAccounts[0]").value("locked@skillsphere.com"))
                .andExpect(jsonPath("$.recentFailedAttemptsCount").value(2));
    }
}
