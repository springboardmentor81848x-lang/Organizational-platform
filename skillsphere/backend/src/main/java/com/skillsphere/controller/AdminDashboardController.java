package com.skillsphere.controller;

import com.skillsphere.dto.AdminDashboardDtos.*;
import com.skillsphere.entity.AccountStatus;
import com.skillsphere.entity.Role;
import com.skillsphere.service.AdminDashboardService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminDashboardController {

    private final AdminDashboardService adminDashboardService;

    public AdminDashboardController(AdminDashboardService adminDashboardService) {
        this.adminDashboardService = adminDashboardService;
    }

    // =========================================================================
    // 1. Dashboard Overview
    // =========================================================================
    @GetMapping("/overview")
    public ResponseEntity<AdminDashboardOverviewDto> getOverview() {
        return ResponseEntity.ok(adminDashboardService.getDashboardOverview());
    }

    // =========================================================================
    // 2. User Lifecycle Management
    // =========================================================================
    @GetMapping("/users/detailed")
    public ResponseEntity<List<AdminUserDetailDto>> getUsers(
            @RequestParam(required = false) Role role,
            @RequestParam(required = false) String department,
            @RequestParam(required = false) AccountStatus status,
            @RequestParam(required = false) String search) {
        return ResponseEntity.ok(adminDashboardService.getUsers(role, department, status, search));
    }

    @GetMapping("/users/detailed/{id}")
    public ResponseEntity<AdminUserDetailDto> getUserById(@PathVariable Long id) {
        return ResponseEntity.ok(adminDashboardService.getUserById(id));
    }

    @PostMapping("/users/detailed")
    public ResponseEntity<AdminUserDetailDto> createUser(
            @Valid @RequestBody CreateUserAdminRequest req,
            Authentication auth,
            HttpServletRequest request) {
        AdminUserDetailDto created = adminDashboardService.createUser(req, auth.getName(), request.getRemoteAddr());
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/users/detailed/{id}")
    public ResponseEntity<AdminUserDetailDto> updateUser(
            @PathVariable Long id,
            @Valid @RequestBody UpdateUserAdminRequest req,
            Authentication auth,
            HttpServletRequest request) {
        AdminUserDetailDto updated = adminDashboardService.updateUser(id, req, auth.getName(), request.getRemoteAddr());
        return ResponseEntity.ok(updated);
    }

    // =========================================================================
    // 3. Roles & Permissions Management
    // =========================================================================
    @GetMapping("/roles/overview")
    public ResponseEntity<List<RolePermissionDto>> getRolesOverview() {
        return ResponseEntity.ok(adminDashboardService.getRolesOverview());
    }

    @PostMapping("/roles/assign/{userId}")
    public ResponseEntity<AdminUserDetailDto> assignRole(
            @PathVariable Long userId,
            @Valid @RequestBody AssignRoleRequest req,
            Authentication auth,
            HttpServletRequest request) {
        AdminUserDetailDto updated = adminDashboardService.assignUserRole(userId, req.role(), auth.getName(), request.getRemoteAddr());
        return ResponseEntity.ok(updated);
    }

    @GetMapping("/roles/history")
    public ResponseEntity<List<RoleAssignmentHistoryDto>> getRoleAssignmentHistory() {
        return ResponseEntity.ok(adminDashboardService.getRoleAssignmentHistory());
    }

    // =========================================================================
    // 6. Communication Settings (Least-Privilege Privacy Enforcement)
    // =========================================================================
    @GetMapping("/communication-settings")
    public ResponseEntity<CommunicationSettingsDto> getCommunicationSettings() {
        return ResponseEntity.ok(adminDashboardService.getCommunicationSettings());
    }

    @PutMapping("/communication-settings")
    public ResponseEntity<CommunicationSettingsDto> updateCommunicationSettings(
            @Valid @RequestBody UpdateCommunicationSettingsRequest req,
            Authentication auth,
            HttpServletRequest request) {
        CommunicationSettingsDto updated = adminDashboardService.updateCommunicationSettings(req, auth.getName(), request.getRemoteAddr());
        return ResponseEntity.ok(updated);
    }

    // =========================================================================
    // 7. Notification Settings
    // =========================================================================
    @GetMapping("/notification-settings")
    public ResponseEntity<NotificationSettingsDto> getNotificationSettings() {
        return ResponseEntity.ok(adminDashboardService.getNotificationSettings());
    }

    @PutMapping("/notification-settings")
    public ResponseEntity<NotificationSettingsDto> updateNotificationSettings(
            @Valid @RequestBody UpdateNotificationSettingsRequest req,
            Authentication auth,
            HttpServletRequest request) {
        NotificationSettingsDto updated = adminDashboardService.updateNotificationSettings(req, auth.getName(), request.getRemoteAddr());
        return ResponseEntity.ok(updated);
    }

    // =========================================================================
    // 9. Security Monitoring
    // =========================================================================
    @GetMapping("/security/overview")
    public ResponseEntity<AdminSecurityOverviewDto> getSecurityOverview() {
        return ResponseEntity.ok(adminDashboardService.getSecurityOverview());
    }

    // =========================================================================
    // 10. System Configuration
    // =========================================================================
    @GetMapping("/configuration/overview")
    public ResponseEntity<SystemConfigOverviewDto> getSystemConfigurationOverview() {
        return ResponseEntity.ok(adminDashboardService.getSystemConfigurationOverview());
    }

    // =========================================================================
    // 12. Support & Issues Management
    // =========================================================================
    @GetMapping("/support-tickets")
    public ResponseEntity<List<SupportTicketAdminDto>> getSupportTickets(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String priority) {
        return ResponseEntity.ok(adminDashboardService.getSupportTickets(status, priority));
    }

    @PutMapping("/support-tickets/{id}")
    public ResponseEntity<SupportTicketAdminDto> updateSupportTicket(
            @PathVariable Long id,
            @Valid @RequestBody UpdateSupportTicketAdminRequest req,
            Authentication auth,
            HttpServletRequest request) {
        SupportTicketAdminDto updated = adminDashboardService.updateSupportTicket(id, req, auth.getName(), request.getRemoteAddr());
        return ResponseEntity.ok(updated);
    }
}
