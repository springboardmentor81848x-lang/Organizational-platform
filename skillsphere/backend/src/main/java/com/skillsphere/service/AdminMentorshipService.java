package com.skillsphere.service;

import com.skillsphere.dto.*;
import com.skillsphere.entity.*;
import com.skillsphere.exception.ApiException;
import com.skillsphere.repository.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Service
public class AdminMentorshipService {

    private final UserRepository userRepository;
    private final EmployeeProfileRepository profileRepository;
    private final MentorProfileRepository mentorProfileRepository;
    private final MentorAssignmentRepository assignmentRepository;

    public AdminMentorshipService(UserRepository userRepository,
                                  EmployeeProfileRepository profileRepository,
                                  MentorProfileRepository mentorProfileRepository,
                                  MentorAssignmentRepository assignmentRepository) {
        this.userRepository = userRepository;
        this.profileRepository = profileRepository;
        this.mentorProfileRepository = mentorProfileRepository;
        this.assignmentRepository = assignmentRepository;
    }

    @Transactional(readOnly = true)
    public List<AdminMentorDto> listMentors() {
        List<MentorProfile> list = mentorProfileRepository.findAllWithDetails();
        List<AdminMentorDto> dtos = new ArrayList<>();
        for (MentorProfile mp : list) {
            long activeCount = assignmentRepository.countByMentorIdAndStatus(mp.getUser().getId(), AssignmentStatus.ACTIVE);
            dtos.add(toAdminMentorDto(mp, (int) activeCount));
        }
        return dtos;
    }

    @Transactional(readOnly = true)
    public List<EligibleEmployeeDto> listEligibleEmployees() {
        List<User> users = userRepository.findAll();
        List<EligibleEmployeeDto> result = new ArrayList<>();
        for (User u : users) {
            if (u.getAccountStatus() != AccountStatus.ACTIVE) continue;
            boolean isMentor = mentorProfileRepository.existsByUserId(u.getId());
            boolean hasActiveMentor = assignmentRepository.existsByEmployeeIdAndStatus(u.getId(), AssignmentStatus.ACTIVE);
            EmployeeProfile ep = profileRepository.findByUserId(u.getId()).orElse(null);

            result.add(new EligibleEmployeeDto(
                    u.getId(),
                    u.getFullName(),
                    u.getEmail(),
                    u.getEmployeeId(),
                    ep != null ? ep.getJobRole() : "Software Developer",
                    ep != null ? ep.getDepartment() : null,
                    isMentor,
                    hasActiveMentor
            ));
        }
        return result;
    }

