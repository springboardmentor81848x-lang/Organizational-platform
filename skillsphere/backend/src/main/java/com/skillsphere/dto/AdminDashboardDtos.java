package com.skillsphere.dto;

import com.skillsphere.entity.AccountStatus;
import com.skillsphere.entity.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public class AdminDashboardDtos {

    // ==========================================
    // 1. Dashboard Overview
    // ==========================================
    public record AdminDashboardOverviewDto(
            long totalUsers,
            long activeUsers,
            long inactiveUsers,
            Map<String, Long> usersByRole,
            long totalDepartments,
            long totalTeams,
            long totalSkills,
            long totalCourses,
            long openSupportTicketsCount,
            long totalAuditLogsCount,
            AdminSystemHealthDto systemHealth,
            List<AdminUserDetailDto> recentUsers,
            List<AdminSecurityAlertDto> recentSecurityAlerts,
            List<AuditLogDto> recentAuditLogs
    ) {}

    public record AdminSystemHealthDto(
            String status,
            String databaseStatus,
            String databaseVersion,
            long activeTablesCount,
            long appliedMigrationsCount,
            String backupStatus,
            String lastBackupAt,
            boolean geminiConfigured,
            String jvmMemoryUsage
    ) {}

    // ==========================================
    // 2. User Management
    // ==========================================
    public record AdminUserDetailDto(
            Long id,
            String fullName,
            String email,
            String employeeId,
            Role role,
            Role requestedRole,
            AccountStatus accountStatus,
            String department,
            String jobRole,
            Instant createdAt,
            Instant updatedAt
    ) {}

    public record CreateUserAdminRequest(
            @NotBlank(message = "Full name is required")
            @Size(max = 120, message = "Full name cannot exceed 120 characters")
            String fullName,

            @NotBlank(message = "Email is required")
            @Email(message = "Valid email is required")
            String email,

            String employeeId,

            @NotNull(message = "Role is required")
            Role role,

            String department,
            String jobRole,

            @NotBlank(message = "Password is required")
            @Size(min = 8, message = "Password must be at least 8 characters")
            String password
    ) {
        public CreateUserAdminRequest(String fullName, String email, String employeeId, Role role, String department, String password) {
            this(fullName, email, employeeId, role, department, null, password);
        }
    }

    public record UpdateUserAdminRequest(
            @NotBlank(message = "Full name is required")
            @Size(max = 120)
            String fullName,

            String employeeId,
            String department,
            String jobRole,
            AccountStatus accountStatus
    ) {
        public UpdateUserAdminRequest(String fullName, String employeeId, String department, AccountStatus accountStatus) {
            this(fullName, employeeId, department, null, accountStatus);
        }
    }

    // ==========================================
    // 3. Roles & Permissions
    // ==========================================
    public record RolePermissionDto(
            Role role,
            String label,
            String description,
            int userCount,
            List<String> permissions,
            boolean isPrivileged
    ) {}

    public record AssignRoleRequest(
            @NotNull(message = "Target role is required")
            Role role
    ) {}

    public record RoleAssignmentHistoryDto(
            Long auditId,
            String targetUserEmail,
            String action,
            String details,
            String performedBy,
            Instant timestamp
    ) {}

    // ==========================================
    // 6. Communication Settings (Privacy Protected)
    // ==========================================
    public record CommunicationSettingsDto(
            int maxUploadSizeMb,
            String allowedAttachmentTypes,
            int chatRetentionDays,
            boolean messagingEnabled,
            long totalConversationsCount,
            long totalMessagesCount,
            String privacyNotice
    ) {}

    public record UpdateCommunicationSettingsRequest(
            int maxUploadSizeMb,
            @NotBlank String allowedAttachmentTypes,
            int chatRetentionDays,
            boolean messagingEnabled
    ) {}

    // ==========================================
    // 7. Notification Settings
    // ==========================================
    public record NotificationSettingsDto(
            boolean inAppNotificationsEnabled,
            String defaultPriority,
            long totalNotificationsDelivered,
            long unreadNotificationsCount,
            List<String> supportedCategories
    ) {}

    public record UpdateNotificationSettingsRequest(
            boolean inAppNotificationsEnabled,
            @NotBlank String defaultPriority
    ) {}

    // ==========================================
    // 9. Security Monitoring
    // ==========================================
    public record AdminSecurityOverviewDto(
            List<String> currentlyLockedAccounts,
            int recentFailedAttemptsCount,
            long totalAuditLogsLast24h,
            long privilegeChangesCount,
            List<AdminSecurityAlertDto> securityAlerts
    ) {}

    public record AdminSecurityAlertDto(
            String id,
            String severity, // CRITICAL, HIGH, MEDIUM, LOW, INFO
            String eventCategory,
            Instant timestamp,
            String affectedAccount,
            String supportingEvidence,
            String recommendedResponse,
            String status
    ) {}

    // ==========================================
    // 10. System Configuration
    // ==========================================
    public record SystemConfigOverviewDto(
            List<SystemConfigItemDto> configurations,
            GeminiIntegrationStatusDto geminiStatus,
            String environment,
            int serverPort,
            int sessionTimeoutMinutes
    ) {}

    public record SystemConfigItemDto(
            String key,
            String value,
            String description,
            Instant updatedAt
    ) {}

    public record GeminiIntegrationStatusDto(
            boolean configured,
            String model,
            int timeoutSeconds,
            String status,
            String keyFingerprint // masked preview (e.g. "AIzaSy...***")
    ) {}

    // ==========================================
    // 12. Support & Issues
    // ==========================================
    public record SupportTicketAdminDto(
            Long id,
            Long userId,
            String userName,
            String userEmail,
            String subject,
            String description,
            String priority,
            String status,
            String adminNotes,
            Instant createdAt,
            Instant updatedAt
    ) {}

    public record UpdateSupportTicketAdminRequest(
            String status,
            String adminNotes
    ) {}
}
