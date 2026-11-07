package com.skillsphere.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.skillsphere.config.SecurityConfig;
import com.skillsphere.dto.ChangePasswordRequest;
import com.skillsphere.dto.EmployeeDashboardOverviewDto;
import com.skillsphere.security.JsonErrorWriter;
import com.skillsphere.service.AuthService;
import com.skillsphere.service.EmployeeDashboardService;
import com.skillsphere.service.PasswordResetService;
import com.skillsphere.service.UserService;
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

import java.util.Collections;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest({DashboardController.class, AuthController.class})
@Import({SecurityConfig.class, JsonErrorWriter.class})
@TestPropertySource(properties = {
        "app.cors.allowed-origins=http://localhost:5173"
})
class EmployeeDashboardControllerSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private UserService userService;

    @MockBean
    private AuthService authService;

    @MockBean
    private PasswordResetService passwordResetService;

    @MockBean
    private EmployeeDashboardService employeeDashboardService;

    @MockBean
    private UserDetailsService userDetailsService;

    @Test
    @DisplayName("GET /api/dashboard/employee/overview returns 401 when unauthenticated")
    void testOverview_Unauthenticated_ReturnsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/dashboard/employee/overview"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("GET /api/dashboard/employee/overview returns 200 for authenticated EMPLOYEE")
    @WithMockUser(username = "emp@skillsphere.com", roles = "EMPLOYEE")
    void testOverview_Employee_ReturnsOk() throws Exception {
        EmployeeDashboardOverviewDto dto = new EmployeeDashboardOverviewDto(
                1L, "Alice Employee", "emp@skillsphere.com", "EMP-001", "Developer", "IT", "Mid-Level",
                5, 3, 3.8, 2, 4, 85, "Java",
                1, 3, 2, true, "Jane Mentor", "Engineering",
                2, 3L, Collections.emptyList()
        );
        when(employeeDashboardService.getOverview("emp@skillsphere.com")).thenReturn(dto);

        mockMvc.perform(get("/api/dashboard/employee/overview"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("POST /api/auth/change-password returns 401 when unauthenticated")
    void testChangePassword_Unauthenticated_ReturnsUnauthorized() throws Exception {
        ChangePasswordRequest req = new ChangePasswordRequest("OldPass123!", "NewPass123!", "NewPass123!");

        mockMvc.perform(post("/api/auth/change-password")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("POST /api/auth/change-password returns 403 Forbidden without CSRF token")
    @WithMockUser(username = "emp@skillsphere.com", roles = "EMPLOYEE")
    void testChangePassword_WithoutCsrf_ReturnsForbidden() throws Exception {
        ChangePasswordRequest req = new ChangePasswordRequest("OldPass123!", "NewPass123!", "NewPass123!");

        mockMvc.perform(post("/api/auth/change-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("POST /api/auth/change-password returns 400 when validation fails (invalid password pattern)")
    @WithMockUser(username = "emp@skillsphere.com", roles = "EMPLOYEE")
    void testChangePassword_InvalidPattern_ReturnsBadRequest() throws Exception {
        // "short" does not meet 8 chars, letter, digit, special char requirement
        ChangePasswordRequest req = new ChangePasswordRequest("OldPass123!", "short", "short");

        mockMvc.perform(post("/api/auth/change-password")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /api/auth/change-password returns 200 on valid request")
    @WithMockUser(username = "emp@skillsphere.com", roles = "EMPLOYEE")
    void testChangePassword_Valid_ReturnsOk() throws Exception {
        ChangePasswordRequest req = new ChangePasswordRequest("CurrentValid1!", "NewStrongPass1@", "NewStrongPass1@");

        mockMvc.perform(post("/api/auth/change-password")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk());
    }
}
