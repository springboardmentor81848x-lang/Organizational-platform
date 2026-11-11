package com.skillsphere.service;

import com.skillsphere.dto.*;
import com.skillsphere.entity.*;
import com.skillsphere.exception.ApiException;
import com.skillsphere.repository.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;

@Service
public class MentorshipService {

    private final UserRepository userRepository;
    private final EmployeeProfileRepository profileRepository;
    private final MentorProfileRepository mentorProfileRepository;
    private final MentorAssignmentRepository assignmentRepository;
    private final MentorshipSessionRepository sessionRepository;
    private final MentorshipGoalRepository goalRepository;
    private final MentorshipFeedbackRepository feedbackRepository;

    public MentorshipService(UserRepository userRepository,
                             EmployeeProfileRepository profileRepository,
                             MentorProfileRepository mentorProfileRepository,
                             MentorAssignmentRepository assignmentRepository,
                             MentorshipSessionRepository sessionRepository,
                             MentorshipGoalRepository goalRepository,
                             MentorshipFeedbackRepository feedbackRepository) {
        this.userRepository = userRepository;
        this.profileRepository = profileRepository;
        this.mentorProfileRepository = mentorProfileRepository;
        this.assignmentRepository = assignmentRepository;
        this.sessionRepository = sessionRepository;
        this.goalRepository = goalRepository;
        this.feedbackRepository = feedbackRepository;
    }

    // -------------------------------------------------------------
    // Expert Directory
    // -------------------------------------------------------------

    @Transactional(readOnly = true)
    public List<MentorDirectoryItem> getExpertDirectory(String query, String department, String expertise) {
        List<MentorProfile> activeMentors = mentorProfileRepository.findActiveMentors();
        List<MentorDirectoryItem> result = new ArrayList<>();

        String q = query == null ? "" : query.trim().toLowerCase();
        String deptFilter = department == null ? "" : department.trim().toLowerCase();
        String expFilter = expertise == null ? "" : expertise.trim().toLowerCase();

        for (MentorProfile mp : activeMentors) {
            User mentorUser = mp.getUser();
            EmployeeProfile ep = profileRepository.findByUserId(mentorUser.getId()).orElse(null);

            String dept = ep != null && ep.getDepartment() != null ? ep.getDepartment() : "General";
            String jobRole = ep != null && ep.getJobRole() != null ? ep.getJobRole() : "Professional";
            String expLevel = ep != null && ep.getExperienceLevel() != null ? ep.getExperienceLevel() : "Experienced";
            String bio = ep != null ? ep.getBio() : "";

            List<String> expertiseList = splitList(mp.getExpertiseAreas());
            long activeCount = assignmentRepository.countByMentorIdAndStatus(mentorUser.getId(), AssignmentStatus.ACTIVE);
            boolean isAvailable = activeCount < mp.getMaxMentees();

            // Filter checks
            if (!q.isEmpty()) {
                boolean matchName = mentorUser.getFullName().toLowerCase().contains(q);
                boolean matchHeadline = mp.getHeadline().toLowerCase().contains(q);
                boolean matchExpertise = expertiseList.stream().anyMatch(e -> e.toLowerCase().contains(q));
                if (!matchName && !matchHeadline && !matchExpertise) {
                    continue;
                }
            }
            if (!deptFilter.isEmpty() && !dept.toLowerCase().contains(deptFilter)) {
                continue;
            }
            if (!expFilter.isEmpty() && expertiseList.stream().noneMatch(e -> e.toLowerCase().contains(expFilter))) {
                continue;
            }

            result.add(new MentorDirectoryItem(
                    mp.getId(),
                    mentorUser.getId(),
                    mentorUser.getFullName(),
                    mentorUser.getEmail(),
                    mp.getHeadline(),
                    dept,
                    jobRole,
                    expLevel,
                    expertiseList,
                    bio,
                    mp.getMaxMentees(),
                    (int) activeCount,
                    isAvailable
            ));
        }

        return result;
    }

    // -------------------------------------------------------------
    // Employee Mentorship
    // -------------------------------------------------------------

