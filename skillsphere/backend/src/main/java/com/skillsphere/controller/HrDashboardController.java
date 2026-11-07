package com.skillsphere.controller;

import com.skillsphere.dto.HrDtos.*;
import com.skillsphere.entity.Role;
import com.skillsphere.entity.User;
import com.skillsphere.exception.ApiException;
import com.skillsphere.repository.UserRepository;
import com.skillsphere.service.HrDashboardService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/hr")
public class HrDashboardController {

    private final HrDashboardService hrDashboardService;
    private final UserRepository userRepository;

    public HrDashboardController(HrDashboardService hrDashboardService, UserRepository userRepository) {
        this.hrDashboardService = hrDashboardService;
        this.userRepository = userRepository;
    }

    private User getAuthenticatedUser(Authentication auth) {
        if (auth == null || auth.getName() == null) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Unauthenticated");
        }
        return userRepository.findByEmail(auth.getName().trim().toLowerCase())
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "User not found"));
    }

    // =========================================================================
    // 1. Dashboard Overview
    // =========================================================================
    @GetMapping("/overview")
    @PreAuthorize("hasAnyRole('HR_SPECIALIST', 'ADMIN')")
    public ResponseEntity<HrDashboardOverviewDto> getOverview() {
        return ResponseEntity.ok(hrDashboardService.getOverview());
    }

    // =========================================================================
    // 2. Employee Directory & Profiles
    // =========================================================================
    @GetMapping("/employees")
    @PreAuthorize("hasAnyRole('HR_SPECIALIST', 'ADMIN')")
    public ResponseEntity<List<HrEmployeeDirectoryItemDto>> getEmployeeDirectory(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String department,
            @RequestParam(required = false) String role) {
        return ResponseEntity.ok(hrDashboardService.getEmployeeDirectory(search, department, role));
    }

    @GetMapping("/employees/{id}/dossier")
    @PreAuthorize("hasAnyRole('HR_SPECIALIST', 'ADMIN')")
    public ResponseEntity<HrEmployeeDossierDto> getEmployeeDossier(@PathVariable Long id) {
        return ResponseEntity.ok(hrDashboardService.getEmployeeDossier(id));
    }

    // =========================================================================
    // 3. Competency Framework
    // =========================================================================
    @GetMapping("/competencies")
    @PreAuthorize("hasAnyRole('HR_SPECIALIST', 'ADMIN')")
    public ResponseEntity<List<HrCompetencyDto>> getCompetencies() {
        return ResponseEntity.ok(hrDashboardService.getCompetencies());
    }

    @PostMapping("/competencies")
    @PreAuthorize("hasAnyRole('HR_SPECIALIST', 'ADMIN')")
    public ResponseEntity<HrCompetencyDto> createCompetency(@Valid @RequestBody CreateCompetencyRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(hrDashboardService.createCompetency(req));
    }

    @PutMapping("/competencies/{id}")
    @PreAuthorize("hasAnyRole('HR_SPECIALIST', 'ADMIN')")
    public ResponseEntity<HrCompetencyDto> updateCompetency(@PathVariable Long id, @Valid @RequestBody UpdateCompetencyRequest req) {
        return ResponseEntity.ok(hrDashboardService.updateCompetency(id, req));
    }

    @PatchMapping("/competencies/{id}/toggle-active")
    @PreAuthorize("hasAnyRole('HR_SPECIALIST', 'ADMIN')")
    public ResponseEntity<HrCompetencyDto> toggleCompetencyActive(@PathVariable Long id) {
        return ResponseEntity.ok(hrDashboardService.toggleCompetencyActive(id));
    }

    // =========================================================================
    // 4. Role Requirements
    // =========================================================================
    @GetMapping("/role-requirements")
    @PreAuthorize("hasAnyRole('HR_SPECIALIST', 'ADMIN')")
    public ResponseEntity<List<HrRoleRequirementDto>> getRoleRequirements(@RequestParam(required = false) String jobRole) {
        return ResponseEntity.ok(hrDashboardService.getRoleRequirements(jobRole));
    }

    @GetMapping("/role-requirements/matrix")
    @PreAuthorize("hasAnyRole('HR_SPECIALIST', 'ADMIN')")
    public ResponseEntity<List<RoleSkillMatrixDto>> getRoleMatrix() {
        return ResponseEntity.ok(hrDashboardService.getRoleMatrix());
    }

    @PostMapping("/role-requirements")
    @PreAuthorize("hasAnyRole('HR_SPECIALIST', 'ADMIN')")
    public ResponseEntity<HrRoleRequirementDto> createRoleRequirement(@Valid @RequestBody CreateRoleRequirementRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(hrDashboardService.createRoleRequirement(req));
    }

    @PutMapping("/role-requirements/{id}")
    @PreAuthorize("hasAnyRole('HR_SPECIALIST', 'ADMIN')")
    public ResponseEntity<HrRoleRequirementDto> updateRoleRequirement(@PathVariable Long id, @Valid @RequestBody UpdateRoleRequirementRequest req) {
        return ResponseEntity.ok(hrDashboardService.updateRoleRequirement(id, req));
    }

    @DeleteMapping("/role-requirements/{id}")
    @PreAuthorize("hasAnyRole('HR_SPECIALIST', 'ADMIN')")
    public ResponseEntity<Void> deleteRoleRequirement(@PathVariable Long id) {
        hrDashboardService.deleteRoleRequirement(id);
        return ResponseEntity.noContent().build();
    }

    // =========================================================================
    // 5. Organisation Skill Gap Analysis
    // =========================================================================
    @GetMapping("/skill-gaps/summary")
    @PreAuthorize("hasAnyRole('HR_SPECIALIST', 'ADMIN')")
    public ResponseEntity<HrSkillGapSummaryDto> getSkillGapSummary() {
        return ResponseEntity.ok(hrDashboardService.getSkillGapSummary());
    }

    @GetMapping("/skill-gaps/heatmap")
    @PreAuthorize("hasAnyRole('HR_SPECIALIST', 'ADMIN')")
    public ResponseEntity<OrgHeatmapDto> getOrgHeatmap() {
        return ResponseEntity.ok(hrDashboardService.getOrgHeatmap());
    }

    // =========================================================================
    // 6. Learning Catalogue
    // =========================================================================
    @GetMapping("/courses")
    @PreAuthorize("hasAnyRole('HR_SPECIALIST', 'ADMIN')")
    public ResponseEntity<List<HrCourseDto>> getCourses() {
        return ResponseEntity.ok(hrDashboardService.getCourses());
    }

    @PostMapping("/courses")
    @PreAuthorize("hasAnyRole('HR_SPECIALIST', 'ADMIN')")
    public ResponseEntity<HrCourseDto> createCourse(@Valid @RequestBody CreateOrUpdateCourseRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(hrDashboardService.createCourse(req));
    }

    @PutMapping("/courses/{id}")
    @PreAuthorize("hasAnyRole('HR_SPECIALIST', 'ADMIN')")
    public ResponseEntity<HrCourseDto> updateCourse(@PathVariable Long id, @Valid @RequestBody CreateOrUpdateCourseRequest req) {
        return ResponseEntity.ok(hrDashboardService.updateCourse(id, req));
    }

    @PatchMapping("/courses/{id}/toggle-active")
    @PreAuthorize("hasAnyRole('HR_SPECIALIST', 'ADMIN')")
    public ResponseEntity<HrCourseDto> toggleCourseActive(@PathVariable Long id) {
        return ResponseEntity.ok(hrDashboardService.toggleCourseActive(id));
    }

    // =========================================================================
    // 7. Learning Paths
    // =========================================================================
    @GetMapping("/learning-paths")
    @PreAuthorize("hasAnyRole('HR_SPECIALIST', 'ADMIN')")
    public ResponseEntity<List<HrLearningPathDto>> getLearningPaths() {
        return ResponseEntity.ok(hrDashboardService.getLearningPaths());
    }

    @PostMapping("/learning-paths")
    @PreAuthorize("hasAnyRole('HR_SPECIALIST', 'ADMIN')")
    public ResponseEntity<HrLearningPathDto> createLearningPath(@Valid @RequestBody CreateOrUpdateLearningPathRequest req, Authentication auth) {
        User user = getAuthenticatedUser(auth);
        return ResponseEntity.status(HttpStatus.CREATED).body(hrDashboardService.createLearningPath(req, user.getId()));
    }

    @PutMapping("/learning-paths/{id}")
    @PreAuthorize("hasAnyRole('HR_SPECIALIST', 'ADMIN')")
    public ResponseEntity<HrLearningPathDto> updateLearningPath(@PathVariable Long id, @Valid @RequestBody CreateOrUpdateLearningPathRequest req) {
        return ResponseEntity.ok(hrDashboardService.updateLearningPath(id, req));
    }

    @PatchMapping("/learning-paths/{id}/toggle-active")
    @PreAuthorize("hasAnyRole('HR_SPECIALIST', 'ADMIN')")
    public ResponseEntity<HrLearningPathDto> toggleLearningPathActive(@PathVariable Long id) {
        return ResponseEntity.ok(hrDashboardService.toggleLearningPathActive(id));
    }

    // =========================================================================
    // 8. Training Programmes
    // =========================================================================
    @GetMapping("/training-programmes")
    @PreAuthorize("hasAnyRole('HR_SPECIALIST', 'ADMIN')")
    public ResponseEntity<List<HrTrainingProgrammeDto>> getTrainingProgrammes() {
        return ResponseEntity.ok(hrDashboardService.getTrainingProgrammes());
    }

    @PostMapping("/training-programmes")
    @PreAuthorize("hasAnyRole('HR_SPECIALIST', 'ADMIN')")
    public ResponseEntity<HrTrainingProgrammeDto> createTrainingProgramme(@Valid @RequestBody CreateOrUpdateTrainingProgrammeRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(hrDashboardService.createTrainingProgramme(req));
    }

    @PutMapping("/training-programmes/{id}")
    @PreAuthorize("hasAnyRole('HR_SPECIALIST', 'ADMIN')")
    public ResponseEntity<HrTrainingProgrammeDto> updateTrainingProgramme(@PathVariable Long id, @Valid @RequestBody CreateOrUpdateTrainingProgrammeRequest req) {
        return ResponseEntity.ok(hrDashboardService.updateTrainingProgramme(id, req));
    }

    @GetMapping("/training-programmes/{id}/enrollments")
    @PreAuthorize("hasAnyRole('HR_SPECIALIST', 'ADMIN')")
    public ResponseEntity<List<ProgrammeEnrollmentDetailDto>> getProgrammeEnrollments(@PathVariable Long id) {
        return ResponseEntity.ok(hrDashboardService.getProgrammeEnrollments(id));
    }

    @PostMapping("/training-programmes/{id}/enroll")
    @PreAuthorize("hasAnyRole('HR_SPECIALIST', 'ADMIN')")
    public ResponseEntity<ProgrammeEnrollmentDetailDto> enrollEmployee(@PathVariable Long id, @Valid @RequestBody EnrollEmployeeRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(hrDashboardService.enrollEmployee(id, req.userId()));
    }

    @PatchMapping("/training-programmes/enrollments/{enrollmentId}/status")
    @PreAuthorize("hasAnyRole('HR_SPECIALIST', 'ADMIN')")
    public ResponseEntity<ProgrammeEnrollmentDetailDto> updateProgrammeEnrollmentStatus(
            @PathVariable Long enrollmentId,
            @Valid @RequestBody UpdateProgrammeEnrollmentStatusRequest req) {
        return ResponseEntity.ok(hrDashboardService.updateProgrammeEnrollmentStatus(enrollmentId, req.status()));
    }

    // =========================================================================
    // 9. Mentor Directory
    // =========================================================================
    @GetMapping("/mentors")
    @PreAuthorize("hasAnyRole('HR_SPECIALIST', 'ADMIN')")
    public ResponseEntity<List<HrMentorDirectoryItemDto>> getMentorDirectory() {
        return ResponseEntity.ok(hrDashboardService.getMentorDirectory());
    }

    @GetMapping("/mentors/by-skill/{skillId}")
    @PreAuthorize("hasAnyRole('HR_SPECIALIST', 'ADMIN')")
    public ResponseEntity<MentorsForSkillDto> getMentorsForSkill(@PathVariable Long skillId) {
        return ResponseEntity.ok(hrDashboardService.getMentorsForSkill(skillId));
    }

    // =========================================================================
    // 10. Learning Progress
    // =========================================================================
    @GetMapping("/learning-progress")
    @PreAuthorize("hasAnyRole('HR_SPECIALIST', 'ADMIN')")
    public ResponseEntity<HrLearningProgressOverviewDto> getLearningProgressOverview() {
        return ResponseEntity.ok(hrDashboardService.getLearningProgressOverview());
    }

    // =========================================================================
    // 11. Feedback & Effectiveness
    // =========================================================================
    @GetMapping("/feedback")
    @PreAuthorize("hasAnyRole('HR_SPECIALIST', 'ADMIN')")
    public ResponseEntity<HrFeedbackOverviewDto> getFeedbackOverview() {
        return ResponseEntity.ok(hrDashboardService.getFeedbackOverview());
    }

    // =========================================================================
    // 12. Reports & Analytics & Leadership Sharing
    // =========================================================================
    @GetMapping("/reports/generate")
    @PreAuthorize("hasAnyRole('HR_SPECIALIST', 'ADMIN')")
    public ResponseEntity<HrReportResultDto> generateReport(
            @RequestParam(required = false) String reportType,
            @RequestParam(required = false) Long departmentId) {
        return ResponseEntity.ok(hrDashboardService.generateReport(reportType, departmentId));
    }

    @PostMapping("/leadership-reports/share")
    @PreAuthorize("hasAnyRole('HR_SPECIALIST', 'ADMIN')")
    public ResponseEntity<LeadershipReportShareDto> shareReportWithLeadership(
            @Valid @RequestBody ShareReportRequest req,
            Authentication auth) {
        User user = getAuthenticatedUser(auth);
        return ResponseEntity.status(HttpStatus.CREATED).body(hrDashboardService.shareReportWithLeadership(req, user.getId()));
    }

    @GetMapping("/leadership-reports/shared")
    @PreAuthorize("hasAnyRole('HR_SPECIALIST', 'ADMIN', 'DEPARTMENT_HEAD', 'MANAGER')")
    public ResponseEntity<List<LeadershipReportShareDto>> getSharedLeadershipReports(
            @RequestParam(required = false) Long departmentId,
            Authentication auth) {
        User user = getAuthenticatedUser(auth);
        return ResponseEntity.ok(hrDashboardService.getSharedLeadershipReports(user.getRole().name(), departmentId));
    }

    // =========================================================================
    // 13. Notifications & Announcements
    // =========================================================================
    @GetMapping("/announcements")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<HrAnnouncementDto>> getAnnouncements(Authentication auth) {
        User user = auth != null && auth.getName() != null ?
                userRepository.findByEmail(auth.getName().trim().toLowerCase()).orElse(null) : null;
        Long userId = user != null ? user.getId() : null;
        return ResponseEntity.ok(hrDashboardService.getAnnouncements(userId));
    }

    @PostMapping("/announcements")
    @PreAuthorize("hasAnyRole('HR_SPECIALIST', 'ADMIN')")
    public ResponseEntity<HrAnnouncementDto> createAnnouncement(
            @Valid @RequestBody CreateAnnouncementRequest req,
            Authentication auth) {
        User user = getAuthenticatedUser(auth);
        return ResponseEntity.status(HttpStatus.CREATED).body(hrDashboardService.createAnnouncement(req, user.getId()));
    }

    @PostMapping("/announcements/{id}/read")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> markAnnouncementRead(@PathVariable Long id, Authentication auth) {
        User user = getAuthenticatedUser(auth);
        hrDashboardService.markAnnouncementRead(id, user.getId());
        return ResponseEntity.ok().build();
    }
}
