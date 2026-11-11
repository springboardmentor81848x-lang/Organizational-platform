package com.skillsphere.controller;

import com.skillsphere.dto.*;
import com.skillsphere.service.AdminMentorshipService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/mentorship")
@PreAuthorize("hasRole('ADMIN')")
public class AdminMentorshipController {

    private final AdminMentorshipService adminMentorshipService;

    public AdminMentorshipController(AdminMentorshipService adminMentorshipService) {
        this.adminMentorshipService = adminMentorshipService;
    }

    @GetMapping("/mentors")
    public List<AdminMentorDto> listMentors() {
        return adminMentorshipService.listMentors();
    }

    @GetMapping("/eligible-employees")
    public List<EligibleEmployeeDto> listEligibleEmployees() {
        return adminMentorshipService.listEligibleEmployees();
    }

    @PostMapping("/mentors/{userId}/approve")
    @ResponseStatus(HttpStatus.CREATED)
    public AdminMentorDto approveMentor(
            @PathVariable Long userId,
            @Valid @RequestBody ApproveMentorRequest request,
            Authentication auth) {
        return adminMentorshipService.approveMentor(userId, request, auth.getName());
    }

    @PatchMapping("/mentors/{userId}/status")
    public AdminMentorDto updateMentorStatus(
            @PathVariable Long userId,
            @Valid @RequestBody UpdateMentorStatusRequest request,
            Authentication auth) {
        return adminMentorshipService.updateMentorStatus(userId, request, auth.getName());
    }

    @GetMapping("/assignments")
    public List<AdminAssignmentDto> listAssignments() {
        return adminMentorshipService.listAssignments();
    }

    @GetMapping("/unassigned-employees")
    public List<EligibleEmployeeDto> listUnassignedEmployees() {
        return adminMentorshipService.listUnassignedEmployees();
    }

    @PostMapping("/assign")
    @ResponseStatus(HttpStatus.CREATED)
    public AdminAssignmentDto assignMentor(
            @Valid @RequestBody CreateAssignmentRequest request,
            Authentication auth) {
        return adminMentorshipService.createAssignment(request, auth.getName());
    }

    @PatchMapping("/assignments/{assignmentId}/end")
    public AdminAssignmentDto endAssignment(
            @PathVariable Long assignmentId,
            @Valid @RequestBody EndAssignmentRequest request,
            Authentication auth) {
        return adminMentorshipService.endAssignment(assignmentId, request, auth.getName());
    }
}
