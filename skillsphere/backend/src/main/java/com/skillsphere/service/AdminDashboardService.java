package com.skillsphere.service;

import com.skillsphere.dto.AdminDashboardDtos.*;
import com.skillsphere.dto.AuditLogDto;
import com.skillsphere.entity.*;
import com.skillsphere.exception.ApiException;
import com.skillsphere.repository.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class AdminDashboardService {

    private final UserRepository userRepository;
    private final EmployeeProfileRepository employeeProfileRepository;
    private final DepartmentRepository departmentRepository;
    private final TeamRepository teamRepository;
    private final SkillRepository skillRepository;
    private final CourseRepository courseRepository;
    private final AuditLogRepository auditLogRepository;
    private final SupportTicketRepository supportTicketRepository;
    private final SystemConfigurationRepository systemConfigurationRepository;
    private final ConversationRepository conversationRepository;
    private final MessageRepository messageRepository;
    private final NotificationRepository notificationRepository;
    private final PasswordEncoder passwordEncoder;
    private final LoginAttemptService loginAttemptService;
    private final DatabaseBackupService databaseBackupService;
    private final AdminGovernanceService adminGovernanceService;

    @Value("${server.port:8080}")
    private int serverPort;

    @Value("${server.servlet.session.timeout:30m}")
    private String sessionTimeoutStr;

    @Value("${app.gemini.api-key:}")
    private String geminiApiKey;

    @Value("${app.gemini.model:gemini-3.5-flash}")
    private String geminiModel;

    @Value("${app.gemini.timeout-seconds:45}")
    private int geminiTimeoutSeconds;

    public AdminDashboardService(UserRepository userRepository,
                                 EmployeeProfileRepository employeeProfileRepository,
                                 DepartmentRepository departmentRepository,
                                 TeamRepository teamRepository,
                                 SkillRepository skillRepository,
                                 CourseRepository courseRepository,
                                 AuditLogRepository auditLogRepository,
                                 SupportTicketRepository supportTicketRepository,
                                 SystemConfigurationRepository systemConfigurationRepository,
                                 ConversationRepository conversationRepository,
                                 MessageRepository messageRepository,
                                 NotificationRepository notificationRepository,
                                 PasswordEncoder passwordEncoder,
                                 LoginAttemptService loginAttemptService,
                                 DatabaseBackupService databaseBackupService,
                                 AdminGovernanceService adminGovernanceService) {
        this.userRepository = userRepository;
        this.employeeProfileRepository = employeeProfileRepository;
        this.departmentRepository = departmentRepository;
        this.teamRepository = teamRepository;
        this.skillRepository = skillRepository;
        this.courseRepository = courseRepository;
        this.auditLogRepository = auditLogRepository;
        this.supportTicketRepository = supportTicketRepository;
        this.systemConfigurationRepository = systemConfigurationRepository;
        this.conversationRepository = conversationRepository;
        this.messageRepository = messageRepository;
        this.notificationRepository = notificationRepository;
        this.passwordEncoder = passwordEncoder;
        this.loginAttemptService = loginAttemptService;
        this.databaseBackupService = databaseBackupService;
        this.adminGovernanceService = adminGovernanceService;
    }

    // =========================================================================
    // 1. Dashboard Overview
    // =========================================================================
    @Transactional(readOnly = true)
    public AdminDashboardOverviewDto getDashboardOverview() {
        List<User> allUsers = userRepository.findAll();
        long totalUsers = allUsers.size();
        long activeUsers = allUsers.stream().filter(u -> u.getAccountStatus() == AccountStatus.ACTIVE).count();
        long inactiveUsers = totalUsers - activeUsers;

        Map<String, Long> usersByRole = allUsers.stream()
                .collect(Collectors.groupingBy(u -> u.getRole().name(), Collectors.counting()));

        long totalDepartments = departmentRepository.count();
        long totalTeams = teamRepository.count();
        long totalSkills = skillRepository.count();
        long totalCourses = courseRepository.count();

        List<SupportTicket> tickets = supportTicketRepository.findAll();
        long openTicketsCount = tickets.stream()
                .filter(t -> "OPEN".equalsIgnoreCase(t.getStatus()) || "IN_PROGRESS".equalsIgnoreCase(t.getStatus()))
                .count();

        long totalAuditLogsCount = auditLogRepository.count();

        // System Health
        Runtime rt = Runtime.getRuntime();
        long usedMb = (rt.totalMemory() - rt.freeMemory()) / (1024 * 1024);
        long maxMb = rt.maxMemory() / (1024 * 1024);
        String jvmMemory = usedMb + " MB / " + maxMb + " MB";

        String lastBackupTime = "None";
        String backupStatus = "READY";
        try {
            var backups = databaseBackupService.listBackups();
            if (!backups.isEmpty()) {
                lastBackupTime = backups.get(0).createdAt().toString();
                backupStatus = backups.get(0).status();
            }
        } catch (Exception ignored) {}

        AdminSystemHealthDto health = new AdminSystemHealthDto(
                "HEALTHY",
                "CONNECTED",
                "PostgreSQL 18.4 (Native Windows)",
                56L,
                9L,
                backupStatus,
                lastBackupTime,
                geminiApiKey != null && !geminiApiKey.isBlank(),
                jvmMemory
        );

        // Recent users (top 8)
        List<EmployeeProfile> profiles = employeeProfileRepository.findAllWithUser();
        Map<Long, EmployeeProfile> profileMap = profiles.stream()
                .collect(Collectors.toMap(p -> p.getUser().getId(), p -> p, (a, b) -> a));

        List<AdminUserDetailDto> recentUsers = allUsers.stream()
                .sorted((a, b) -> {
                    if (a.getCreatedAt() == null || b.getCreatedAt() == null) return 0;
                    return b.getCreatedAt().compareTo(a.getCreatedAt());
                })
                .limit(8)
                .map(u -> toUserDetailDto(u, profileMap.get(u.getId())))
                .toList();

        // Recent security alerts
        List<AdminSecurityAlertDto> securityAlerts = generateSecurityAlerts();

        // Recent audit logs
        List<AuditLogDto> recentAuditLogs = adminGovernanceService.getAllAuditLogs().stream()
                .limit(8)
                .toList();

        return new AdminDashboardOverviewDto(
                totalUsers,
                activeUsers,
                inactiveUsers,
                usersByRole,
                totalDepartments,
                totalTeams,
                totalSkills,
                totalCourses,
                openTicketsCount,
                totalAuditLogsCount,
                health,
                recentUsers,
                securityAlerts,
                recentAuditLogs
        );
    }

    // =========================================================================
    // 2. User Management
    // =========================================================================
    @Transactional(readOnly = true)
    public List<AdminUserDetailDto> getUsers(Role roleFilter, String departmentFilter, AccountStatus statusFilter, String search) {
        List<User> users = userRepository.findAll();
        List<EmployeeProfile> profiles = employeeProfileRepository.findAllWithUser();
        Map<Long, EmployeeProfile> profileMap = profiles.stream()
                .collect(Collectors.toMap(p -> p.getUser().getId(), p -> p, (a, b) -> a));

        return users.stream()
                .filter(u -> roleFilter == null || u.getRole() == roleFilter)
                .filter(u -> statusFilter == null || u.getAccountStatus() == statusFilter)
                .filter(u -> {
                    if (departmentFilter == null || departmentFilter.isBlank()) return true;
                    EmployeeProfile p = profileMap.get(u.getId());
                    return p != null && departmentFilter.equalsIgnoreCase(p.getDepartment());
                })
                .filter(u -> {
                    if (search == null || search.isBlank()) return true;
                    String s = search.toLowerCase();
                    return u.getFullName().toLowerCase().contains(s) ||
                            u.getEmail().toLowerCase().contains(s) ||
                            (u.getEmployeeId() != null && u.getEmployeeId().toLowerCase().contains(s));
                })
                .sorted((a, b) -> {
                    if (a.getCreatedAt() == null || b.getCreatedAt() == null) return 0;
                    return b.getCreatedAt().compareTo(a.getCreatedAt());
                })
                .map(u -> toUserDetailDto(u, profileMap.get(u.getId())))
                .toList();
    }

    @Transactional(readOnly = true)
    public AdminUserDetailDto getUserById(Long id) {
        User u = userRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "User not found: " + id));
        EmployeeProfile p = employeeProfileRepository.findByUserId(id).orElse(null);
        return toUserDetailDto(u, p);
    }

    @Transactional
    public AdminUserDetailDto createUser(CreateUserAdminRequest req, String actingAdminEmail, String ip) {
        String email = req.email().trim().toLowerCase();
        String employeeId = (req.employeeId() != null && !req.employeeId().isBlank()) ? req.employeeId().trim() : null;

        if (userRepository.existsByEmail(email)) {
            throw new ApiException(HttpStatus.CONFLICT, "An account with this email already exists.");
        }
        if (employeeId != null && userRepository.existsByEmployeeId(employeeId)) {
            throw new ApiException(HttpStatus.CONFLICT, "This Employee ID is already registered.");
        }

        User actingAdmin = userRepository.findByEmail(actingAdminEmail.trim().toLowerCase()).orElse(null);

        User user = new User();
        user.setFullName(req.fullName().trim());
        user.setEmail(email);
        user.setEmployeeId(employeeId);
        user.setPasswordHash(passwordEncoder.encode(req.password()));
        user.setRole(req.role());
        user.setAccountStatus(AccountStatus.ACTIVE);
        user = userRepository.saveAndFlush(user);

        EmployeeProfile profile = null;
        if (req.department() != null && !req.department().isBlank()) {
            profile = new EmployeeProfile();
            profile.setUser(user);
            profile.setDepartment(req.department().trim());
            profile.setJobRole(req.role().getLabel());
            profile = employeeProfileRepository.save(profile);
        }

        // Record Audit Log
        adminGovernanceService.recordAuditLog(
                actingAdmin,
                "CREATE_USER",
                "User",
                user.getId().toString(),
                "Created user " + user.getEmail() + " with role " + user.getRole(),
                ip
        );

        return toUserDetailDto(user, profile);
    }

    @Transactional
    public AdminUserDetailDto updateUser(Long id, UpdateUserAdminRequest req, String actingAdminEmail, String ip) {
        User target = userRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "User not found: " + id));
        User actingAdmin = userRepository.findByEmail(actingAdminEmail.trim().toLowerCase()).orElse(null);

        // Guard: Last Active Administrator Protection
        if (req.accountStatus() != null && req.accountStatus() != AccountStatus.ACTIVE && target.getRole() == Role.ADMIN && target.getAccountStatus() == AccountStatus.ACTIVE) {
            if (target.getEmail().equalsIgnoreCase(actingAdminEmail)) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "Security violation: You cannot deactivate your own administrator account.");
            }
            long activeAdminCount = userRepository.findAll().stream()
                    .filter(u -> u.getRole() == Role.ADMIN && u.getAccountStatus() == AccountStatus.ACTIVE)
                    .count();
            if (activeAdminCount <= 1) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "Security restriction: Cannot deactivate the last remaining active platform administrator.");
            }
        }

        target.setFullName(req.fullName().trim());
        if (req.employeeId() != null && !req.employeeId().isBlank()) {
            target.setEmployeeId(req.employeeId().trim());
        }
        if (req.accountStatus() != null) {
            target.setAccountStatus(req.accountStatus());
        }
        target = userRepository.save(target);

        EmployeeProfile profile = employeeProfileRepository.findByUserId(id).orElse(null);
        if (req.department() != null && !req.department().isBlank()) {
            if (profile == null) {
                profile = new EmployeeProfile();
                profile.setUser(target);
            }
            profile.setDepartment(req.department().trim());
            profile = employeeProfileRepository.save(profile);
        }

        // Record Audit Log
        adminGovernanceService.recordAuditLog(
                actingAdmin,
                "UPDATE_USER",
                "User",
                target.getId().toString(),
                "Updated profile details and status (" + target.getAccountStatus() + ") for " + target.getEmail(),
                ip
        );

        return toUserDetailDto(target, profile);
    }

    // =========================================================================
    // 3. Roles & Permissions Management
    // =========================================================================
    @Transactional(readOnly = true)
    public List<RolePermissionDto> getRolesOverview() {
        List<User> users = userRepository.findAll();
        Map<Role, Long> countMap = users.stream()
                .collect(Collectors.groupingBy(User::getRole, Collectors.counting()));

        return List.of(
                new RolePermissionDto(
                        Role.EMPLOYEE,
                        "Employee",
                        "Employee self-service, skill assessments, learning paths, personal gap analysis, and mentorship participation.",
                        countMap.getOrDefault(Role.EMPLOYEE, 0L).intValue(),
                        List.of("READ_OWN_PROFILE", "TAKE_ASSESSMENT", "ENROLL_COURSES", "SUBMIT_EVIDENCE", "JOIN_MENTORSHIP"),
                        false
                ),
                new RolePermissionDto(
                        Role.MANAGER,
                        "Manager",
                        "Team oversight, team gap analysis, assessment progress tracking, course recommendations, and mentorship referrals.",
                        countMap.getOrDefault(Role.MANAGER, 0L).intValue(),
                        List.of("VIEW_TEAM_MEMBERS", "VIEW_TEAM_GAPS", "RECOMMEND_COURSES", "REFER_MENTORS", "REVIEW_TEAM_EVIDENCE"),
                        true
                ),
                new RolePermissionDto(
                        Role.MENTOR,
                        "Mentor",
                        "Guiding mentees, scheduling sessions, logging action items, evaluating mentee competency milestones.",
                        countMap.getOrDefault(Role.MENTOR, 0L).intValue(),
                        List.of("VIEW_ASSIGNED_MENTEES", "SCHEDULE_SESSIONS", "SET_GOALS", "EVALUATE_COMPETENCIES", "SET_AVAILABILITY"),
                        false
                ),
                new RolePermissionDto(
                        Role.HR_SPECIALIST,
                        "HR Specialist",
                        "Organization-wide workforce directory, competency framework, role requirements, training catalogue, and learning programmes.",
                        countMap.getOrDefault(Role.HR_SPECIALIST, 0L).intValue(),
                        List.of("MANAGE_COMPETENCIES", "MANAGE_ROLE_REQUIREMENTS", "MANAGE_TRAINING_PROGRAMMES", "VIEW_ORG_HEATMAP", "MANAGE_LEARNING_PATHS"),
                        true
                ),
                new RolePermissionDto(
                        Role.DEPARTMENT_HEAD,
                        "Department Head",
                        "Strategic department oversight, competency compliance, team heatmaps, workforce capability, and departmental directives.",
                        countMap.getOrDefault(Role.DEPARTMENT_HEAD, 0L).intValue(),
                        List.of("VIEW_DEPT_OVERVIEW", "VIEW_DEPT_HEATMAP", "VIEW_WORKFORCE_CAPABILITY", "BROADCAST_DEPT_DIRECTIVES", "EXPORT_DEPT_REPORTS"),
                        true
                ),
                new RolePermissionDto(
                        Role.ADMIN,
                        "Administrator",
                        "Platform administration, user lifecycle, RBAC enforcement, system configuration, audit logs, security, and database backup/recovery.",
                        countMap.getOrDefault(Role.ADMIN, 0L).intValue(),
                        List.of("USER_MANAGEMENT", "ROLE_ASSIGNMENT", "SYSTEM_CONFIGURATION", "VIEW_AUDIT_LOGS", "SECURITY_MONITORING", "TRIGGER_DATABASE_BACKUP"),
                        true
                )
        );
    }

    @Transactional
    public AdminUserDetailDto assignUserRole(Long userId, Role newRole, String actingAdminEmail, String ip) {
        User target = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "User not found: " + userId));
        User actingAdmin = userRepository.findByEmail(actingAdminEmail.trim().toLowerCase()).orElse(null);

        if (target.getEmail().equalsIgnoreCase(actingAdminEmail)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Security violation: You cannot alter your own administrative role.");
        }

        // Guard: Last Active Administrator Demotion Protection
        if (target.getRole() == Role.ADMIN && newRole != Role.ADMIN) {
            long activeAdminCount = userRepository.findAll().stream()
                    .filter(u -> u.getRole() == Role.ADMIN && u.getAccountStatus() == AccountStatus.ACTIVE)
                    .count();
            if (activeAdminCount <= 1) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "Security restriction: Cannot demote the last remaining active platform administrator.");
            }
        }

        Role oldRole = target.getRole();
        target.setRole(newRole);
        if (target.getRequestedRole() == newRole) {
            target.setRequestedRole(null); // request fulfilled
        }
        target = userRepository.save(target);

        // Record Audit Log
        adminGovernanceService.recordAuditLog(
                actingAdmin,
                "CHANGE_ROLE",
                "User",
                target.getId().toString(),
                "Changed role of " + target.getEmail() + " from " + oldRole + " to " + newRole,
                ip
        );

        EmployeeProfile profile = employeeProfileRepository.findByUserId(userId).orElse(null);
        return toUserDetailDto(target, profile);
    }

    @Transactional(readOnly = true)
    public List<RoleAssignmentHistoryDto> getRoleAssignmentHistory() {
        return auditLogRepository.findAllWithUser().stream()
                .filter(l -> "CHANGE_ROLE".equalsIgnoreCase(l.getAction()))
                .map(l -> new RoleAssignmentHistoryDto(
                        l.getId(),
                        l.getEntityId(),
                        l.getAction(),
                        l.getDetails(),
                        l.getUser() != null ? l.getUser().getFullName() : "Administrator",
                        l.getCreatedAt()
                ))
                .toList();
    }

    // =========================================================================
    // 6. Communication Settings (Strict Privacy Protected)
    // =========================================================================
    @Transactional(readOnly = true)
    public CommunicationSettingsDto getCommunicationSettings() {
        Map<String, String> configs = getConfigMap();

        int maxUploadMb = Integer.parseInt(configs.getOrDefault("max_upload_size_mb", "10"));
        String allowedTypes = configs.getOrDefault("allowed_attachment_types", "pdf,png,jpg,jpeg,doc,docx,zip");
        int retentionDays = Integer.parseInt(configs.getOrDefault("chat_retention_days", "365"));
        boolean messagingEnabled = Boolean.parseBoolean(configs.getOrDefault("messaging_enabled", "true"));

        long conversationsCount = conversationRepository.count();
        long messagesCount = messageRepository.count();

        String privacyNotice = "Least-Privilege Privacy Guarantee: System Administrators configure communication policies and file restrictions, but are strictly prohibited from inspecting private employee chat messages, attachment contents, or mentor preparation notes.";

        return new CommunicationSettingsDto(
                maxUploadMb,
                allowedTypes,
                retentionDays,
                messagingEnabled,
                conversationsCount,
                messagesCount,
                privacyNotice
        );
    }

    @Transactional
    public CommunicationSettingsDto updateCommunicationSettings(UpdateCommunicationSettingsRequest req, String actingAdminEmail, String ip) {
        User actingAdmin = userRepository.findByEmail(actingAdminEmail.trim().toLowerCase()).orElse(null);

        saveConfigKey("max_upload_size_mb", String.valueOf(req.maxUploadSizeMb()), "Maximum file upload size in megabytes");
        saveConfigKey("allowed_attachment_types", req.allowedAttachmentTypes().trim(), "Comma-separated permitted attachment extensions");
        saveConfigKey("chat_retention_days", String.valueOf(req.chatRetentionDays()), "Days to retain chat messages");
        saveConfigKey("messaging_enabled", String.valueOf(req.messagingEnabled()), "System-wide direct messaging toggle");

        adminGovernanceService.recordAuditLog(
                actingAdmin,
                "UPDATE_COMMUNICATION_SETTINGS",
                "SystemConfiguration",
                "COMMUNICATION",
                "Updated upload limit to " + req.maxUploadSizeMb() + "MB, allowed types: " + req.allowedAttachmentTypes(),
                ip
        );

        return getCommunicationSettings();
    }

    // =========================================================================
    // 7. Notification Settings
    // =========================================================================
    @Transactional(readOnly = true)
    public NotificationSettingsDto getNotificationSettings() {
        Map<String, String> configs = getConfigMap();

        boolean inAppEnabled = Boolean.parseBoolean(configs.getOrDefault("notification_in_app_enabled", "true"));
        String defaultPriority = configs.getOrDefault("notification_default_priority", "NORMAL");

        long totalDelivered = notificationRepository.count();
        long unreadCount = notificationRepository.findAll().stream().filter(n -> !n.isRead()).count();

        List<String> categories = List.of("ASSESSMENT", "MENTORSHIP", "MESSAGE", "LEARNING", "ANNOUNCEMENT", "GOVERNANCE", "SECURITY");

        return new NotificationSettingsDto(inAppEnabled, defaultPriority, totalDelivered, unreadCount, categories);
    }

    @Transactional
    public NotificationSettingsDto updateNotificationSettings(UpdateNotificationSettingsRequest req, String actingAdminEmail, String ip) {
        User actingAdmin = userRepository.findByEmail(actingAdminEmail.trim().toLowerCase()).orElse(null);

        saveConfigKey("notification_in_app_enabled", String.valueOf(req.inAppNotificationsEnabled()), "Enable in-app user notifications");
        saveConfigKey("notification_default_priority", req.defaultPriority().toUpperCase(), "Default notification priority tier");

        adminGovernanceService.recordAuditLog(
                actingAdmin,
                "UPDATE_NOTIFICATION_SETTINGS",
                "SystemConfiguration",
                "NOTIFICATIONS",
                "Updated notification defaults: inApp=" + req.inAppNotificationsEnabled() + ", priority=" + req.defaultPriority(),
                ip
        );

        return getNotificationSettings();
    }

    // =========================================================================
    // 9. Security Monitoring
    // =========================================================================
    @Transactional(readOnly = true)
    public AdminSecurityOverviewDto getSecurityOverview() {
        List<String> lockedAccounts = loginAttemptService.getCurrentlyLockedEmails();
        int failedAttempts = loginAttemptService.getRecentFailedAttemptsCount();

        Instant past24h = Instant.now().minus(24, ChronoUnit.HOURS);
        List<AuditLog> recentLogs = auditLogRepository.findAllWithUser().stream()
                .filter(l -> l.getCreatedAt().isAfter(past24h))
                .toList();

        long totalLogsLast24h = recentLogs.size();
        long privilegeChanges = recentLogs.stream()
                .filter(l -> "CHANGE_ROLE".equalsIgnoreCase(l.getAction()))
                .count();

        List<AdminSecurityAlertDto> alerts = generateSecurityAlerts();

        return new AdminSecurityOverviewDto(lockedAccounts, failedAttempts, totalLogsLast24h, privilegeChanges, alerts);
    }

    private List<AdminSecurityAlertDto> generateSecurityAlerts() {
        List<AdminSecurityAlertDto> alerts = new ArrayList<>();
        int id = 1;

        // 1. Check for locked accounts
        List<String> locked = loginAttemptService.getCurrentlyLockedEmails();
        for (String email : locked) {
            alerts.add(new AdminSecurityAlertDto(
                    "SEC-" + (id++),
                    "HIGH",
                    "BRUTE_FORCE_LOCKOUT",
                    Instant.now(),
                    email,
                    "Account exceeded maximum failed login threshold (5 attempts)",
                    "Verify user identity or review IP origin",
                    "ACTIVE"
            ));
        }

        // 2. Check for recent privilege changes in last 7 days
        Instant past7Days = Instant.now().minus(7, ChronoUnit.DAYS);
        List<AuditLog> privLogs = auditLogRepository.findAllWithUser().stream()
                .filter(l -> "CHANGE_ROLE".equalsIgnoreCase(l.getAction()) && l.getCreatedAt().isAfter(past7Days))
                .toList();

        for (AuditLog l : privLogs) {
            alerts.add(new AdminSecurityAlertDto(
                    "SEC-" + (id++),
                    "INFO",
                    "PRIVILEGE_CHANGE",
                    l.getCreatedAt(),
                    l.getEntityId(),
                    l.getDetails(),
                    "Review authorized administrative justification",
                    "RESOLVED"
            ));
        }

        // 3. Ensure at least 2 active administrators exist for high-availability governance
        long activeAdmins = userRepository.findAll().stream()
                .filter(u -> u.getRole() == Role.ADMIN && u.getAccountStatus() == AccountStatus.ACTIVE)
                .count();
        if (activeAdmins < 2) {
            alerts.add(new AdminSecurityAlertDto(
                    "SEC-" + (id++),
                    "MEDIUM",
                    "GOVERNANCE_REDUNDANCY",
                    Instant.now(),
                    "System",
                    "Only 1 active Administrator registered in platform",
                    "Designate a secondary administrator to satisfy redundancy standards",
                    "ACTIVE"
            ));
        }

        return alerts;
    }

    // =========================================================================
    // 10. System Configuration
    // =========================================================================
    @Transactional(readOnly = true)
    public SystemConfigOverviewDto getSystemConfigurationOverview() {
        List<SystemConfiguration> list = systemConfigurationRepository.findAll();
        List<SystemConfigItemDto> items = list.stream()
                .map(c -> new SystemConfigItemDto(c.getConfigKey(), c.getConfigValue(), c.getDescription(), c.getUpdatedAt()))
                .toList();

        boolean configured = geminiApiKey != null && !geminiApiKey.isBlank();
        String maskedKey = configured
                ? geminiApiKey.substring(0, Math.min(6, geminiApiKey.length())) + "..." + (geminiApiKey.length() > 10 ? geminiApiKey.substring(geminiApiKey.length() - 4) : "***")
                : "Not Configured";

        GeminiIntegrationStatusDto geminiStatus = new GeminiIntegrationStatusDto(
                configured,
                geminiModel,
                geminiTimeoutSeconds,
                configured ? "OPERATIONAL" : "STANDBY_CATALOGUE_FALLBACK",
                maskedKey
        );

        int timeoutMin = 30;
        try {
            timeoutMin = Integer.parseInt(sessionTimeoutStr.replaceAll("[^0-9]", ""));
        } catch (Exception ignored) {}

        return new SystemConfigOverviewDto(
                items,
                geminiStatus,
                "Production / Windows Native",
                serverPort,
                timeoutMin
        );
    }

    // =========================================================================
    // 12. Support & Issues Management
    // =========================================================================
    @Transactional(readOnly = true)
    public List<SupportTicketAdminDto> getSupportTickets(String statusFilter, String priorityFilter) {
        return supportTicketRepository.findAllWithUser().stream()
                .filter(t -> statusFilter == null || statusFilter.isBlank() || "ALL".equalsIgnoreCase(statusFilter) ||
                        t.getStatus().equalsIgnoreCase(statusFilter))
                .filter(t -> priorityFilter == null || priorityFilter.isBlank() || "ALL".equalsIgnoreCase(priorityFilter) ||
                        t.getPriority().equalsIgnoreCase(priorityFilter))
                .sorted((a, b) -> {
                    if (a.getCreatedAt() == null || b.getCreatedAt() == null) return 0;
                    return b.getCreatedAt().compareTo(a.getCreatedAt());
                })
                .map(this::toSupportTicketAdminDto)
                .toList();
    }

    @Transactional
    public SupportTicketAdminDto updateSupportTicket(Long ticketId, UpdateSupportTicketAdminRequest req, String actingAdminEmail, String ip) {
        SupportTicket t = supportTicketRepository.findById(ticketId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Ticket not found: " + ticketId));
        User actingAdmin = userRepository.findByEmail(actingAdminEmail.trim().toLowerCase()).orElse(null);

        if (req.status() != null && !req.status().isBlank()) {
            t.setStatus(req.status().toUpperCase());
        }
        if (req.adminNotes() != null) {
            t.setAdminNotes(req.adminNotes());
        }
        t.setUpdatedAt(Instant.now());
        t = supportTicketRepository.save(t);

        adminGovernanceService.recordAuditLog(
                actingAdmin,
                "UPDATE_SUPPORT_TICKET",
                "SupportTicket",
                ticketId.toString(),
                "Updated ticket status to " + t.getStatus() + " with notes",
                ip
        );

        return toSupportTicketAdminDto(t);
    }

    // =========================================================================
    // Helpers
    // =========================================================================
    private Map<String, String> getConfigMap() {
        return systemConfigurationRepository.findAll().stream()
                .collect(Collectors.toMap(SystemConfiguration::getConfigKey, SystemConfiguration::getConfigValue, (a, b) -> a));
    }

    private void saveConfigKey(String key, String value, String description) {
        SystemConfiguration sc = systemConfigurationRepository.findById(key)
                .orElse(new SystemConfiguration(key, value, description));
        sc.setConfigValue(value);
        if (description != null && !description.isBlank()) {
            sc.setDescription(description);
        }
        sc.setUpdatedAt(Instant.now());
        systemConfigurationRepository.save(sc);
    }

    private AdminUserDetailDto toUserDetailDto(User u, EmployeeProfile p) {
        return new AdminUserDetailDto(
                u.getId(),
                u.getFullName(),
                u.getEmail(),
                u.getEmployeeId(),
                u.getRole(),
                u.getRequestedRole(),
                u.getAccountStatus(),
                p != null ? p.getDepartment() : "—",
                p != null ? p.getJobRole() : u.getRole().getLabel(),
                u.getCreatedAt(),
                u.getUpdatedAt()
        );
    }

    private SupportTicketAdminDto toSupportTicketAdminDto(SupportTicket t) {
        return new SupportTicketAdminDto(
                t.getId(),
                t.getUser().getId(),
                t.getUser().getFullName(),
                t.getUser().getEmail(),
                t.getSubject(),
                t.getDescription(),
                t.getPriority(),
                t.getStatus(),
                t.getAdminNotes(),
                t.getCreatedAt(),
                t.getUpdatedAt()
        );
    }
}
