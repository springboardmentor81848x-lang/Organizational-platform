package com.skillsphere.controller;

import com.skillsphere.dto.*;
import com.skillsphere.entity.SystemConfiguration;
import com.skillsphere.service.AdminGovernanceService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class AdminGovernanceController {

    private final AdminGovernanceService governanceService;

    public AdminGovernanceController(AdminGovernanceService governanceService) {
        this.governanceService = governanceService;
    }

    // --- Support Tickets (General & Admin) ---

    @PostMapping("/support/tickets")
    public ResponseEntity<SupportTicketDto> createTicket(@AuthenticationPrincipal UserDetails userDetails,
                                                         @RequestBody SupportTicketDto.CreateRequest req) {
        SupportTicketDto dto = governanceService.createTicket(userDetails.getUsername(), req);
        return ResponseEntity.status(HttpStatus.CREATED).body(dto);
    }

    @GetMapping("/support/tickets/my")
    public ResponseEntity<List<SupportTicketDto>> getMyTickets(@AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(governanceService.getMyTickets(userDetails.getUsername()));
    }

    @PatchMapping("/support/tickets/{id}/close")
    public ResponseEntity<SupportTicketDto> closeMyTicket(@AuthenticationPrincipal UserDetails userDetails,
                                                          @PathVariable Long id) {
        return ResponseEntity.ok(governanceService.closeMyTicket(userDetails.getUsername(), id));
    }

    @GetMapping("/admin/support/tickets")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<SupportTicketDto>> getAllTickets() {
        return ResponseEntity.ok(governanceService.getAllTickets());
    }

    @PatchMapping("/admin/support/tickets/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<SupportTicketDto> updateTicket(@PathVariable Long id,
                                                         @RequestBody SupportTicketDto.UpdateRequest req) {
        return ResponseEntity.ok(governanceService.updateTicket(id, req));
    }

    // --- Platform Feedback ---

    @PostMapping("/feedback")
    public ResponseEntity<PlatformFeedbackDto> submitFeedback(@AuthenticationPrincipal UserDetails userDetails,
                                                              @RequestBody PlatformFeedbackDto.CreateRequest req) {
        PlatformFeedbackDto dto = governanceService.submitFeedback(userDetails.getUsername(), req);
        return ResponseEntity.status(HttpStatus.CREATED).body(dto);
    }

    @GetMapping("/feedback/all")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR_SPECIALIST')")
    public ResponseEntity<List<PlatformFeedbackDto>> getAllFeedback() {
        return ResponseEntity.ok(governanceService.getAllFeedback());
    }

    // --- Audit Logs ---

    @GetMapping("/admin/audit-logs")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<AuditLogDto>> getAuditLogs() {
        return ResponseEntity.ok(governanceService.getAllAuditLogs());
    }

    // --- System Configuration ---

    @GetMapping("/admin/system-config")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<SystemConfiguration>> getConfigurations() {
        return ResponseEntity.ok(governanceService.getAllConfigurations());
    }

    @PutMapping("/admin/system-config/{key}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<SystemConfiguration> updateConfiguration(@PathVariable String key,
                                                                   @RequestBody Map<String, String> body) {
        String value = body.getOrDefault("value", "");
        return ResponseEntity.ok(governanceService.updateConfiguration(key, value));
    }
}