    @Transactional
    public AdminMentorDto approveMentor(Long userId, ApproveMentorRequest req, String actingAdminEmail) {
        User admin = userRepository.findByEmail(actingAdminEmail.trim().toLowerCase())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Admin user not found."));

        if (admin.getId().equals(userId)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Administrators cannot approve themselves as mentors.");
        }

        User target = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "User not found."));

        if (target.getAccountStatus() != AccountStatus.ACTIVE) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Inactive or suspended users cannot be approved as mentors.");
        }

        if (mentorProfileRepository.existsByUserId(userId)) {
            throw new ApiException(HttpStatus.CONFLICT, "User is already an approved mentor.");
        }

        MentorProfile mp = new MentorProfile(
                target,
                req.headline().trim(),
                req.expertiseAreas().trim(),
                req.maxMentees(),
                admin
        );
        MentorProfile saved = mentorProfileRepository.save(mp);
        return toAdminMentorDto(saved, 0);
    }

    @Transactional
    public AdminMentorDto updateMentorStatus(Long userId, UpdateMentorStatusRequest req, String actingAdminEmail) {
        MentorProfile mp = mentorProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Mentor profile not found."));

        mp.setActive(req.isActive());
        MentorProfile saved = mentorProfileRepository.save(mp);
        long activeCount = assignmentRepository.countByMentorIdAndStatus(userId, AssignmentStatus.ACTIVE);
        return toAdminMentorDto(saved, (int) activeCount);
    }

    @Transactional(readOnly = true)
    public List<AdminAssignmentDto> listAssignments() {
        return assignmentRepository.findAllWithUsers().stream()
                .map(this::toAdminAssignmentDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<EligibleEmployeeDto> listUnassignedEmployees() {
        List<User> users = userRepository.findAll();
        List<EligibleEmployeeDto> result = new ArrayList<>();
        for (User u : users) {
            if (u.getAccountStatus() != AccountStatus.ACTIVE) continue;
            boolean hasActiveMentor = assignmentRepository.existsByEmployeeIdAndStatus(u.getId(), AssignmentStatus.ACTIVE);
            if (hasActiveMentor) continue; // skip already assigned

            boolean isMentor = mentorProfileRepository.existsByUserId(u.getId());
            EmployeeProfile ep = profileRepository.findByUserId(u.getId()).orElse(null);

            result.add(new EligibleEmployeeDto(
                    u.getId(),
                    u.getFullName(),
                    u.getEmail(),
                    u.getEmployeeId(),
                    ep != null ? ep.getJobRole() : "Software Developer",
                    ep != null ? ep.getDepartment() : null,
                    isMentor,
                    false
            ));
        }
        return result;
    }

    @Transactional
    public AdminAssignmentDto createAssignment(CreateAssignmentRequest req, String actingAdminEmail) {
        User admin = userRepository.findByEmail(actingAdminEmail.trim().toLowerCase())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Admin user not found."));

        if (req.mentorUserId().equals(req.employeeUserId())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "A mentor cannot be assigned to mentor themselves.");
        }

        User mentor = userRepository.findById(req.mentorUserId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Mentor user not found."));

        MentorProfile mp = mentorProfileRepository.findByUserId(mentor.getId())
                .orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST, "Selected user is not an approved mentor."));

        if (!mp.isActive()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Selected mentor is currently inactive.");
        }

        long activeCount = assignmentRepository.countByMentorIdAndStatus(mentor.getId(), AssignmentStatus.ACTIVE);
        if (activeCount >= mp.getMaxMentees()) {
            throw new ApiException(HttpStatus.BAD_REQUEST,
                    "Mentor has reached their maximum capacity of " + mp.getMaxMentees() + " mentees.");
        }

        User employee = userRepository.findById(req.employeeUserId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Employee user not found."));

        if (assignmentRepository.existsByEmployeeIdAndStatus(employee.getId(), AssignmentStatus.ACTIVE)) {
            throw new ApiException(HttpStatus.CONFLICT, "This employee already has an active mentor assignment.");
        }

        MentorAssignment assignment = new MentorAssignment(
                mentor,
                employee,
                admin,
                req.notes() == null || req.notes().isBlank() ? null : req.notes().trim()
        );
        MentorAssignment saved = assignmentRepository.save(assignment);
        return toAdminAssignmentDto(saved);
    }

    @Transactional
    public AdminAssignmentDto endAssignment(Long assignmentId, EndAssignmentRequest req, String actingAdminEmail) {
        MentorAssignment assignment = assignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Assignment not found."));

        if (assignment.getStatus() != AssignmentStatus.ACTIVE) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Assignment is already " + assignment.getStatus().name() + ".");
        }

        assignment.setStatus(req.status());
        assignment.setEndedAt(Instant.now());
        if (req.notes() != null && !req.notes().isBlank()) {
            String combined = assignment.getNotes() == null ? "" : assignment.getNotes() + "\n";
            assignment.setNotes(combined + "[Ended note]: " + req.notes().trim());
        }

        MentorAssignment saved = assignmentRepository.save(assignment);
        return toAdminAssignmentDto(saved);
    }

    // -------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------

    private AdminMentorDto toAdminMentorDto(MentorProfile mp, int currentMentees) {
        User u = mp.getUser();
        User admin = mp.getApprovedBy();
        return new AdminMentorDto(
                mp.getId(),
                u.getId(),
                u.getFullName(),
                u.getEmail(),
                u.getEmployeeId(),
                mp.getHeadline(),
                splitList(mp.getExpertiseAreas()),
                mp.getMaxMentees(),
                currentMentees,
                mp.isActive(),
                admin != null ? admin.getFullName() : "Administrator",
                mp.getCreatedAt()
        );
    }

    private AdminAssignmentDto toAdminAssignmentDto(MentorAssignment a) {
        return new AdminAssignmentDto(
                a.getId(),
                a.getMentor().getId(),
                a.getMentor().getFullName(),
                a.getMentor().getEmail(),
                a.getEmployee().getId(),
                a.getEmployee().getFullName(),
                a.getEmployee().getEmail(),
                a.getAssignedBy() != null ? a.getAssignedBy().getFullName() : "Administrator",
                a.getStatus().name(),
                a.getNotes(),
                a.getAssignedAt(),
                a.getEndedAt()
        );
    }

    private List<String> splitList(String text) {
        if (text == null || text.isBlank()) return List.of();
        return Arrays.stream(text.split("[,;]"))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
    }
}
