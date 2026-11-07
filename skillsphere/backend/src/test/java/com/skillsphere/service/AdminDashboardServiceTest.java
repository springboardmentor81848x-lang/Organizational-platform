package com.skillsphere.service;

import com.skillsphere.dto.AdminDashboardDtos.*;
import com.skillsphere.entity.*;
import com.skillsphere.exception.ApiException;
import com.skillsphere.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AdminDashboardServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private EmployeeProfileRepository employeeProfileRepository;
    @Mock private DepartmentRepository departmentRepository;
    @Mock private TeamRepository teamRepository;
    @Mock private SkillRepository skillRepository;
    @Mock private CourseRepository courseRepository;
    @Mock private AuditLogRepository auditLogRepository;
    @Mock private SupportTicketRepository supportTicketRepository;
    @Mock private SystemConfigurationRepository systemConfigurationRepository;
    @Mock private ConversationRepository conversationRepository;
    @Mock private MessageRepository messageRepository;
    @Mock private NotificationRepository notificationRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private LoginAttemptService loginAttemptService;
    @Mock private DatabaseBackupService databaseBackupService;
    @Mock private AdminGovernanceService adminGovernanceService;

    @InjectMocks
    private AdminDashboardService adminDashboardService;

    private User adminUser1;
    private User adminUser2;
    private User employeeUser;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(adminDashboardService, "serverPort", 8080);
        ReflectionTestUtils.setField(adminDashboardService, "sessionTimeoutStr", "30m");
        ReflectionTestUtils.setField(adminDashboardService, "geminiApiKey", "AIzaSyFakeKey123456789");
        ReflectionTestUtils.setField(adminDashboardService, "geminiModel", "gemini-3.5-flash");
        ReflectionTestUtils.setField(adminDashboardService, "geminiTimeoutSeconds", 45);

        adminUser1 = new User();
        ReflectionTestUtils.setField(adminUser1, "id", 1L);
        ReflectionTestUtils.setField(adminUser1, "createdAt", Instant.now().minusSeconds(3600));
        adminUser1.setFullName("Lead Administrator");
        adminUser1.setEmail("admin1@skillsphere.com");
        adminUser1.setRole(Role.ADMIN);
        adminUser1.setAccountStatus(AccountStatus.ACTIVE);

        adminUser2 = new User();
        ReflectionTestUtils.setField(adminUser2, "id", 2L);
        ReflectionTestUtils.setField(adminUser2, "createdAt", Instant.now().minusSeconds(1800));
        adminUser2.setFullName("Secondary Administrator");
        adminUser2.setEmail("admin2@skillsphere.com");
        adminUser2.setRole(Role.ADMIN);
        adminUser2.setAccountStatus(AccountStatus.ACTIVE);

        employeeUser = new User();
        ReflectionTestUtils.setField(employeeUser, "id", 100L);
        ReflectionTestUtils.setField(employeeUser, "createdAt", Instant.now());
        employeeUser.setFullName("Alice Developer");
        employeeUser.setEmail("alice@skillsphere.com");
        employeeUser.setEmployeeId("EMP-100");
        employeeUser.setRole(Role.EMPLOYEE);
        employeeUser.setAccountStatus(AccountStatus.ACTIVE);
    }

    // =========================================================================
    // 1. Dashboard Overview Tests
    // =========================================================================
    @Test
    @DisplayName("getDashboardOverview: Computes real-time system metrics")
    void testGetDashboardOverview() {
        when(userRepository.findAll()).thenReturn(List.of(adminUser1, adminUser2, employeeUser));
        when(departmentRepository.count()).thenReturn(3L);
        when(teamRepository.count()).thenReturn(5L);
        when(skillRepository.count()).thenReturn(25L);
        when(courseRepository.count()).thenReturn(15L);
        when(supportTicketRepository.findAll()).thenReturn(Collections.emptyList());
        when(auditLogRepository.count()).thenReturn(100L);
        when(employeeProfileRepository.findAllWithUser()).thenReturn(Collections.emptyList());
        when(databaseBackupService.listBackups()).thenReturn(Collections.emptyList());
        when(adminGovernanceService.getAllAuditLogs()).thenReturn(Collections.emptyList());
        when(loginAttemptService.getCurrentlyLockedEmails()).thenReturn(Collections.emptyList());

        AdminDashboardOverviewDto overview = adminDashboardService.getDashboardOverview();

        assertNotNull(overview);
        assertEquals(3L, overview.totalUsers());
        assertEquals(3L, overview.activeUsers());
        assertEquals(0L, overview.inactiveUsers());
        assertEquals(2L, overview.usersByRole().get("ADMIN"));
        assertEquals(1L, overview.usersByRole().get("EMPLOYEE"));
        assertEquals(3L, overview.totalDepartments());
        assertEquals("HEALTHY", overview.systemHealth().status());
        assertEquals("CONNECTED", overview.systemHealth().databaseStatus());
        assertTrue(overview.systemHealth().geminiConfigured());
    }

    // =========================================================================
    // 2. User Lifecycle Management Tests
    // =========================================================================
    @Test
    @DisplayName("createUser: Successfully creates new user and records audit log")
    void testCreateUser_Success() {
        when(userRepository.existsByEmail("bob@skillsphere.com")).thenReturn(false);
        when(userRepository.existsByEmployeeId("EMP-200")).thenReturn(false);
        when(passwordEncoder.encode("SecurePass123!")).thenReturn("hashed_pass");
        when(userRepository.findByEmail("admin1@skillsphere.com")).thenReturn(Optional.of(adminUser1));

        User savedUser = new User();
        ReflectionTestUtils.setField(savedUser, "id", 200L);
        savedUser.setFullName("Bob Smith");
        savedUser.setEmail("bob@skillsphere.com");
        savedUser.setEmployeeId("EMP-200");
        savedUser.setRole(Role.EMPLOYEE);
        savedUser.setAccountStatus(AccountStatus.ACTIVE);
        when(userRepository.saveAndFlush(any(User.class))).thenReturn(savedUser);

        CreateUserAdminRequest req = new CreateUserAdminRequest(
                "Bob Smith", "bob@skillsphere.com", "EMP-200", Role.EMPLOYEE, "Engineering", "SecurePass123!"
        );

        AdminUserDetailDto res = adminDashboardService.createUser(req, "admin1@skillsphere.com", "127.0.0.1");

        assertNotNull(res);
        assertEquals("Bob Smith", res.fullName());
        assertEquals("bob@skillsphere.com", res.email());
        assertEquals(Role.EMPLOYEE, res.role());
        verify(adminGovernanceService, times(1)).recordAuditLog(
                eq(adminUser1), eq("CREATE_USER"), eq("User"), eq("200"), anyString(), eq("127.0.0.1")
        );
    }

    @Test
    @DisplayName("createUser: 409 Conflict when duplicate email")
    void testCreateUser_DuplicateEmail_ThrowsConflict() {
        when(userRepository.existsByEmail("alice@skillsphere.com")).thenReturn(true);

        CreateUserAdminRequest req = new CreateUserAdminRequest(
                "Alice Again", "alice@skillsphere.com", "EMP-999", Role.EMPLOYEE, "Sales", "Pass12345!"
        );

        ApiException ex = assertThrows(ApiException.class, () ->
                adminDashboardService.createUser(req, "admin1@skillsphere.com", "127.0.0.1"));
        assertEquals(HttpStatus.CONFLICT, ex.getStatus());
        assertTrue(ex.getMessage().contains("already exists"));
    }

    @Test
    @DisplayName("updateUser: Last active admin protection prevents self-deactivation")
    void testUpdateUser_SelfDeactivation_ThrowsBadRequest() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(adminUser1));
        when(userRepository.findByEmail("admin1@skillsphere.com")).thenReturn(Optional.of(adminUser1));

        UpdateUserAdminRequest req = new UpdateUserAdminRequest(
                "Lead Administrator", null, null, AccountStatus.SUSPENDED
        );

        ApiException ex = assertThrows(ApiException.class, () ->
                adminDashboardService.updateUser(1L, req, "admin1@skillsphere.com", "127.0.0.1"));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
        assertTrue(ex.getMessage().contains("cannot deactivate your own administrator account"));
    }

    @Test
    @DisplayName("updateUser: Last active admin protection prevents deactivating sole admin")
    void testUpdateUser_LastAdminDeactivation_ThrowsBadRequest() {
        when(userRepository.findById(2L)).thenReturn(Optional.of(adminUser2));
        when(userRepository.findByEmail("admin1@skillsphere.com")).thenReturn(Optional.of(adminUser1));
        // Only 1 active admin remains
        when(userRepository.findAll()).thenReturn(List.of(adminUser2, employeeUser));

        UpdateUserAdminRequest req = new UpdateUserAdminRequest(
                "Secondary Administrator", null, null, AccountStatus.SUSPENDED
        );

        ApiException ex = assertThrows(ApiException.class, () ->
                adminDashboardService.updateUser(2L, req, "admin1@skillsphere.com", "127.0.0.1"));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
        assertTrue(ex.getMessage().contains("Cannot deactivate the last remaining active platform administrator"));
    }

    // =========================================================================
    // 3. Role & Permission Governance Tests
    // =========================================================================
    @Test
    @DisplayName("getRolesOverview: Returns all 6 roles with permissions and user counts")
    void testGetRolesOverview() {
        when(userRepository.findAll()).thenReturn(List.of(adminUser1, adminUser2, employeeUser));

        List<RolePermissionDto> roles = adminDashboardService.getRolesOverview();

        assertNotNull(roles);
        assertEquals(6, roles.size());
        assertTrue(roles.stream().anyMatch(r -> r.role() == Role.ADMIN && r.userCount() == 2));
        assertTrue(roles.stream().anyMatch(r -> r.role() == Role.EMPLOYEE && r.userCount() == 1));
        assertTrue(roles.stream().anyMatch(r -> r.role() == Role.DEPARTMENT_HEAD));
    }

    @Test
    @DisplayName("assignUserRole: Prevents demoting the last active administrator")
    void testAssignUserRole_DemoteLastAdmin_ThrowsBadRequest() {
        when(userRepository.findById(2L)).thenReturn(Optional.of(adminUser2));
        when(userRepository.findByEmail("admin1@skillsphere.com")).thenReturn(Optional.of(adminUser1));
        when(userRepository.findAll()).thenReturn(List.of(adminUser2)); // only 1 active admin

        ApiException ex = assertThrows(ApiException.class, () ->
                adminDashboardService.assignUserRole(2L, Role.EMPLOYEE, "admin1@skillsphere.com", "127.0.0.1"));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
        assertTrue(ex.getMessage().contains("Cannot demote the last remaining active platform administrator"));
    }

    @Test
    @DisplayName("assignUserRole: Successfully promotes employee and records audit log")
    void testAssignUserRole_Success() {
        when(userRepository.findById(100L)).thenReturn(Optional.of(employeeUser));
        when(userRepository.findByEmail("admin1@skillsphere.com")).thenReturn(Optional.of(adminUser1));
        when(userRepository.save(any(User.class))).thenReturn(employeeUser);

        AdminUserDetailDto res = adminDashboardService.assignUserRole(100L, Role.MANAGER, "admin1@skillsphere.com", "127.0.0.1");

        assertNotNull(res);
        assertEquals(Role.MANAGER, employeeUser.getRole());
        verify(adminGovernanceService, times(1)).recordAuditLog(
                eq(adminUser1), eq("CHANGE_ROLE"), eq("User"), eq("100"), anyString(), eq("127.0.0.1")
        );
    }

    // =========================================================================
    // 6. Communication Settings & Privacy Verification
    // =========================================================================
    @Test
    @DisplayName("getCommunicationSettings: Enforces least privilege (zero private chat exposure)")
    void testGetCommunicationSettings_PrivacyEnforced() {
        when(systemConfigurationRepository.findAll()).thenReturn(List.of(
                new SystemConfiguration("max_upload_size_mb", "25", "Upload size"),
                new SystemConfiguration("allowed_attachment_types", "pdf,png,zip", "Allowed types")
        ));
        when(conversationRepository.count()).thenReturn(40L);
        when(messageRepository.count()).thenReturn(350L);

        CommunicationSettingsDto settings = adminDashboardService.getCommunicationSettings();

        assertNotNull(settings);
        assertEquals(25, settings.maxUploadSizeMb());
        assertEquals("pdf,png,zip", settings.allowedAttachmentTypes());
        assertEquals(40L, settings.totalConversationsCount());
        assertEquals(350L, settings.totalMessagesCount());
        assertTrue(settings.privacyNotice().contains("strictly prohibited from inspecting private employee chat messages"));

        // Strictly verify only aggregate count was invoked and zero message content repositories were accessed
        verify(messageRepository, times(1)).count();
        verifyNoMoreInteractions(messageRepository);
    }

    // =========================================================================
    // 7. Notification Settings
    // =========================================================================
    @Test
    @DisplayName("getNotificationSettings: Returns delivery statistics and settings")
    void testGetNotificationSettings() {
        when(systemConfigurationRepository.findAll()).thenReturn(List.of(
                new SystemConfiguration("notification_in_app_enabled", "true", "In-app flag")
        ));
        when(notificationRepository.count()).thenReturn(150L);

        NotificationSettingsDto dto = adminDashboardService.getNotificationSettings();

        assertNotNull(dto);
        assertTrue(dto.inAppNotificationsEnabled());
        assertEquals(150L, dto.totalNotificationsDelivered());
        assertFalse(dto.supportedCategories().isEmpty());
    }

    // =========================================================================
    // 9. Security Monitoring
    // =========================================================================
    @Test
    @DisplayName("getSecurityOverview: Returns locked accounts and generates alerts")
    void testGetSecurityOverview() {
        when(loginAttemptService.getCurrentlyLockedEmails()).thenReturn(List.of("attacker@skillsphere.com"));
        when(loginAttemptService.getRecentFailedAttemptsCount()).thenReturn(5);
        when(auditLogRepository.findAllWithUser()).thenReturn(Collections.emptyList());
        when(userRepository.findAll()).thenReturn(List.of(adminUser1, adminUser2));

        AdminSecurityOverviewDto overview = adminDashboardService.getSecurityOverview();

        assertNotNull(overview);
        assertEquals(1, overview.currentlyLockedAccounts().size());
        assertEquals("attacker@skillsphere.com", overview.currentlyLockedAccounts().get(0));
        assertEquals(5, overview.recentFailedAttemptsCount());
        assertFalse(overview.securityAlerts().isEmpty());
        assertTrue(overview.securityAlerts().stream().anyMatch(a -> a.eventCategory().equals("BRUTE_FORCE_LOCKOUT")));
    }

    // =========================================================================
    // 10. System Configuration & Masking
    // =========================================================================
    @Test
    @DisplayName("getSystemConfigurationOverview: Masks Gemini secret API key")
    void testGetSystemConfigurationOverview_MasksSecret() {
        when(systemConfigurationRepository.findAll()).thenReturn(List.of(
                new SystemConfiguration("chat_retention_days", "365", "Retention period")
        ));

        SystemConfigOverviewDto overview = adminDashboardService.getSystemConfigurationOverview();

        assertNotNull(overview);
        assertNotNull(overview.geminiStatus());
        assertTrue(overview.geminiStatus().configured());
        assertEquals("OPERATIONAL", overview.geminiStatus().status());
        // Verify key is masked and does not expose full plain-text secret
        assertFalse(overview.geminiStatus().keyFingerprint().equals("AIzaSyFakeKey123456789"));
        assertTrue(overview.geminiStatus().keyFingerprint().contains("..."));
    }

    // =========================================================================
    // 12. Support & Issues
    // =========================================================================
    @Test
    @DisplayName("updateSupportTicket: Updates status, admin notes, and records audit log")
    void testUpdateSupportTicket() {
        SupportTicket ticket = new SupportTicket(employeeUser, "Login failure", "Cannot sign in", "HIGH");
        ReflectionTestUtils.setField(ticket, "id", 50L);
        when(supportTicketRepository.findById(50L)).thenReturn(Optional.of(ticket));
        when(supportTicketRepository.save(any(SupportTicket.class))).thenReturn(ticket);
        when(userRepository.findByEmail("admin1@skillsphere.com")).thenReturn(Optional.of(adminUser1));

        UpdateSupportTicketAdminRequest req = new UpdateSupportTicketAdminRequest(
                "RESOLVED", "Reset password token generated and issued to employee."
        );

        SupportTicketAdminDto res = adminDashboardService.updateSupportTicket(50L, req, "admin1@skillsphere.com", "127.0.0.1");

        assertNotNull(res);
        assertEquals("RESOLVED", res.status());
        assertEquals("Reset password token generated and issued to employee.", res.adminNotes());
        verify(adminGovernanceService, times(1)).recordAuditLog(
                eq(adminUser1), eq("UPDATE_SUPPORT_TICKET"), eq("SupportTicket"), eq("50"), anyString(), eq("127.0.0.1")
        );
    }
}