    @Transactional(readOnly = true)
    public MyMentorResponse getMyMentor(String employeeEmail) {
        User user = getUserByEmail(employeeEmail);
        return assignmentRepository.findByEmployeeIdAndStatus(user.getId(), AssignmentStatus.ACTIVE)
                .map(this::toMyMentorResponse)
                .orElse(null);
    }

    @Transactional(readOnly = true)
    public List<SessionResponse> getMySessions(String employeeEmail) {
        User user = getUserByEmail(employeeEmail);
        MentorAssignment assignment = assignmentRepository.findByEmployeeIdAndStatus(user.getId(), AssignmentStatus.ACTIVE)
                .orElse(null);
        if (assignment == null) {
            return List.of();
        }
        return sessionRepository.findByAssignmentIdOrderByScheduledAtDesc(assignment.getId())
                .stream()
                .map(this::toSessionResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<GoalResponse> getMyGoals(String employeeEmail) {
        User user = getUserByEmail(employeeEmail);
        MentorAssignment assignment = assignmentRepository.findByEmployeeIdAndStatus(user.getId(), AssignmentStatus.ACTIVE)
                .orElse(null);
        if (assignment == null) {
            return List.of();
        }
        return goalRepository.findByAssignmentIdOrderByCreatedAtDesc(assignment.getId())
                .stream()
                .map(this::toGoalResponse)
                .toList();
    }

    @Transactional
    public FeedbackResponse submitFeedback(String userEmail, Long sessionId, CreateFeedbackRequest req) {
        User user = getUserByEmail(userEmail);
        MentorshipSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Session not found."));

        if (session.getStatus() != SessionStatus.COMPLETED) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Feedback can only be submitted for completed sessions.");
        }

        MentorAssignment assignment = session.getAssignment();
        boolean isParticipant = (assignment.getEmployee() != null && assignment.getEmployee().getId() != null && assignment.getEmployee().getId().equals(user.getId()))
                || (assignment.getMentor() != null && assignment.getMentor().getId() != null && assignment.getMentor().getId().equals(user.getId()));
        if (!isParticipant) {
            throw new ApiException(HttpStatus.FORBIDDEN, "You are not a participant in this mentorship session.");
        }

        if (feedbackRepository.existsBySessionId(sessionId)) {
            throw new ApiException(HttpStatus.CONFLICT, "Feedback has already been submitted for this session.");
        }

        MentorshipFeedback feedback = new MentorshipFeedback(session, user, req.rating(), req.comments().trim());
        MentorshipFeedback saved = feedbackRepository.save(feedback);
        return toFeedbackResponse(saved);
    }

    // -------------------------------------------------------------
    // Mentor Workspace
    // -------------------------------------------------------------

    @Transactional(readOnly = true)
    public Map<String, Object> getMentorStatus(String userEmail) {
        User user = getUserByEmail(userEmail);
        Optional<MentorProfile> mp = mentorProfileRepository.findByUserId(user.getId());
        if (mp.isEmpty()) {
            return Map.of("isApprovedMentor", false);
        }
        MentorProfile profile = mp.get();
        long activeMentees = assignmentRepository.countByMentorIdAndStatus(user.getId(), AssignmentStatus.ACTIVE);
        return Map.of(
                "isApprovedMentor", true,
                "isActive", profile.isActive(),
                "headline", profile.getHeadline(),
                "expertiseAreas", splitList(profile.getExpertiseAreas()),
                "maxMentees", profile.getMaxMentees(),
                "activeMenteesCount", activeMentees
        );
    }

    @Transactional(readOnly = true)
    public List<MenteeSummaryDto> getMyMentees(String mentorEmail) {
        User mentor = getUserByEmail(mentorEmail);
        assertActiveMentor(mentor.getId());

        List<MentorAssignment> assignments = assignmentRepository.findByMentorIdAndStatus(mentor.getId(), AssignmentStatus.ACTIVE);
        List<MenteeSummaryDto> list = new ArrayList<>();
        for (MentorAssignment a : assignments) {
            User emp = a.getEmployee();
            EmployeeProfile ep = profileRepository.findByUserId(emp.getId()).orElse(null);
            long goalsCount = goalRepository.findByAssignmentIdOrderByCreatedAtDesc(a.getId()).size();
            long sessionsCount = sessionRepository.findByAssignmentIdOrderByScheduledAtDesc(a.getId()).size();

            list.add(new MenteeSummaryDto(
                    a.getId(),
                    emp.getId(),
                    emp.getFullName(),
                    emp.getEmail(),
                    emp.getEmployeeId(),
                    ep != null ? ep.getJobRole() : "Software Developer",
                    ep != null ? ep.getDepartment() : null,
                    ep != null ? ep.getExperienceLevel() : "Entry level",
                    ep != null ? ep.getBio() : null,
                    a.getNotes(),
                    a.getAssignedAt(),
                    goalsCount,
                    sessionsCount
            ));
        }
        return list;
    }

    @Transactional(readOnly = true)
    public MenteeProgressResponse getMenteeProgress(String mentorEmail, Long employeeId) {
        User mentor = getUserByEmail(mentorEmail);
        assertActiveMentor(mentor.getId());

        MentorAssignment assignment = assignmentRepository.findByEmployeeIdAndStatus(employeeId, AssignmentStatus.ACTIVE)
                .filter(a -> a.getMentor().getId().equals(mentor.getId()))
                .orElseThrow(() -> new ApiException(HttpStatus.FORBIDDEN,
                        "You do not have an active mentorship assignment with this employee."));

        User emp = assignment.getEmployee();
        EmployeeProfile ep = profileRepository.findByUserId(emp.getId()).orElse(null);

        List<GoalResponse> goals = goalRepository.findByAssignmentIdOrderByCreatedAtDesc(assignment.getId())
                .stream().map(this::toGoalResponse).toList();
        List<SessionResponse> sessions = sessionRepository.findByAssignmentIdOrderByScheduledAtDesc(assignment.getId())
                .stream().map(this::toSessionResponse).toList();

        MenteeSummaryDto menteeDto = new MenteeSummaryDto(
                assignment.getId(),
                emp.getId(),
                emp.getFullName(),
                emp.getEmail(),
                emp.getEmployeeId(),
                ep != null ? ep.getJobRole() : "Software Developer",
                ep != null ? ep.getDepartment() : null,
                ep != null ? ep.getExperienceLevel() : "Entry level",
                ep != null ? ep.getBio() : null,
                assignment.getNotes(),
                assignment.getAssignedAt(),
                goals.size(),
                sessions.size()
        );

        return new MenteeProgressResponse(menteeDto, goals, sessions);
    }

    @Transactional
    public SessionResponse createSession(String mentorEmail, CreateSessionRequest req) {
        User mentor = getUserByEmail(mentorEmail);
        assertActiveMentor(mentor.getId());

        MentorAssignment assignment = assignmentRepository.findById(req.assignmentId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Assignment not found."));

        if (!assignment.getMentor().getId().equals(mentor.getId())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "You can only schedule sessions for your own mentees.");
        }
        if (assignment.getStatus() != AssignmentStatus.ACTIVE) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Cannot schedule sessions for inactive assignments.");
        }

        MentorshipSession session = new MentorshipSession(
                assignment,
                req.title().trim(),
                req.description() == null ? null : req.description().trim(),
                req.scheduledAt(),
                req.durationMinutes(),
                req.meetingLink() == null || req.meetingLink().isBlank() ? null : req.meetingLink().trim()
        );
        MentorshipSession saved = sessionRepository.save(session);
        return toSessionResponse(saved);
    }

