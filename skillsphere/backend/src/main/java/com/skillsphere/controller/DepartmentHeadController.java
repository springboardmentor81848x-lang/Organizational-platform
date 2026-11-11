package com.skillsphere.controller;

import com.skillsphere.dto.DepartmentHeadDtos.*;
import com.skillsphere.service.DepartmentHeadService;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/department-head")
@PreAuthorize("hasAnyRole('DEPARTMENT_HEAD', 'ADMIN')")
public class DepartmentHeadController {

    private final DepartmentHeadService departmentHeadService;

    public DepartmentHeadController(DepartmentHeadService departmentHeadService) {
        this.departmentHeadService = departmentHeadService;
    }

    // =========================================================================
    // 1. Dashboard Overview
    // =========================================================================
    @GetMapping("/overview")
    public ResponseEntity<DepartmentHeadOverviewDto> getOverview(
            @RequestParam(required = false) Long departmentId,
            Authentication auth) {
        return ResponseEntity.ok(departmentHeadService.getDashboardOverview(auth.getName(), departmentId));
    }

    // =========================================================================
    // 2. Department Overview & Teams
    // =========================================================================
    @GetMapping("/department")
    public ResponseEntity<DepartmentProfileDto> getDepartmentOverview(
            @RequestParam(required = false) Long departmentId,
            Authentication auth) {
        return ResponseEntity.ok(departmentHeadService.getDepartmentOverview(auth.getName(), departmentId));
    }

    @GetMapping("/teams")
    public ResponseEntity<List<DeptTeamSummaryDto>> getTeams(
            @RequestParam(required = false) Long departmentId,
            Authentication auth) {
        return ResponseEntity.ok(departmentHeadService.getDepartmentOverview(auth.getName(), departmentId).teams());
    }

    // =========================================================================
    // 3. Department Skill Matrix
    // =========================================================================
    @GetMapping("/skill-matrix")
    public ResponseEntity<DeptSkillMatrixDto> getSkillMatrix(
            @RequestParam(required = false) Long departmentId,
            @RequestParam(required = false) Long teamId,
            @RequestParam(required = false) String search,
            Authentication auth) {
        return ResponseEntity.ok(departmentHeadService.getDepartmentSkillMatrix(auth.getName(), departmentId, teamId, search));
    }

    // =========================================================================
    // 4. Gap Heatmap
    // =========================================================================
    @GetMapping("/heatmap")
    public ResponseEntity<DeptHeatmapDto> getHeatmap(
            @RequestParam(required = false) Long departmentId,
            Authentication auth) {
        return ResponseEntity.ok(departmentHeadService.getDepartmentHeatmap(auth.getName(), departmentId));
    }

    // =========================================================================
    // 5. Workforce Capability
    // =========================================================================
    @GetMapping("/capability")
    public ResponseEntity<DeptCapabilityProfileDto> getCapability(
            @RequestParam(required = false) Long departmentId,
            Authentication auth) {
        return ResponseEntity.ok(departmentHeadService.getWorkforceCapability(auth.getName(), departmentId));
    }

    // =========================================================================
    // 6. Learning & Mentoring Overview
    // =========================================================================
    @GetMapping("/learning-mentoring")
    public ResponseEntity<DeptLearningMentoringDto> getLearningAndMentoring(
            @RequestParam(required = false) Long departmentId,
            Authentication auth) {
        return ResponseEntity.ok(departmentHeadService.getLearningAndMentoringOverview(auth.getName(), departmentId));
    }

    // =========================================================================
    // 7. Risk & Priority Alerts
    // =========================================================================
    @GetMapping("/risks")
    public ResponseEntity<List<DeptRiskAlertDto>> getRiskAlerts(
            @RequestParam(required = false) Long departmentId,
            Authentication auth) {
        return ResponseEntity.ok(departmentHeadService.getDepartmentRiskAlerts(auth.getName(), departmentId));
    }

