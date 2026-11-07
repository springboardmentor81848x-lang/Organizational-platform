package com.skillsphere.controller;

import com.skillsphere.config.SecurityConfig;
import com.skillsphere.dto.BackupDtos.*;
import com.skillsphere.security.JsonErrorWriter;
import com.skillsphere.service.DatabaseBackupService;
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
import java.util.Collections;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = DatabaseBackupController.class)
@Import({SecurityConfig.class, JsonErrorWriter.class})
@TestPropertySource(properties = {
        "app.cors.allowed-origins=http://localhost:5173"
})
class DatabaseBackupControllerSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private DatabaseBackupService backupService;

    @MockBean
    private UserDetailsService userDetailsService;

    @Test
    @DisplayName("Unauthenticated request to list backups returns 401 Unauthorized")
    void testUnauthenticated_ReturnsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/admin/backups"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "emp@skillsphere.com", roles = {"EMPLOYEE"})
    @DisplayName("EMPLOYEE access to list backups returns 403 Forbidden")
    void testEmployeeRole_ReturnsForbidden() throws Exception {
        mockMvc.perform(get("/api/admin/backups"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "mgr@skillsphere.com", roles = {"MANAGER"})
    @DisplayName("MANAGER access to create backup returns 403 Forbidden")
    void testManagerRole_ReturnsForbidden() throws Exception {
        mockMvc.perform(post("/api/admin/backups").with(csrf()))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "hr@skillsphere.com", roles = {"HR_SPECIALIST"})
    @DisplayName("HR_SPECIALIST access to test restore returns 403 Forbidden")
    void testHrRole_ReturnsForbidden() throws Exception {
        mockMvc.perform(post("/api/admin/backups/skillsphere_backup_20261106_120000.dump/test-restore").with(csrf()))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "admin@skillsphere.com", roles = {"ADMIN"})
    @DisplayName("ADMIN access to list backups returns 200 OK")
    void testAdminRole_ListBackups_ReturnsOk() throws Exception {
        when(backupService.listBackups()).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/api/admin/backups"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "admin@skillsphere.com", roles = {"ADMIN"})
    @DisplayName("ADMIN access to create backup returns 201 Created")
    void testAdminRole_CreateBackup_ReturnsCreated() throws Exception {
        BackupMetadataDto dto = new BackupMetadataDto(
                "skillsphere_backup_20261106_120000.dump",
                1024,
                "1.0 KB",
                Instant.now(),
                "sha256hash",
                "SUCCESS",
                "skillsphere_db",
                "PostgreSQL 16",
                9,
                "Manual backup",
                false,
                true
        );

        when(backupService.createBackup(any(), any(), any())).thenReturn(dto);

        mockMvc.perform(post("/api/admin/backups")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"note\":\"Manual backup\"}"))
                .andExpect(status().isCreated());
    }
}
