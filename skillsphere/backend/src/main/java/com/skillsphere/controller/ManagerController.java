package com.skillsphere.controller;

import com.skillsphere.dto.*;
import com.skillsphere.service.ManagerService;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/manager")
@PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
public class ManagerController {

    private final ManagerService managerService;

    public ManagerController(ManagerService managerService) {
        this.managerService = managerService;
    }

    @GetMapping("/teams")
    public ResponseEntity<List<TeamDto>> getManagedTeams(@AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(managerService.getManagedTeamsDto(userDetails.getUsername()));
    }

    @GetMapping("/overview")
    public ResponseEntity<ManagerDashboardOverviewDto> getOverview(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(required = false) Long teamId) {
        return ResponseEntity.ok(managerService.getDashboardOverview(userDetails.getUsername(), teamId));
    }

    @GetMapping("/team-members")
    public ResponseEntity<List<ManagerTeamMemberDto>> getTeamMembers(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(required = false) Long teamId,
            @RequestParam(required = false) String search) {
        return ResponseEntity.ok(managerService.getTeamMembers(userDetails.getUsername(), teamId, search));
    }

    @GetMapping("/team-members/{employeeId}")
    public ResponseEntity<ManagerEmployeeDetailDto> getEmployeeDetail(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long employeeId) {
        return ResponseEntity.ok(managerService.getEmployeeDetail(userDetails.getUsername(), employeeId));
    }

    @GetMapping("/skill-inventory")
    public ResponseEntity<List<TeamSkillInventoryDto>> getSkillInventory(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(required = false) Long teamId,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String search) {
        return ResponseEntity.ok(managerService.getSkillInventory(userDetails.getUsername(), teamId, categoryId, search));
    }

    @GetMapping("/gap-analysis")
    public ResponseEntity<List<TeamGapAnalysisDto>> getGapAnalysis(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(required = false) Long teamId,
            @RequestParam(required = false) String priority,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String search) {
        return ResponseEntity.ok(managerService.getGapAnalysis(userDetails.getUsername(), teamId, priority, categoryId, search));
    }

    @GetMapping("/heatmap")
    public ResponseEntity<ManagerHeatmapDto> getHeatmap(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(required = false) Long teamId) {
        return ResponseEntity.ok(managerService.getHeatmap(userDetails.getUsername(), teamId));
    }

    @GetMapping("/assessments")
    public ResponseEntity<List<AssessmentAttemptDto>> getAssessments(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(required = false) Long teamId) {
        return ResponseEntity.ok(managerService.getTeamAssessments(userDetails.getUsername(), teamId));
    }

    @GetMapping("/reassessments")
    public ResponseEntity<List<ReassessmentRequestDto>> getReassessments(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(required = false) Long teamId) {
        return ResponseEntity.ok(managerService.getReassessmentRequests(userDetails.getUsername(), teamId));
    }

    @PostMapping("/reassessments")
    public ResponseEntity<ReassessmentRequestDto> createReassessment(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody CreateReassessmentRequest req) {
        ReassessmentRequestDto created = managerService.createReassessmentRequest(userDetails.getUsername(), req);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @DeleteMapping("/reassessments/{id}")
    public ResponseEntity<Void> cancelReassessment(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long id) {
        managerService.cancelReassessmentRequest(userDetails.getUsername(), id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/recommendations")
    public ResponseEntity<RecommendationResponse> getAiRecommendations(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(required = false) Long teamId) {
        return ResponseEntity.ok(managerService.getTeamAiRecommendations(userDetails.getUsername(), teamId));
    }

    @GetMapping("/course-recommendations")
    public ResponseEntity<List<ManagerCourseRecommendationDto>> getCourseRecommendations(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(required = false) Long teamId) {
        return ResponseEntity.ok(managerService.getCourseRecommendations(userDetails.getUsername(), teamId));
    }

    @PostMapping("/course-recommendations")
    public ResponseEntity<ManagerCourseRecommendationDto> recommendCourse(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody CreateManagerCourseRecommendationRequest req) {
        ManagerCourseRecommendationDto created = managerService.recommendCourse(userDetails.getUsername(), req);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping("/learning-progress")
    public ResponseEntity<List<EmployeeLearningDto>> getLearningProgress(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(required = false) Long teamId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String search) {
        return ResponseEntity.ok(managerService.getLearningProgress(userDetails.getUsername(), teamId, status, search));
    }

    @GetMapping("/mentorship")
    public ResponseEntity<ManagerMentorshipStatusDto> getMentorshipStatus(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(required = false) Long teamId) {
        return ResponseEntity.ok(managerService.getMentorshipSupport(userDetails.getUsername(), teamId));
    }

    @PostMapping("/mentorship/refer")
    public ResponseEntity<Void> referMentor(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody ManagerMentorReferralRequest req) {
        managerService.referMentor(userDetails.getUsername(), req);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/reports")
    public ResponseEntity<ManagerReportDto> getReport(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(defaultValue = "TEAM_SKILL_GAP_SUMMARY") String reportType,
            @RequestParam(required = false) Long teamId) {
        return ResponseEntity.ok(managerService.getReport(userDetails.getUsername(), reportType, teamId));
    }

    @GetMapping("/reports/export")
    public ResponseEntity<String> exportReportCsv(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(defaultValue = "TEAM_SKILL_GAP_SUMMARY") String reportType,
            @RequestParam(required = false) Long teamId) {
        String csv = managerService.getReportCsv(userDetails.getUsername(), reportType, teamId);
        String filename = reportType.toLowerCase() + "_" + System.currentTimeMillis() + ".csv";
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
                .body(csv);
    }
}