    @Transactional
    public SessionResponse updateSessionStatus(String userEmail, Long sessionId, UpdateSessionStatusRequest req) {
        User user = getUserByEmail(userEmail);
        MentorshipSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Session not found."));

        MentorAssignment a = session.getAssignment();
        boolean isParticipant = (a.getMentor() != null && a.getMentor().getId() != null && a.getMentor().getId().equals(user.getId()))
                || (a.getEmployee() != null && a.getEmployee().getId() != null && a.getEmployee().getId().equals(user.getId()));
        if (!isParticipant) {
            throw new ApiException(HttpStatus.FORBIDDEN, "You do not have permission to update this session.");
        }

        session.setStatus(req.status());
        MentorshipSession saved = sessionRepository.save(session);
        return toSessionResponse(saved);
    }

    @Transactional
    public GoalResponse createGoal(String mentorEmail, CreateGoalRequest req) {
        User mentor = getUserByEmail(mentorEmail);
        assertActiveMentor(mentor.getId());

        MentorAssignment assignment = assignmentRepository.findById(req.assignmentId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Assignment not found."));

        if (!assignment.getMentor().getId().equals(mentor.getId())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "You can only create goals for your own mentees.");
        }
        if (assignment.getStatus() != AssignmentStatus.ACTIVE) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Cannot add goals to inactive assignments.");
        }

