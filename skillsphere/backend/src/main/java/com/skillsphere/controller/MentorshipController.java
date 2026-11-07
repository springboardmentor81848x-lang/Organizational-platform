package com.skillsphere.controller;

import com.skillsphere.dto.*;
import com.skillsphere.service.MentorshipService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/mentorship")
public class MentorshipController {

    private final MentorshipService mentorshipService;
    private final com.skillsphere.service.MentorshipRequestService mentorshipRequestService;

    public MentorshipController(MentorshipService mentorshipService,
                                com.skillsphere.service.MentorshipRequestService mentorshipRequestService) {
        this.mentorshipService = mentorshipService;
        this.mentorshipRequestService = mentorshipRequestService;
    }

    // -------------------------------------------------------------
    // Expert Directory
    // -------------------------------------------------------------

    @GetMapping("/experts")
    public List<MentorDirectoryItem> getExperts(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) String department,
            @RequestParam(required = false) String expertise) {
        return mentorshipService.getExpertDirectory(query, department, expertise);
    }

    // -------------------------------------------------------------
    // Employee Mentorship
    // -------------------------------------------------------------

    @GetMapping("/my-mentor")
    public ResponseEntity<MyMentorResponse> getMyMentor(Authentication auth) {
        MyMentorResponse res = mentorshipService.getMyMentor(auth.getName());
        return res != null ? ResponseEntity.ok(res) : ResponseEntity.noContent().build();
    }

    @GetMapping("/my-sessions")
    public List<SessionResponse> getMySessions(Authentication auth) {
        return mentorshipService.getMySessions(auth.getName());
    }

    @GetMapping("/my-goals")
    public List<GoalResponse> getMyGoals(Authentication auth) {
        return mentorshipService.getMyGoals(auth.getName());
    }

    @PostMapping("/sessions/{sessionId}/feedback")
    @ResponseStatus(HttpStatus.CREATED)
    public FeedbackResponse submitFeedback(
            @PathVariable Long sessionId,
            @Valid @RequestBody CreateFeedbackRequest request,
            Authentication auth) {
        return mentorshipService.submitFeedback(auth.getName(), sessionId, request);
    }

    // -------------------------------------------------------------
    // Mentor Workspace
    // -------------------------------------------------------------

    @GetMapping("/mentor/status")
    public Map<String, Object> getMentorStatus(Authentication auth) {
        return mentorshipService.getMentorStatus(auth.getName());
    }

    @GetMapping("/mentor/mentees")
    public List<MenteeSummaryDto> getMyMentees(Authentication auth) {
        return mentorshipService.getMyMentees(auth.getName());
    }

    @GetMapping("/mentor/mentees/{employeeId}/progress")
    public MenteeProgressResponse getMenteeProgress(@PathVariable Long employeeId, Authentication auth) {
        return mentorshipService.getMenteeProgress(auth.getName(), employeeId);
    }

    @PostMapping("/sessions")
    @ResponseStatus(HttpStatus.CREATED)
    public SessionResponse createSession(@Valid @RequestBody CreateSessionRequest request, Authentication auth) {
        return mentorshipService.createSession(auth.getName(), request);
    }

    @PatchMapping("/sessions/{sessionId}/status")
    public SessionResponse updateSessionStatus(
            @PathVariable Long sessionId,
            @Valid @RequestBody UpdateSessionStatusRequest request,
            Authentication auth) {
        return mentorshipService.updateSessionStatus(auth.getName(), sessionId, request);
    }

    @PostMapping("/goals")
    @ResponseStatus(HttpStatus.CREATED)
    public GoalResponse createGoal(@Valid @RequestBody CreateGoalRequest request, Authentication auth) {
        return mentorshipService.createGoal(auth.getName(), request);
    }

    @PatchMapping("/goals/{goalId}")
    public GoalResponse updateGoalStatus(
            @PathVariable Long goalId,
            @Valid @RequestBody UpdateGoalStatusRequest request,
            Authentication auth) {
        return mentorshipService.updateGoalStatus(auth.getName(), goalId, request);
    }

    // -------------------------------------------------------------
    // Mentorship Direct Requests & Shared Resources
    // -------------------------------------------------------------

    @PostMapping("/requests")
    @ResponseStatus(HttpStatus.CREATED)
    public MentorshipRequestResponseDto createMentorshipRequest(
            @Valid @RequestBody CreateMentorshipRequestDto request,
            Authentication auth) {
        return mentorshipRequestService.createRequest(auth.getName(), request);
    }

    @GetMapping("/requests/sent")
    public List<MentorshipRequestResponseDto> getMySentRequests(Authentication auth) {
        return mentorshipRequestService.getMySentRequests(auth.getName());
    }

    @GetMapping("/requests/incoming")
    public List<MentorshipRequestResponseDto> getMyIncomingRequests(Authentication auth) {
        return mentorshipRequestService.getMyIncomingRequests(auth.getName());
    }

    @PutMapping("/requests/{requestId}/respond")
    public MentorshipRequestResponseDto respondToRequest(
            @PathVariable Long requestId,
            @Valid @RequestBody RespondMentorshipRequestDto request,
            Authentication auth) {
        return mentorshipRequestService.respondToRequest(auth.getName(), requestId, request);
    }

    @PostMapping("/resources")
    @ResponseStatus(HttpStatus.CREATED)
    public SharedResourceDto shareResource(
            @Valid @RequestBody SharedResourceDto.CreateRequest request,
            Authentication auth) {
        return mentorshipRequestService.shareResource(auth.getName(), request);
    }

    @GetMapping("/assignments/{assignmentId}/resources")
    public List<SharedResourceDto> getSharedResources(
            @PathVariable Long assignmentId,
            Authentication auth) {
        return mentorshipRequestService.getResourcesForAssignment(auth.getName(), assignmentId);
    }
}
