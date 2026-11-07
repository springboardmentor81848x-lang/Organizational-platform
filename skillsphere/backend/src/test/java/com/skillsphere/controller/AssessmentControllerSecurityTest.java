package com.skillsphere.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.skillsphere.config.SecurityConfig;
import com.skillsphere.dto.StartAssessmentRequest;
import com.skillsphere.security.JsonErrorWriter;
import com.skillsphere.service.AssessmentService;
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

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AssessmentController.class)
@Import({SecurityConfig.class, JsonErrorWriter.class})
@TestPropertySource(properties = {
        "app.cors.allowed-origins=http://localhost:5173"
})
class AssessmentControllerSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AssessmentService assessmentService;

    @MockBean
    private UserDetailsService userDetailsService;

    @Test
    @DisplayName("Unauthenticated user cannot start dynamic assessment (401 Unauthorized)")
    void testStartAssessment_Unauthenticated_ReturnsUnauthorized() throws Exception {
        StartAssessmentRequest req = new StartAssessmentRequest(1L, "Intermediate", null, null);

        mockMvc.perform(post("/api/assessments/start")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Unauthenticated user cannot view ongoing assessment (401 Unauthorized)")
    void testGetOngoingAssessment_Unauthenticated_ReturnsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/assessments/ongoing"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Authenticated EMPLOYEE can access ongoing assessment endpoint")
    @WithMockUser(username = "emp@example.com", roles = "EMPLOYEE")
    void testGetOngoingAssessment_AuthenticatedEmployee_ReturnsOk() throws Exception {
        mockMvc.perform(get("/api/assessments/ongoing"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Authenticated EMPLOYEE can call start assessment endpoint")
    @WithMockUser(username = "emp@example.com", roles = "EMPLOYEE")
    void testStartAssessment_AuthenticatedEmployee_ReturnsCreated() throws Exception {
        StartAssessmentRequest req = new StartAssessmentRequest(1L, "Intermediate", null, null);

        mockMvc.perform(post("/api/assessments/start")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("Unauthenticated user cannot view assessment attempt by ID (401 Unauthorized)")
    void testGetAttemptById_Unauthenticated_ReturnsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/assessments/attempts/1"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Authenticated user can invoke get assessment attempt by ID endpoint")
    @WithMockUser(username = "emp@example.com", roles = "EMPLOYEE")
    void testGetAttemptById_Authenticated_ReturnsOk() throws Exception {
        mockMvc.perform(get("/api/assessments/attempts/1"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("EMPLOYEE cannot assign assessments (403 Forbidden)")
    @WithMockUser(username = "emp@example.com", roles = "EMPLOYEE")
    void testAssignAssessment_Employee_ReturnsForbidden() throws Exception {
        com.skillsphere.dto.AssignAssessmentRequest req = new com.skillsphere.dto.AssignAssessmentRequest(1L, 2L, java.time.LocalDate.now().plusDays(7));

        mockMvc.perform(post("/api/assessments/assign")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("MANAGER can assign assessments")
    @WithMockUser(username = "manager@example.com", roles = "MANAGER")
    void testAssignAssessment_Manager_ReturnsCreated() throws Exception {
        com.skillsphere.dto.AssignAssessmentRequest req = new com.skillsphere.dto.AssignAssessmentRequest(1L, 2L, java.time.LocalDate.now().plusDays(7));

        mockMvc.perform(post("/api/assessments/assign")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("EMPLOYEE cannot access other users skill gaps directly (403 Forbidden)")
    @WithMockUser(username = "emp@example.com", roles = "EMPLOYEE")
    void testGetUserSkillGaps_Employee_ReturnsForbidden() throws Exception {
        mockMvc.perform(get("/api/assessments/gaps/user/2"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("MANAGER and MENTOR can access user skill gaps endpoint")
    @WithMockUser(username = "mgr@example.com", roles = "MANAGER")
    void testGetUserSkillGaps_Manager_ReturnsOk() throws Exception {
        mockMvc.perform(get("/api/assessments/gaps/user/2"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("EMPLOYEE cannot access team skill gaps (403 Forbidden)")
    @WithMockUser(username = "emp@example.com", roles = "EMPLOYEE")
    void testGetTeamSkillGaps_Employee_ReturnsForbidden() throws Exception {
        mockMvc.perform(get("/api/assessments/gaps/team/1"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("MANAGER can access team skill gaps")
    @WithMockUser(username = "mgr@example.com", roles = "MANAGER")
    void testGetTeamSkillGaps_Manager_ReturnsOk() throws Exception {
        mockMvc.perform(get("/api/assessments/gaps/team/1"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("EMPLOYEE and MANAGER cannot access organization skill gaps (403 Forbidden)")
    @WithMockUser(username = "mgr@example.com", roles = "MANAGER")
    void testGetOrgSkillGaps_Manager_ReturnsForbidden() throws Exception {
        mockMvc.perform(get("/api/assessments/gaps/organization"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("HR_SPECIALIST can access organization skill gaps")
    @WithMockUser(username = "hr@example.com", roles = "HR_SPECIALIST")
    void testGetOrgSkillGaps_HrSpecialist_ReturnsOk() throws Exception {
        mockMvc.perform(get("/api/assessments/gaps/organization"))
                .andExpect(status().isOk());
    }
}
