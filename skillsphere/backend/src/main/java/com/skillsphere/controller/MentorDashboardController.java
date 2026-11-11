package com.skillsphere.controller;

import com.skillsphere.dto.MentorDtos.*;
import com.skillsphere.dto.MentorshipRequestResponseDto;
import com.skillsphere.dto.NotificationDto;
import com.skillsphere.dto.RespondMentorshipRequestDto;
import com.skillsphere.service.MentorDashboardService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/mentor")
@PreAuthorize("hasAnyRole('MENTOR', 'ADMIN')")
public class MentorDashboardController {

    private final MentorDashboardService mentorDashboardService;

    public MentorDashboardController(MentorDashboardService mentorDashboardService) {
        this.mentorDashboardService = mentorDashboardService;
    }

    // -------------------------------------------------------------------------
    // 1. Dashboard Overview
    // -------------------------------------------------------------------------

    @GetMapping("/overview")
    public MentorDashboardOverviewDto getOverview(Authentication auth) {
        return mentorDashboardService.getOverview(auth.getName());
    }

    // -------------------------------------------------------------------------
    // 2. Profile & Availability
    // -------------------------------------------------------------------------

    @GetMapping("/profile")
    public MentorProfileDto getProfile(Authentication auth) {
        return mentorDashboardService.getProfile(auth.getName());
    }

    @PutMapping("/profile")
    public MentorProfileDto updateProfile(@Valid @RequestBody UpdateMentorProfileRequest req, Authentication auth) {
        return mentorDashboardService.updateProfile(auth.getName(), req);
    }

    // -------------------------------------------------------------------------
    // 3. Mentorship Requests
    // -------------------------------------------------------------------------

    @GetMapping("/requests")
    public List<MentorshipRequestResponseDto> getRequests(
            @RequestParam(required = false) String status,
            Authentication auth) {
        return mentorDashboardService.getRequests(auth.getName(), status);
    }

    @PutMapping("/requests/{requestId}/respond")
    public MentorshipRequestResponseDto respondToRequest(
            @PathVariable Long requestId,
            @Valid @RequestBody RespondMentorshipRequestDto req,
            Authentication auth) {
        return mentorDashboardService.respondToRequest(auth.getName(), requestId, req);
    }

    // -------------------------------------------------------------------------
    // 4. My Mentees & Dossier
    // -------------------------------------------------------------------------

    @GetMapping("/mentees")
    public List<MenteeOverviewItemDto> getMentees(
            @RequestParam(required = false) String status,
            Authentication auth) {
        return mentorDashboardService.getMentees(auth.getName(), status);
    }

    @GetMapping("/mentees/assignments/{assignmentId}/dossier")
    public MenteeDossierDto getMenteeDossier(
            @PathVariable Long assignmentId,
            Authentication auth) {
        return mentorDashboardService.getMenteeDossier(auth.getName(), assignmentId);
    }

    @PostMapping("/mentees/assignments/{assignmentId}/end")
    public MenteeOverviewItemDto endMentorship(
            @PathVariable Long assignmentId,
            @RequestBody(required = false) EndMentorshipRequest req,
            Authentication auth) {
        return mentorDashboardService.endMentorship(auth.getName(), assignmentId, req);
    }

    // -------------------------------------------------------------------------
    // 5. Mentee Skill Overview (Consent Aware, Read-Only)
    // -------------------------------------------------------------------------

    @GetMapping("/mentees/{menteeId}/skills")
    public MenteeSkillDetailDto getMenteeSkills(
            @PathVariable Long menteeId,
            Authentication auth) {
        return mentorDashboardService.getMenteeSkills(auth.getName(), menteeId);
    }

    // -------------------------------------------------------------------------
    // 6. Development Goals & Action Items
    // -------------------------------------------------------------------------

    @GetMapping("/goals")
    public List<MentorGoalDetailDto> getGoals(
            @RequestParam(required = false) Long menteeId,
            @RequestParam(required = false) String status,
            Authentication auth) {
        return mentorDashboardService.getGoals(auth.getName(), menteeId, status);
    }

    @PostMapping("/goals")
    @ResponseStatus(HttpStatus.CREATED)
    public MentorGoalDetailDto createGoal(
            @Valid @RequestBody CreateMentorGoalRequest req,
            Authentication auth) {
        return mentorDashboardService.createGoal(auth.getName(), req);
    }

    @PatchMapping("/goals/{goalId}")
    public MentorGoalDetailDto updateGoal(
            @PathVariable Long goalId,
            @Valid @RequestBody UpdateMentorGoalRequest req,
            Authentication auth) {
        return mentorDashboardService.updateGoal(auth.getName(), goalId, req);
    }

    @PostMapping("/goals/{goalId}/action-items")
    @ResponseStatus(HttpStatus.CREATED)
    public GoalActionItemDto createGoalActionItem(
            @PathVariable Long goalId,
            @Valid @RequestBody CreateGoalActionItemRequest req,
            Authentication auth) {
        return mentorDashboardService.createGoalActionItem(auth.getName(), goalId, req);
    }

    @PatchMapping("/goals/action-items/{actionItemId}/toggle")
    public GoalActionItemDto toggleGoalActionItem(
            @PathVariable Long actionItemId,
            Authentication auth) {
        return mentorDashboardService.toggleGoalActionItem(auth.getName(), actionItemId);
    }

