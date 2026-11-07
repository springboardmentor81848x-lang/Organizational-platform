package com.skillsphere.controller;

import com.skillsphere.dto.ReportDtos;
import com.skillsphere.service.ReportService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/reports")
public class ReportController {

    private final ReportService reportService;

    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    @GetMapping("/my")
    public ResponseEntity<ReportDtos.EmployeeReportDto> getMyReport(@AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(reportService.getEmployeeReport(userDetails.getUsername()));
    }

    @GetMapping("/team/{teamId}")
    @PreAuthorize("hasAnyRole('MANAGER', 'DEPARTMENT_HEAD', 'ADMIN')")
    public ResponseEntity<ReportDtos.TeamReportDto> getTeamReport(@PathVariable Long teamId,
                                                                  @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(reportService.getTeamReport(teamId, userDetails.getUsername()));
    }

    @GetMapping("/department/{deptId}")
    @PreAuthorize("hasAnyRole('DEPARTMENT_HEAD', 'HR_SPECIALIST', 'ADMIN')")
    public ResponseEntity<ReportDtos.DepartmentReportDto> getDepartmentReport(@PathVariable Long deptId,
                                                                              @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(reportService.getDepartmentReport(deptId, userDetails.getUsername()));
    }

    @GetMapping("/organization")
    @PreAuthorize("hasAnyRole('HR_SPECIALIST', 'ADMIN')")
    public ResponseEntity<ReportDtos.OrganizationReportDto> getOrganizationReport() {
        return ResponseEntity.ok(reportService.getOrganizationReport());
    }

    @GetMapping("/admin")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ReportDtos.AdminReportDto> getAdminReport() {
        return ResponseEntity.ok(reportService.getAdminReport());
    }

    @GetMapping(value = "/export/gaps.csv", produces = "text/csv")
    @PreAuthorize("hasAnyRole('HR_SPECIALIST', 'ADMIN', 'DEPARTMENT_HEAD', 'MANAGER')")
    public ResponseEntity<String> exportGapsCsv(Authentication auth) {
        String callerEmail = auth != null ? auth.getName() : null;
        String csv = reportService.generateSkillGapsCsv(callerEmail);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"skill_gaps_report.csv\"")
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(csv);
    }
}
