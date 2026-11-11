package com.skillsphere.controller;

import com.skillsphere.dto.*;
import com.skillsphere.service.AssessmentService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/assessments")
public class AssessmentController {

    private final AssessmentService assessmentService;

    public AssessmentController(AssessmentService assessmentService) {
        this.assessmentService = assessmentService;
    }

    @GetMapping
    public ResponseEntity<List<AssessmentSummaryDto>> getAllAssessments() {
        return ResponseEntity.ok(assessmentService.getAllAssessments());
    }

    @GetMapping("/{id}")
    public ResponseEntity<AssessmentDetailDto> getAssessmentById(@PathVariable Long id) {
        return ResponseEntity.ok(assessmentService.getAssessmentById(id));
    }

    /* ---------------------------------------------------------------- Dynamic Assessment Endpoints */

    @PostMapping("/start")
    public ResponseEntity<AssessmentAttemptDto> startAssessment(@AuthenticationPrincipal UserDetails userDetails,
                                                                @Valid @RequestBody StartAssessmentRequest req) {
        AssessmentAttemptDto attempt = assessmentService.startAssessment(userDetails.getUsername(), req);
        return ResponseEntity.status(HttpStatus.CREATED).body(attempt);
    }

    @GetMapping("/ongoing")
    public ResponseEntity<AssessmentAttemptDto> getOngoingAttempt(@AuthenticationPrincipal UserDetails userDetails) {
        AssessmentAttemptDto ongoing = assessmentService.getOngoingAttempt(userDetails.getUsername());
        return ResponseEntity.ok(ongoing);
    }

    @GetMapping("/attempts/{attemptId}")
    public ResponseEntity<AssessmentAttemptDto> getAttemptById(@AuthenticationPrincipal UserDetails userDetails,
                                                               @PathVariable Long attemptId) {
        return ResponseEntity.ok(assessmentService.getAttemptById(userDetails.getUsername(), attemptId));
    }

    @PutMapping("/attempts/{attemptId}/save")
    public ResponseEntity<Void> saveAttemptAnswers(@AuthenticationPrincipal UserDetails userDetails,
                                                   @PathVariable Long attemptId,
                                                   @RequestBody SaveAnswersRequest req) {
        assessmentService.saveAttemptAnswers(userDetails.getUsername(), attemptId, req);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/attempts/{attemptId}/submit")
    public ResponseEntity<AssessmentResultDto> submitAttempt(@AuthenticationPrincipal UserDetails userDetails,
                                                             @PathVariable Long attemptId,
                                                             @Valid @RequestBody SubmitAssessmentRequest req) {
        AssessmentResultDto result = assessmentService.submitAttempt(userDetails.getUsername(), attemptId, req);
        return ResponseEntity.ok(result);
    }

    /* ---------------------------------------------------------------- Legacy & Existing Submissions */

    @PostMapping("/{id}/submit")
    public ResponseEntity<AssessmentResultDto> submitAssessment(@AuthenticationPrincipal UserDetails userDetails,
                                                                @PathVariable Long id,
                                                                @Valid @RequestBody SubmitAssessmentRequest req) {
        AssessmentResultDto result = assessmentService.submitAssessment(userDetails.getUsername(), id, req);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/my-submissions")
    public ResponseEntity<List<AssessmentResultDto>> getMySubmissions(@AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(assessmentService.getUserSubmissions(userDetails.getUsername()));
    }

    @GetMapping("/my-gaps")
    public ResponseEntity<List<SkillGapDto>> getMySkillGaps(@AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(assessmentService.getUserGaps(userDetails.getUsername()));
    }

    @GetMapping("/my-assignments")
    public ResponseEntity<List<AssessmentAssignmentDto>> getMyAssignments(@AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(assessmentService.getUserAssignments(userDetails.getUsername()));
    }

    @PostMapping("/assign")
    @PreAuthorize("hasAnyRole('MANAGER', 'HR_SPECIALIST', 'ADMIN')")
    public ResponseEntity<AssessmentAssignmentDto> assignAssessment(@AuthenticationPrincipal UserDetails userDetails,
                                                                    @Valid @RequestBody AssignAssessmentRequest req) {
        AssessmentAssignmentDto assignment = assessmentService.assignAssessment(userDetails.getUsername(), req);
        return ResponseEntity.status(HttpStatus.CREATED).body(assignment);
    }

    @GetMapping("/gaps/user/{userId}")
    @PreAuthorize("hasAnyRole('MANAGER', 'HR_SPECIALIST', 'ADMIN', 'MENTOR')")
    public ResponseEntity<List<SkillGapDto>> getUserSkillGaps(@AuthenticationPrincipal UserDetails userDetails,
                                                              @PathVariable Long userId) {
        return ResponseEntity.ok(assessmentService.getUserGapsByUserId(userDetails.getUsername(), userId));
    }

    @GetMapping("/gaps/team/{teamId}")
    @PreAuthorize("hasAnyRole('MANAGER', 'DEPARTMENT_HEAD', 'ADMIN')")
    public ResponseEntity<List<SkillGapDto>> getTeamSkillGaps(@AuthenticationPrincipal UserDetails userDetails,
                                                              @PathVariable Long teamId) {
        return ResponseEntity.ok(assessmentService.getTeamGaps(userDetails.getUsername(), teamId));
    }

    @GetMapping("/gaps/organization")
    @PreAuthorize("hasAnyRole('HR_SPECIALIST', 'DEPARTMENT_HEAD', 'ADMIN')")
    public ResponseEntity<List<SkillGapDto>> getOrgSkillGaps() {
        return ResponseEntity.ok(assessmentService.getAllOrgGaps());
    }
}