        MentorshipGoal goal = new MentorshipGoal(
                assignment,
                req.title().trim(),
                req.description() == null ? null : req.description().trim(),
                req.targetDate()
        );
        MentorshipGoal saved = goalRepository.save(goal);
        return toGoalResponse(saved);
    }

    @Transactional
    public GoalResponse updateGoalStatus(String userEmail, Long goalId, UpdateGoalStatusRequest req) {
        User user = getUserByEmail(userEmail);
        MentorshipGoal goal = goalRepository.findById(goalId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Goal not found."));

        MentorAssignment a = goal.getAssignment();
        boolean isParticipant = (a.getMentor() != null && a.getMentor().getId() != null && a.getMentor().getId().equals(user.getId()))
                || (a.getEmployee() != null && a.getEmployee().getId() != null && a.getEmployee().getId().equals(user.getId()));
        if (!isParticipant) {
            throw new ApiException(HttpStatus.FORBIDDEN, "You do not have permission to update this goal.");
        }

        goal.setStatus(req.status());
        MentorshipGoal saved = goalRepository.save(goal);
        return toGoalResponse(saved);
    }

    // -------------------------------------------------------------
    // Helpers & Mappers
    // -------------------------------------------------------------

    private User getUserByEmail(String email) {
        return userRepository.findByEmail(email.trim().toLowerCase())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "User not found."));
    }

    private void assertActiveMentor(Long userId) {
        MentorProfile mp = mentorProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new ApiException(HttpStatus.FORBIDDEN, "You are not an approved mentor."));
        if (!mp.isActive()) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Your mentor profile is currently inactive.");
        }
    }

    private MyMentorResponse toMyMentorResponse(MentorAssignment a) {
        User mentor = a.getMentor();
        MentorProfile mp = mentorProfileRepository.findByUserId(mentor.getId()).orElse(null);
        EmployeeProfile ep = profileRepository.findByUserId(mentor.getId()).orElse(null);

        return new MyMentorResponse(
                a.getId(),
                mentor.getId(),
                mentor.getFullName(),
                mentor.getEmail(),
                mp != null ? mp.getHeadline() : "Senior Professional",
                ep != null ? ep.getDepartment() : "General",
                ep != null ? ep.getJobRole() : "Mentor",
                mp != null ? splitList(mp.getExpertiseAreas()) : List.of(),
                a.getNotes(),
                a.getStatus().name(),
                a.getAssignedAt()
        );
    }

    private SessionResponse toSessionResponse(MentorshipSession s) {
        FeedbackResponse feedback = feedbackRepository.findBySessionId(s.getId())
                .map(this::toFeedbackResponse)
                .orElse(null);

        return new SessionResponse(
                s.getId(),
                s.getAssignment().getId(),
                s.getTitle(),
                s.getDescription(),
                s.getScheduledAt(),
                s.getDurationMinutes(),
                s.getMeetingLink(),
                s.getStatus().name(),
                s.getCreatedAt(),
                feedback
        );
    }

    private GoalResponse toGoalResponse(MentorshipGoal g) {
        return new GoalResponse(
                g.getId(),
                g.getAssignment().getId(),
                g.getTitle(),
                g.getDescription(),
                g.getTargetDate(),
                g.getStatus().name(),
                g.getCreatedAt(),
                g.getUpdatedAt()
        );
    }

    private FeedbackResponse toFeedbackResponse(MentorshipFeedback f) {
        return new FeedbackResponse(
                f.getId(),
                f.getSession().getId(),
                f.getAuthor().getId(),
                f.getAuthor().getFullName(),
                f.getRating(),
                f.getComments(),
                f.getCreatedAt()
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