    // =========================================================================
    // 8. Reports & Analytics
    // =========================================================================
    @GetMapping("/reports")
    public ResponseEntity<DeptReportResultDto> getReport(
            @RequestParam(required = false) String reportType,
            @RequestParam(required = false) Long departmentId,
            Authentication auth) {
        return ResponseEntity.ok(departmentHeadService.generateDepartmentReport(auth.getName(), reportType, departmentId));
    }

    @GetMapping("/reports/csv")
    public ResponseEntity<byte[]> exportReportCsv(
            @RequestParam(required = false) String reportType,
            @RequestParam(required = false) Long departmentId,
            Authentication auth) {
        DeptReportResultDto report = departmentHeadService.generateDepartmentReport(auth.getName(), reportType, departmentId);

        StringBuilder sb = new StringBuilder();
        sb.append("# ").append(report.title()).append("\n");
        sb.append("# Department: ").append(report.departmentName()).append("\n");
        sb.append("# Generated At: ").append(report.generatedAt()).append("\n\n");

        // Headers
        sb.append(String.join(",", report.tableHeaders())).append("\n");

        // Rows
        for (Map<String, Object> row : report.tableRows()) {
            StringBuilder rowBuilder = new StringBuilder();
            for (int i = 0; i < report.tableHeaders().size(); i++) {
                String header = report.tableHeaders().get(i);
                Object val = row.get(header);
                String strVal = val != null ? val.toString().replace("\"", "\"\"") : "";
                if (strVal.contains(",") || strVal.contains("\"") || strVal.contains("\n")) {
                    rowBuilder.append("\"").append(strVal).append("\"");
                } else {
                    rowBuilder.append(strVal);
                }
                if (i < report.tableHeaders().size() - 1) {
                    rowBuilder.append(",");
                }
            }
            sb.append(rowBuilder).append("\n");
        }

        byte[] bytes = sb.toString().getBytes(StandardCharsets.UTF_8);
        String filename = "dept_report_" + report.reportType().toLowerCase() + ".csv";

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
                .body(bytes);
    }

    @GetMapping("/leadership-reports/shared")
    public ResponseEntity<List<DeptLeadershipSharedReportDto>> getSharedLeadershipReports(
            @RequestParam(required = false) Long departmentId,
            Authentication auth) {
        return ResponseEntity.ok(departmentHeadService.getSharedLeadershipReports(auth.getName(), departmentId));
    }

    // =========================================================================
    // 9. Department Announcements
    // =========================================================================
    @GetMapping("/announcements")
    public ResponseEntity<List<DeptAnnouncementDto>> getAnnouncements(
            @RequestParam(required = false) Long departmentId,
            Authentication auth) {
        return ResponseEntity.ok(departmentHeadService.getDepartmentAnnouncements(auth.getName(), departmentId));
    }

    @PostMapping("/announcements")
    public ResponseEntity<DeptAnnouncementDto> createAnnouncement(
            @Valid @RequestBody CreateDeptAnnouncementRequest req,
            @RequestParam(required = false) Long departmentId,
            Authentication auth) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(departmentHeadService.createDepartmentAnnouncement(auth.getName(), req, departmentId));
    }

    @PostMapping("/announcements/{id}/read")
    public ResponseEntity<Void> markAnnouncementRead(
            @PathVariable Long id,
            Authentication auth) {
        departmentHeadService.markAnnouncementRead(id, auth.getName());
        return ResponseEntity.ok().build();
    }

    // =========================================================================
    // 10. Department Notifications
    // =========================================================================
    @GetMapping("/notifications")
    public ResponseEntity<List<DeptNotificationDto>> getNotifications(
            @RequestParam(defaultValue = "false") boolean unreadOnly,
            Authentication auth) {
        return ResponseEntity.ok(departmentHeadService.getNotifications(auth.getName(), unreadOnly));
    }

    @PostMapping("/notifications/{id}/read")
    public ResponseEntity<Void> markNotificationRead(
            @PathVariable Long id,
            Authentication auth) {
        departmentHeadService.markNotificationRead(id, auth.getName());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/notifications/read-all")
    public ResponseEntity<Void> markAllNotificationsRead(Authentication auth) {
        departmentHeadService.markAllNotificationsRead(auth.getName());
        return ResponseEntity.ok().build();
    }
}