    @DeleteMapping("/goals/action-items/{actionItemId}")
    public ResponseEntity<Void> deleteGoalActionItem(
            @PathVariable Long actionItemId,
            Authentication auth) {
        mentorDashboardService.deleteGoalActionItem(auth.getName(), actionItemId);
        return ResponseEntity.noContent().build();
    }

    // -------------------------------------------------------------------------
    // 7. Mentoring Sessions & Notes
    // -------------------------------------------------------------------------

    @GetMapping("/sessions")
    public List<MentorSessionSummaryDto> getSessions(
            @RequestParam(required = false) Long menteeId,
            @RequestParam(required = false) String status,
            Authentication auth) {
        return mentorDashboardService.getSessions(auth.getName(), menteeId, status);
    }

    @PostMapping("/sessions")
    @ResponseStatus(HttpStatus.CREATED)
    public MentorSessionSummaryDto createSession(
            @Valid @RequestBody CreateMentorSessionRequest req,
            Authentication auth) {
        return mentorDashboardService.createSession(auth.getName(), req);
    }

    @PatchMapping("/sessions/{sessionId}/status")
    public MentorSessionSummaryDto updateSessionStatus(
            @PathVariable Long sessionId,
            @RequestBody Map<String, String> body,
            Authentication auth) {
        String status = body.get("status");
        return mentorDashboardService.updateSessionStatus(auth.getName(), sessionId, status);
    }

    @PatchMapping("/sessions/{sessionId}/notes")
    public MentorSessionSummaryDto updateSessionNotes(
            @PathVariable Long sessionId,
            @Valid @RequestBody UpdateSessionNotesRequest req,
            Authentication auth) {
        return mentorDashboardService.updateSessionNotes(auth.getName(), sessionId, req);
    }

    // -------------------------------------------------------------------------
    // 8. Shared Resources
    // -------------------------------------------------------------------------

    @GetMapping("/resources")
    public List<MentorSharedResourceDto> getResources(
            @RequestParam(required = false) Long menteeId,
            Authentication auth) {
        return mentorDashboardService.getResources(auth.getName(), menteeId);
    }

    @PostMapping("/resources")
    @ResponseStatus(HttpStatus.CREATED)
    public MentorSharedResourceDto createResource(
            @Valid @RequestBody CreateMentorSharedResourceRequest req,
            Authentication auth) {
        return mentorDashboardService.createResource(auth.getName(), req);
    }

    @DeleteMapping("/resources/{resourceId}")
    public ResponseEntity<Void> deleteResource(
            @PathVariable Long resourceId,
            Authentication auth) {
        mentorDashboardService.deleteResource(auth.getName(), resourceId);
        return ResponseEntity.noContent().build();
    }

    // -------------------------------------------------------------------------
    // 9. Knowledge Contributions
    // -------------------------------------------------------------------------

    @GetMapping("/contributions")
    public List<MentorArticleDto> getArticles(Authentication auth) {
        return mentorDashboardService.getArticles(auth.getName());
    }

    @PostMapping("/contributions")
    @ResponseStatus(HttpStatus.CREATED)
    public MentorArticleDto createArticle(
            @Valid @RequestBody CreateOrUpdateMentorArticleRequest req,
            Authentication auth) {
        return mentorDashboardService.createArticle(auth.getName(), req);
    }

    @PutMapping("/contributions/{articleId}")
    public MentorArticleDto updateArticle(
            @PathVariable Long articleId,
            @Valid @RequestBody CreateOrUpdateMentorArticleRequest req,
            Authentication auth) {
        return mentorDashboardService.updateArticle(auth.getName(), articleId, req);
    }

    @DeleteMapping("/contributions/{articleId}")
    public ResponseEntity<Void> deleteArticle(
            @PathVariable Long articleId,
            Authentication auth) {
        mentorDashboardService.deleteArticle(auth.getName(), articleId);
        return ResponseEntity.noContent().build();
    }

    // -------------------------------------------------------------------------
    // 10. Feedback & Evaluations
    // -------------------------------------------------------------------------

    @GetMapping("/feedback")
    public MentorFeedbackOverviewDto getFeedbackOverview(Authentication auth) {
        return mentorDashboardService.getFeedbackOverview(auth.getName());
    }

    @PostMapping("/feedback/evaluations")
    @ResponseStatus(HttpStatus.CREATED)
    public MentorEvaluationDto createEvaluation(
            @Valid @RequestBody CreateMentorEvaluationRequest req,
            Authentication auth) {
        return mentorDashboardService.createEvaluation(auth.getName(), req);
    }

    // -------------------------------------------------------------------------
    // 11. Notifications
    // -------------------------------------------------------------------------

    @GetMapping("/notifications")
    public List<NotificationDto> getNotifications(
            @RequestParam(required = false) Boolean unreadOnly,
            Authentication auth) {
        return mentorDashboardService.getNotifications(auth.getName(), unreadOnly);
    }

    @PatchMapping("/notifications/{notificationId}/read")
    public ResponseEntity<Void> markNotificationAsRead(
            @PathVariable Long notificationId,
            Authentication auth) {
        mentorDashboardService.markNotificationAsRead(auth.getName(), notificationId);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/notifications/read-all")
    public ResponseEntity<Void> markAllNotificationsAsRead(Authentication auth) {
        mentorDashboardService.markAllNotificationsAsRead(auth.getName());
        return ResponseEntity.noContent().build();
    }
}
