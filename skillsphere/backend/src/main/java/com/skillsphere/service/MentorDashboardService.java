package com.skillsphere.service;

import com.skillsphere.dto.MentorDtos.*;
import com.skillsphere.dto.MentorshipRequestResponseDto;
import com.skillsphere.dto.NotificationDto;
import com.skillsphere.dto.RespondMentorshipRequestDto;
import com.skillsphere.entity.*;
import com.skillsphere.exception.ApiException;
import com.skillsphere.repository.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class MentorDashboardService {

    private final UserRepository userRepository;
    private final EmployeeProfileRepository employeeProfileRepository;
    private final MentorProfileRepository mentorProfileRepository;
    private final MentorAvailabilityRepository mentorAvailabilityRepository;
    private final MentorAssignmentRepository mentorAssignmentRepository;
    private final MentorshipRequestRepository mentorshipRequestRepository;
    private final MentorshipGoalRepository mentorshipGoalRepository;
    private final GoalActionItemRepository goalActionItemRepository;
    private final MentorshipSessionRepository mentorshipSessionRepository;
    private final MentorshipFeedbackRepository mentorshipFeedbackRepository;
    private final MentorEvaluationRepository mentorEvaluationRepository;
    private final MenteeSkillConsentRepository menteeSkillConsentRepository;
    private final SharedResourceRepository sharedResourceRepository;
    private final KnowledgeArticleRepository knowledgeArticleRepository;
    private final KnowledgeCategoryRepository knowledgeCategoryRepository;
    private final EmployeeSkillRepository employeeSkillRepository;
    private final SkillGapRepository skillGapRepository;
    private final AssessmentAttemptRepository assessmentAttemptRepository;
    private final SkillRepository skillRepository;
    private final NotificationRepository notificationRepository;
    private final NotificationService notificationService;

    public MentorDashboardService(UserRepository userRepository,
                                  EmployeeProfileRepository employeeProfileRepository,
                                  MentorProfileRepository mentorProfileRepository,
                                  MentorAvailabilityRepository mentorAvailabilityRepository,
                                  MentorAssignmentRepository mentorAssignmentRepository,
                                  MentorshipRequestRepository mentorshipRequestRepository,
                                  MentorshipGoalRepository mentorshipGoalRepository,
                                  GoalActionItemRepository goalActionItemRepository,
                                  MentorshipSessionRepository mentorshipSessionRepository,
                                  MentorshipFeedbackRepository mentorshipFeedbackRepository,
                                  MentorEvaluationRepository mentorEvaluationRepository,
                                  MenteeSkillConsentRepository menteeSkillConsentRepository,
                                  SharedResourceRepository sharedResourceRepository,
                                  KnowledgeArticleRepository knowledgeArticleRepository,
                                  KnowledgeCategoryRepository knowledgeCategoryRepository,
                                  EmployeeSkillRepository employeeSkillRepository,
                                  SkillGapRepository skillGapRepository,
                                  AssessmentAttemptRepository assessmentAttemptRepository,
                                  SkillRepository skillRepository,
                                  NotificationRepository notificationRepository,
                                  NotificationService notificationService) {
        this.userRepository = userRepository;
        this.employeeProfileRepository = employeeProfileRepository;
        this.mentorProfileRepository = mentorProfileRepository;
        this.mentorAvailabilityRepository = mentorAvailabilityRepository;
        this.mentorAssignmentRepository = mentorAssignmentRepository;
        this.mentorshipRequestRepository = mentorshipRequestRepository;
        this.mentorshipGoalRepository = mentorshipGoalRepository;
        this.goalActionItemRepository = goalActionItemRepository;
        this.mentorshipSessionRepository = mentorshipSessionRepository;
        this.mentorshipFeedbackRepository = mentorshipFeedbackRepository;
        this.mentorEvaluationRepository = mentorEvaluationRepository;
        this.menteeSkillConsentRepository = menteeSkillConsentRepository;
        this.sharedResourceRepository = sharedResourceRepository;
        this.knowledgeArticleRepository = knowledgeArticleRepository;
        this.knowledgeCategoryRepository = knowledgeCategoryRepository;
        this.employeeSkillRepository = employeeSkillRepository;
        this.skillGapRepository = skillGapRepository;
        this.assessmentAttemptRepository = assessmentAttemptRepository;
        this.skillRepository = skillRepository;
        this.notificationRepository = notificationRepository;
        this.notificationService = notificationService;
    }

    // -------------------------------------------------------------------------
    // Helper & Validation Methods
    // -------------------------------------------------------------------------

    private User getUserByEmail(String email) {
        return userRepository.findByEmail(email.trim().toLowerCase())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "User not found with email: " + email));
    }

    private MentorProfile getOrCreateMentorProfile(User mentor) {
        return mentorProfileRepository.findByUserId(mentor.getId())
                .orElseGet(() -> {
                    User approvedBy = userRepository.findAll().stream()
                            .filter(u -> u.getRole() == Role.ADMIN)
                            .findFirst()
                            .orElse(mentor);
                    MentorProfile mp = new MentorProfile(mentor, "Expert Mentor & Technical Advisor", "General Engineering", 5, approvedBy);
                    return mentorProfileRepository.save(mp);
                });
    }

    private MentorAssignment validateAssignmentOwnership(User mentor, Long assignmentId) {
        MentorAssignment a = mentorAssignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Mentorship assignment not found."));
        if (!a.getMentor().getId().equals(mentor.getId()) && mentor.getRole() != Role.ADMIN) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Access denied: You do not own this mentorship assignment.");
        }
        return a;
    }

    // -------------------------------------------------------------------------
    // 1. Dashboard Overview
    // -------------------------------------------------------------------------

    @Transactional
    public MentorDashboardOverviewDto getOverview(String mentorEmail) {
        User mentor = getUserByEmail(mentorEmail);
        MentorProfile mp = getOrCreateMentorProfile(mentor);
        MentorProfileDto profileDto = toProfileDto(mentor, mp);

        long activeCount = mentorAssignmentRepository.countByMentorIdAndStatus(mentor.getId(), AssignmentStatus.ACTIVE);
        long pendingRequestsCount = mentorshipRequestRepository.findByMentorIdOrderByCreatedAtDesc(mentor.getId())
                .stream().filter(r -> "PENDING".equalsIgnoreCase(r.getStatus())).count();
        long completedSessionsCount = mentorshipSessionRepository.countByAssignmentMentorIdAndStatus(mentor.getId(), SessionStatus.COMPLETED);

        Instant now = Instant.now();
        List<MentorshipSession> allMentorSessions = mentorshipSessionRepository.findByMentorIdOrderByScheduledAtDesc(mentor.getId());
        long upcomingSessionsCount = allMentorSessions.stream()
                .filter(s -> s.getStatus() == SessionStatus.SCHEDULED && s.getScheduledAt().isAfter(now))
                .count();

        long totalGoals = mentorshipGoalRepository.countTotalGoalsByMentorId(mentor.getId());
        long completedGoals = mentorshipGoalRepository.countCompletedGoalsByMentorId(mentor.getId());
        double goalCompletionRate = totalGoals > 0 ? ((double) completedGoals / totalGoals) * 100.0 : 0.0;

        long sharedResourcesCount = sharedResourceRepository.countByAssignmentMentorId(mentor.getId());
        long knowledgeArticlesCount = knowledgeArticleRepository.countByAuthorIdAndStatus(mentor.getId(), "PUBLISHED");

        // Top 5 upcoming sessions
        List<MentorSessionSummaryDto> upcomingSessions = allMentorSessions.stream()
                .filter(s -> s.getStatus() == SessionStatus.SCHEDULED && s.getScheduledAt().isAfter(now))
                .sorted(Comparator.comparing(MentorshipSession::getScheduledAt))
                .limit(5)
                .map(this::toSessionSummaryDto)
                .toList();

        // Top 5 pending requests
        List<MentorshipRequestResponseDto> pendingRequests = mentorshipRequestRepository.findByMentorIdOrderByCreatedAtDesc(mentor.getId())
                .stream()
                .filter(r -> "PENDING".equalsIgnoreCase(r.getStatus()))
                .limit(5)
                .map(this::toRequestResponseDto)
                .toList();

        // Active mentees summary
        List<MentorAssignment> activeAssignments = mentorAssignmentRepository.findByMentorIdAndStatus(mentor.getId(), AssignmentStatus.ACTIVE);
        List<MenteeOverviewItemDto> activeMentees = activeAssignments.stream()
                .map(this::toMenteeOverviewDto)
                .toList();

        // Recent activity feed
        List<MentorRecentActivityDto> recentActivities = new ArrayList<>();
        pendingRequests.forEach(r -> recentActivities.add(new MentorRecentActivityDto(
                "REQUEST",
                "New Mentorship Request",
                r.employeeName() + " requested guidance.",
                r.createdAt(),
                "/mentor/requests"
        )));
        allMentorSessions.stream().limit(5).forEach(s -> recentActivities.add(new MentorRecentActivityDto(
                "SESSION",
                "Mentoring Session " + s.getStatus().name(),
                s.getTitle() + " with " + s.getAssignment().getEmployee().getFullName(),
                s.getScheduledAt(),
                "/mentor/sessions"
        )));
        recentActivities.sort((a, b) -> b.timestamp().compareTo(a.timestamp()));

        return new MentorDashboardOverviewDto(
                profileDto,
                activeCount,
                pendingRequestsCount,
                completedSessionsCount,
                upcomingSessionsCount,
                Math.round(goalCompletionRate * 10.0) / 10.0,
                sharedResourcesCount,
                knowledgeArticlesCount,
                upcomingSessions,
                pendingRequests,
                activeMentees,
                recentActivities.stream().limit(10).toList()
        );
    }

    // -------------------------------------------------------------------------
    // 2. Mentor Profile & Availability
    // -------------------------------------------------------------------------

    @Transactional
    public MentorProfileDto getProfile(String mentorEmail) {
        User mentor = getUserByEmail(mentorEmail);
        MentorProfile mp = getOrCreateMentorProfile(mentor);
        return toProfileDto(mentor, mp);
    }

    @Transactional
    public MentorProfileDto updateProfile(String mentorEmail, UpdateMentorProfileRequest req) {
        User mentor = getUserByEmail(mentorEmail);
        MentorProfile mp = getOrCreateMentorProfile(mentor);

        mp.setHeadline(req.headline().trim());
        mp.setBio(req.bio() != null ? req.bio().trim() : null);
        mp.setExpertiseAreas(req.expertiseAreas().trim());
        mp.setMaxMentees(req.maxMentees());
        mp.setActive(req.isActive());
        if (req.preferredFormat() != null && !req.preferredFormat().isBlank()) {
            mp.setPreferredFormat(req.preferredFormat().trim().toUpperCase());
        }

        // Update employee bio for consistency
        employeeProfileRepository.findByUserId(mentor.getId()).ifPresent(ep -> {
            if (req.bio() != null) ep.setBio(req.bio().trim());
            employeeProfileRepository.save(ep);
        });

        mentorProfileRepository.save(mp);

        // Update availability slots if provided
        if (req.availabilitySlots() != null) {
            mentorAvailabilityRepository.deleteByMentorId(mentor.getId());
            for (MentorAvailabilitySlotDto slot : req.availabilitySlots()) {
                if (slot.dayOfWeek() != null && slot.startTime() != null && slot.endTime() != null) {
                    LocalTime start = LocalTime.parse(slot.startTime().trim());
                    LocalTime end = LocalTime.parse(slot.endTime().trim());
                    if (end.isAfter(start)) {
                        MentorAvailability avail = new MentorAvailability(
                                mentor,
                                slot.dayOfWeek().trim().toUpperCase(),
                                start,
                                end,
                                slot.sessionFormat() != null ? slot.sessionFormat().trim().toUpperCase() : mp.getPreferredFormat(),
                                slot.isAvailable()
                        );
                        mentorAvailabilityRepository.save(avail);
                    }
                }
            }
        }

        return toProfileDto(mentor, mp);
    }

    // -------------------------------------------------------------------------
    // 3. Mentorship Requests
    // -------------------------------------------------------------------------

    @Transactional(readOnly = true)
    public List<MentorshipRequestResponseDto> getRequests(String mentorEmail, String statusFilter) {
        User mentor = getUserByEmail(mentorEmail);
        List<MentorshipRequest> requests = mentorshipRequestRepository.findByMentorIdOrderByCreatedAtDesc(mentor.getId());
        if (statusFilter != null && !statusFilter.isBlank() && !"ALL".equalsIgnoreCase(statusFilter)) {
            requests = requests.stream()
                    .filter(r -> r.getStatus().equalsIgnoreCase(statusFilter.trim()))
                    .toList();
        }
        return requests.stream().map(this::toRequestResponseDto).toList();
    }

    @Transactional
    public MentorshipRequestResponseDto respondToRequest(String mentorEmail, Long requestId, RespondMentorshipRequestDto dto) {
        User mentor = getUserByEmail(mentorEmail);
        MentorshipRequest request = mentorshipRequestRepository.findById(requestId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Mentorship request not found."));

        if (!request.getMentor().getId().equals(mentor.getId()) && mentor.getRole() != Role.ADMIN) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Access denied: You are not authorized to respond to this request.");
        }

        if (!"PENDING".equalsIgnoreCase(request.getStatus())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "This mentorship request has already been processed.");
        }

        String nextStatus = dto.status().trim().toUpperCase();
        if (!"ACCEPTED".equals(nextStatus) && !"DECLINED".equals(nextStatus)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Invalid status. Must be ACCEPTED or DECLINED.");
        }

        request.setStatus(nextStatus);
        request.setResponseNote(dto.responseNote() != null ? dto.responseNote().trim() : null);
        request.setRespondedAt(Instant.now());
        MentorshipRequest saved = mentorshipRequestRepository.save(request);

        if ("ACCEPTED".equals(nextStatus)) {
            // Check capacity
            MentorProfile mp = getOrCreateMentorProfile(mentor);
            long activeCount = mentorAssignmentRepository.countByMentorIdAndStatus(mentor.getId(), AssignmentStatus.ACTIVE);
            if (activeCount >= mp.getMaxMentees()) {
                throw new ApiException(HttpStatus.CONFLICT, "Cannot accept request: You have reached your maximum mentee capacity (" + mp.getMaxMentees() + ").");
            }

            // Check if active assignment exists
            boolean exists = mentorAssignmentRepository.existsByMentorIdAndEmployeeIdAndStatus(
                    mentor.getId(), request.getEmployee().getId(), AssignmentStatus.ACTIVE);
            if (!exists) {
                MentorAssignment assignment = new MentorAssignment(
                        mentor,
                        request.getEmployee(),
                        mentor,
                        "Request accepted: " + (dto.responseNote() != null ? dto.responseNote() : "Welcome!")
                );
                mentorAssignmentRepository.save(assignment);

                // Grant default skill sharing consent
                menteeSkillConsentRepository.findByEmployeeIdAndMentorId(request.getEmployee().getId(), mentor.getId())
                        .orElseGet(() -> menteeSkillConsentRepository.save(
                                new MenteeSkillConsent(request.getEmployee(), mentor, true, true, true, "GRANTED")));
            }
        }

        // Notify employee
        notificationService.createNotification(
                request.getEmployee(),
                "MENTORSHIP_RESPONSE",
                "Mentorship Request " + nextStatus,
                mentor.getFullName() + " has " + nextStatus.toLowerCase() + " your mentorship request.",
                "/dashboard/employee"
        );

        return toRequestResponseDto(saved);
    }

    // -------------------------------------------------------------------------
    // 4. My Mentees & Dossier
    // -------------------------------------------------------------------------

    @Transactional(readOnly = true)
    public List<MenteeOverviewItemDto> getMentees(String mentorEmail, String statusFilter) {
        User mentor = getUserByEmail(mentorEmail);
        List<MentorAssignment> assignments;
        if ("ACTIVE".equalsIgnoreCase(statusFilter)) {
            assignments = mentorAssignmentRepository.findByMentorIdAndStatus(mentor.getId(), AssignmentStatus.ACTIVE);
        } else if ("COMPLETED".equalsIgnoreCase(statusFilter)) {
            assignments = mentorAssignmentRepository.findByMentorIdAndStatus(mentor.getId(), AssignmentStatus.COMPLETED);
        } else {
            assignments = mentorAssignmentRepository.findByMentorIdOrderByAssignedAtDesc(mentor.getId());
        }
        return assignments.stream().map(this::toMenteeOverviewDto).toList();
    }

    @Transactional(readOnly = true)
    public MenteeDossierDto getMenteeDossier(String mentorEmail, Long assignmentId) {
        User mentor = getUserByEmail(mentorEmail);
        MentorAssignment assignment = validateAssignmentOwnership(mentor, assignmentId);
        User emp = assignment.getEmployee();

        MenteeOverviewItemDto summary = toMenteeOverviewDto(assignment);
        EmployeeProfile ep = employeeProfileRepository.findByUserId(emp.getId()).orElse(null);

        MenteeSkillDetailDto skills = getMenteeSkillsInternal(emp, mentor);

        List<MentorGoalDetailDto> goals = mentorshipGoalRepository.findByAssignmentIdOrderByCreatedAtDesc(assignment.getId())
                .stream().map(this::toGoalDetailDto).toList();

        List<MentorSessionSummaryDto> sessions = mentorshipSessionRepository.findByAssignmentIdOrderByScheduledAtDesc(assignment.getId())
                .stream().map(this::toSessionSummaryDto).toList();

        List<MentorSharedResourceDto> resources = sharedResourceRepository.findByAssignmentIdOrderByCreatedAtDesc(assignment.getId())
                .stream().map(this::toSharedResourceDto).toList();

        List<MentorEvaluationDto> evaluations = mentorEvaluationRepository.findByEmployeeIdOrderByCreatedAtDesc(emp.getId())
                .stream()
                .filter(ev -> ev.getMentor().getId().equals(mentor.getId()))
                .map(this::toEvaluationDto)
                .toList();

        return new MenteeDossierDto(
                summary,
                ep != null ? ep.getBio() : "",
                ep != null ? ep.getExperienceLevel() : "Entry level",
                skills,
                goals,
                sessions,
                resources,
                evaluations
        );
    }

    @Transactional
    public MenteeOverviewItemDto endMentorship(String mentorEmail, Long assignmentId, EndMentorshipRequest req) {
        User mentor = getUserByEmail(mentorEmail);
        MentorAssignment assignment = validateAssignmentOwnership(mentor, assignmentId);

        if (assignment.getStatus() != AssignmentStatus.ACTIVE) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Mentorship is not active.");
        }

        assignment.setStatus(AssignmentStatus.COMPLETED);
        assignment.setEndedAt(Instant.now());
        if (req != null && req.reason() != null && !req.reason().isBlank()) {
            assignment.setNotes((assignment.getNotes() != null ? assignment.getNotes() + "\n" : "") + "Completed: " + req.reason().trim());
        }
        MentorAssignment saved = mentorAssignmentRepository.save(assignment);

        notificationService.createNotification(
                assignment.getEmployee(),
                "MENTORSHIP_COMPLETED",
                "Mentorship Concluded",
                "Your mentorship with " + mentor.getFullName() + " has been marked as completed.",
                "/dashboard/employee"
        );

        return toMenteeOverviewDto(saved);
    }

    // -------------------------------------------------------------------------
    // 5. Mentee Skill Overview (Privacy & Consent Enforced, Read-Only)
    // -------------------------------------------------------------------------

    @Transactional(readOnly = true)
    public MenteeSkillDetailDto getMenteeSkills(String mentorEmail, Long menteeId) {
        User mentor = getUserByEmail(mentorEmail);
        User emp = userRepository.findById(menteeId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Mentee not found."));

        // IDOR check: must have active assignment
        boolean hasActive = mentorAssignmentRepository.existsByMentorIdAndEmployeeIdAndStatus(
                mentor.getId(), emp.getId(), AssignmentStatus.ACTIVE);
        if (!hasActive && mentor.getRole() != Role.ADMIN) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Access denied: You can only view skill profiles for active mentees.");
        }

        return getMenteeSkillsInternal(emp, mentor);
    }

    private MenteeSkillDetailDto getMenteeSkillsInternal(User emp, User mentor) {
        MenteeSkillConsent consent = menteeSkillConsentRepository.findByEmployeeIdAndMentorId(emp.getId(), mentor.getId())
                .orElse(null);

        boolean consentGranted = consent == null || "GRANTED".equalsIgnoreCase(consent.getConsentStatus());

        if (!consentGranted) {
            return new MenteeSkillDetailDto(emp.getId(), emp.getFullName(), false, List.of(), List.of(), List.of());
        }

        List<MenteeSkillRecordDto> skills = employeeSkillRepository.findByUserId(emp.getId()).stream()
                .map(es -> new MenteeSkillRecordDto(
                        es.getSkill().getId(),
                        es.getSkill().getName(),
                        es.getSkill().getCategory() != null ? es.getSkill().getCategory().getName() : "General",
                        es.getProficiencyLevel() != null ? es.getProficiencyLevel() : "Beginner",
                        es.isVerified(),
                        es.getUpdatedAt()
                ))
                .toList();

        List<MenteeSkillGapRecordDto> gaps = skillGapRepository.findByUserId(emp.getId()).stream()
                .map(sg -> new MenteeSkillGapRecordDto(
                        sg.getId(),
                        sg.getSkill().getName(),
                        "Level " + sg.getCurrentLevel(),
                        "Level " + sg.getRequiredLevel(),
                        sg.getPriority() != null ? sg.getPriority() : "MEDIUM"
                ))
                .toList();

        List<MenteeAssessmentSummaryDto> assessments = assessmentAttemptRepository.findByUserIdOrderByStartedAtDesc(emp.getId()).stream()
                .map(att -> new MenteeAssessmentSummaryDto(
                        att.getId(),
                        att.getAssessment() != null ? att.getAssessment().getTitle() : (att.getSkill().getName() + " Skill Assessment"),
                        att.getSkill().getName(),
                        att.getScore() != null ? att.getScore() : 0,
                        Boolean.TRUE.equals(att.getPassed()),
                        att.getCompletedAt() != null ? att.getCompletedAt() : att.getStartedAt()
                ))
                .limit(10)
                .toList();

        return new MenteeSkillDetailDto(emp.getId(), emp.getFullName(), true, skills, gaps, assessments);
    }

    // -------------------------------------------------------------------------
    // 6. Development Goals & Action Items
    // -------------------------------------------------------------------------

    @Transactional(readOnly = true)
    public List<MentorGoalDetailDto> getGoals(String mentorEmail, Long menteeId, String statusFilter) {
        User mentor = getUserByEmail(mentorEmail);
        List<MentorshipGoal> goals = mentorshipGoalRepository.findByMentorIdOrderByCreatedAtDesc(mentor.getId());

        if (menteeId != null) {
            goals = goals.stream().filter(g -> g.getAssignment().getEmployee().getId().equals(menteeId)).toList();
        }

        if (statusFilter != null && !statusFilter.isBlank() && !"ALL".equalsIgnoreCase(statusFilter)) {
            goals = goals.stream().filter(g -> g.getStatus().name().equalsIgnoreCase(statusFilter.trim())).toList();
        }

        return goals.stream().map(this::toGoalDetailDto).toList();
    }

    @Transactional
    public MentorGoalDetailDto createGoal(String mentorEmail, CreateMentorGoalRequest req) {
        User mentor = getUserByEmail(mentorEmail);
        MentorAssignment assignment = validateAssignmentOwnership(mentor, req.assignmentId());

        if (assignment.getStatus() != AssignmentStatus.ACTIVE) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Cannot add goals to inactive mentorship assignments.");
        }

        Skill skill = null;
        if (req.skillId() != null) {
            skill = skillRepository.findById(req.skillId()).orElse(null);
        }

        MentorshipGoal goal = new MentorshipGoal(
                assignment,
                req.title().trim(),
                req.description() != null ? req.description().trim() : null,
                req.targetDate(),
                req.priority() != null ? req.priority().trim().toUpperCase() : "MEDIUM",
                skill
        );
        MentorshipGoal saved = mentorshipGoalRepository.save(goal);

        notificationService.createNotification(
                assignment.getEmployee(),
                "GOAL_CREATED",
                "New Mentorship Goal",
                mentor.getFullName() + " created a new development goal: " + saved.getTitle(),
                "/dashboard/employee"
        );

        return toGoalDetailDto(saved);
    }

    @Transactional
    public MentorGoalDetailDto updateGoal(String mentorEmail, Long goalId, UpdateMentorGoalRequest req) {
        User mentor = getUserByEmail(mentorEmail);
        MentorshipGoal goal = mentorshipGoalRepository.findById(goalId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Goal not found."));

        validateAssignmentOwnership(mentor, goal.getAssignment().getId());

        try {
            GoalStatus status = GoalStatus.valueOf(req.status().trim().toUpperCase());
            goal.setStatus(status);
        } catch (IllegalArgumentException e) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Invalid goal status: " + req.status());
        }

        if (req.title() != null && !req.title().isBlank()) goal.setTitle(req.title().trim());
        if (req.description() != null) goal.setDescription(req.description().trim());
        if (req.targetDate() != null) goal.setTargetDate(req.targetDate());
        if (req.priority() != null && !req.priority().isBlank()) goal.setPriority(req.priority().trim().toUpperCase());

        MentorshipGoal saved = mentorshipGoalRepository.save(goal);
        return toGoalDetailDto(saved);
    }

    @Transactional
    public GoalActionItemDto createGoalActionItem(String mentorEmail, Long goalId, CreateGoalActionItemRequest req) {
        User mentor = getUserByEmail(mentorEmail);
        MentorshipGoal goal = mentorshipGoalRepository.findById(goalId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Goal not found."));

        validateAssignmentOwnership(mentor, goal.getAssignment().getId());

        GoalActionItem item = new GoalActionItem(
                goal,
                req.title().trim(),
                req.description() != null ? req.description().trim() : null,
                req.dueDate()
        );
        GoalActionItem saved = goalActionItemRepository.save(item);
        return toActionItemDto(saved);
    }

    @Transactional
    public GoalActionItemDto toggleGoalActionItem(String mentorEmail, Long actionItemId) {
        User mentor = getUserByEmail(mentorEmail);
        GoalActionItem item = goalActionItemRepository.findById(actionItemId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Action item not found."));

        validateAssignmentOwnership(mentor, item.getGoal().getAssignment().getId());

        item.setCompleted(!item.isCompleted());
        item.setCompletedAt(item.isCompleted() ? Instant.now() : null);
        GoalActionItem saved = goalActionItemRepository.save(item);
        return toActionItemDto(saved);
    }

    @Transactional
    public void deleteGoalActionItem(String mentorEmail, Long actionItemId) {
        User mentor = getUserByEmail(mentorEmail);
        GoalActionItem item = goalActionItemRepository.findById(actionItemId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Action item not found."));

        validateAssignmentOwnership(mentor, item.getGoal().getAssignment().getId());
        goalActionItemRepository.delete(item);
    }

    // -------------------------------------------------------------------------
    // 7. Mentoring Sessions & Notes
    // -------------------------------------------------------------------------

    @Transactional(readOnly = true)
    public List<MentorSessionSummaryDto> getSessions(String mentorEmail, Long menteeId, String statusFilter) {
        User mentor = getUserByEmail(mentorEmail);
        List<MentorshipSession> sessions = mentorshipSessionRepository.findByMentorIdOrderByScheduledAtDesc(mentor.getId());

        if (menteeId != null) {
            sessions = sessions.stream().filter(s -> s.getAssignment().getEmployee().getId().equals(menteeId)).toList();
        }

        if (statusFilter != null && !statusFilter.isBlank() && !"ALL".equalsIgnoreCase(statusFilter)) {
            sessions = sessions.stream().filter(s -> s.getStatus().name().equalsIgnoreCase(statusFilter.trim())).toList();
        }

        return sessions.stream().map(this::toSessionSummaryDto).toList();
    }

    @Transactional
    public MentorSessionSummaryDto createSession(String mentorEmail, CreateMentorSessionRequest req) {
        User mentor = getUserByEmail(mentorEmail);
        MentorAssignment assignment = validateAssignmentOwnership(mentor, req.assignmentId());

        if (assignment.getStatus() != AssignmentStatus.ACTIVE) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Cannot schedule sessions for inactive assignments.");
        }

        // Conflict check: ensure no overlapping session scheduled within duration window
        Instant start = req.scheduledAt();
        Instant end = start.plusSeconds(req.durationMinutes() * 60L);
        List<MentorshipSession> existing = mentorshipSessionRepository.findByMentorIdOrderByScheduledAtDesc(mentor.getId());
        boolean hasConflict = existing.stream()
                .filter(s -> s.getStatus() == SessionStatus.SCHEDULED)
                .anyMatch(s -> {
                    Instant sStart = s.getScheduledAt();
                    Instant sEnd = sStart.plusSeconds(s.getDurationMinutes() * 60L);
                    return start.isBefore(sEnd) && end.isAfter(sStart);
                });
        if (hasConflict) {
            throw new ApiException(HttpStatus.CONFLICT, "Time conflict: You already have another session scheduled during this time window.");
        }

        MentorshipSession session = new MentorshipSession(
                assignment,
                req.title().trim(),
                req.description() != null ? req.description().trim() : null,
                req.scheduledAt(),
                req.durationMinutes(),
                req.meetingLink() != null && !req.meetingLink().isBlank() ? req.meetingLink().trim() : "https://meet.google.com/skill-sphere"
        );
        MentorshipSession saved = mentorshipSessionRepository.save(session);

        notificationService.createNotification(
                assignment.getEmployee(),
                "SESSION_SCHEDULED",
                "New Mentoring Session Scheduled",
                mentor.getFullName() + " scheduled a session: " + saved.getTitle(),
                "/dashboard/employee"
        );

        return toSessionSummaryDto(saved);
    }

    @Transactional
    public MentorSessionSummaryDto updateSessionStatus(String mentorEmail, Long sessionId, String statusStr) {
        User mentor = getUserByEmail(mentorEmail);
        MentorshipSession session = mentorshipSessionRepository.findById(sessionId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Session not found."));

        validateAssignmentOwnership(mentor, session.getAssignment().getId());

        try {
            SessionStatus status = SessionStatus.valueOf(statusStr.trim().toUpperCase());
            session.setStatus(status);
        } catch (IllegalArgumentException e) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Invalid session status: " + statusStr);
        }

        MentorshipSession saved = mentorshipSessionRepository.save(session);
        return toSessionSummaryDto(saved);
    }

    @Transactional
    public MentorSessionSummaryDto updateSessionNotes(String mentorEmail, Long sessionId, UpdateSessionNotesRequest req) {
        User mentor = getUserByEmail(mentorEmail);
        MentorshipSession session = mentorshipSessionRepository.findById(sessionId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Session not found."));

        validateAssignmentOwnership(mentor, session.getAssignment().getId());

        if (req.sharedNotes() != null) session.setSharedNotes(req.sharedNotes().trim());
        if (req.mentorPrepNotes() != null) session.setMentorPrepNotes(req.mentorPrepNotes().trim());

        MentorshipSession saved = mentorshipSessionRepository.save(session);
        return toSessionSummaryDto(saved);
    }

    // -------------------------------------------------------------------------
    // 8. Shared Resources
    // -------------------------------------------------------------------------

    @Transactional(readOnly = true)
    public List<MentorSharedResourceDto> getResources(String mentorEmail, Long menteeId) {
        User mentor = getUserByEmail(mentorEmail);
        List<SharedResource> resources = sharedResourceRepository.findByMentorIdOrderByCreatedAtDesc(mentor.getId());
        if (menteeId != null) {
            resources = resources.stream().filter(r -> r.getAssignment().getEmployee().getId().equals(menteeId)).toList();
        }
        return resources.stream().map(this::toSharedResourceDto).toList();
    }

    @Transactional
    public MentorSharedResourceDto createResource(String mentorEmail, CreateMentorSharedResourceRequest req) {
        User mentor = getUserByEmail(mentorEmail);
        MentorAssignment assignment = validateAssignmentOwnership(mentor, req.assignmentId());

        Skill skill = null;
        if (req.skillId() != null) {
            skill = skillRepository.findById(req.skillId()).orElse(null);
        }

        SharedResource resource = new SharedResource(assignment, req.title().trim(), req.urlOrContent().trim(), mentor);
        resource.setResourceType(req.resourceType() != null && !req.resourceType().isBlank() ? req.resourceType().trim().toUpperCase() : "ARTICLE");
        resource.setDescription(req.description() != null ? req.description().trim() : null);
        resource.setSkill(skill);

        SharedResource saved = sharedResourceRepository.save(resource);

        notificationService.createNotification(
                assignment.getEmployee(),
                "RESOURCE_SHARED",
                "New Learning Resource",
                mentor.getFullName() + " shared a resource: " + saved.getTitle(),
                "/dashboard/employee"
        );

        return toSharedResourceDto(saved);
    }

    @Transactional
    public void deleteResource(String mentorEmail, Long resourceId) {
        User mentor = getUserByEmail(mentorEmail);
        SharedResource resource = sharedResourceRepository.findById(resourceId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Resource not found."));

        if (!resource.getSharedBy().getId().equals(mentor.getId()) && mentor.getRole() != Role.ADMIN) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Access denied: You can only delete resources shared by yourself.");
        }
        sharedResourceRepository.delete(resource);
    }

    // -------------------------------------------------------------------------
    // 9. Knowledge Contributions
    // -------------------------------------------------------------------------

    @Transactional(readOnly = true)
    public List<MentorArticleDto> getArticles(String mentorEmail) {
        User mentor = getUserByEmail(mentorEmail);
        return knowledgeArticleRepository.findByAuthorIdOrderByCreatedAtDesc(mentor.getId()).stream()
                .map(this::toArticleDto)
                .toList();
    }

    @Transactional
    public MentorArticleDto createArticle(String mentorEmail, CreateOrUpdateMentorArticleRequest req) {
        User mentor = getUserByEmail(mentorEmail);

        KnowledgeCategory category = null;
        if (req.categoryId() != null) {
            category = knowledgeCategoryRepository.findById(req.categoryId()).orElse(null);
        }

        KnowledgeArticle article = new KnowledgeArticle(
                category,
                mentor,
                req.title().trim(),
                req.content().trim(),
                req.tags() != null ? req.tags().trim() : null,
                req.status() != null && !req.status().isBlank() ? req.status().trim().toUpperCase() : "PUBLISHED"
        );
        KnowledgeArticle saved = knowledgeArticleRepository.save(article);
        return toArticleDto(saved);
    }

    @Transactional
    public MentorArticleDto updateArticle(String mentorEmail, Long articleId, CreateOrUpdateMentorArticleRequest req) {
        User mentor = getUserByEmail(mentorEmail);
        KnowledgeArticle article = knowledgeArticleRepository.findById(articleId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Article not found."));

        if (!article.getAuthor().getId().equals(mentor.getId()) && mentor.getRole() != Role.ADMIN) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Access denied: You can only modify your own articles.");
        }

        if (req.categoryId() != null) {
            KnowledgeCategory cat = knowledgeCategoryRepository.findById(req.categoryId()).orElse(null);
            article.setCategory(cat);
        }
        article.setTitle(req.title().trim());
        article.setContent(req.content().trim());
        if (req.tags() != null) article.setTags(req.tags().trim());
        if (req.status() != null && !req.status().isBlank()) article.setStatus(req.status().trim().toUpperCase());

        KnowledgeArticle saved = knowledgeArticleRepository.save(article);
        return toArticleDto(saved);
    }

    @Transactional
    public void deleteArticle(String mentorEmail, Long articleId) {
        User mentor = getUserByEmail(mentorEmail);
        KnowledgeArticle article = knowledgeArticleRepository.findById(articleId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Article not found."));

        if (!article.getAuthor().getId().equals(mentor.getId()) && mentor.getRole() != Role.ADMIN) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Access denied: You can only delete your own articles.");
        }
        knowledgeArticleRepository.delete(article);
    }

    // -------------------------------------------------------------------------
    // 10. Feedback & Evaluations
    // -------------------------------------------------------------------------

    @Transactional(readOnly = true)
    public MentorFeedbackOverviewDto getFeedbackOverview(String mentorEmail) {
        User mentor = getUserByEmail(mentorEmail);

        // Feedback received from mentees on sessions
        List<MentorshipFeedback> received = mentorshipFeedbackRepository.findByMentorIdOrderByCreatedAtDesc(mentor.getId());
        List<MentorshipFeedbackResponseDto> receivedDto = received.stream()
                .map(f -> new MentorshipFeedbackResponseDto(
                        f.getId(),
                        f.getSession().getId(),
                        f.getSession().getTitle(),
                        f.getAuthor().getFullName(),
                        f.getRating(),
                        f.getComments(),
                        f.getCreatedAt()
                ))
                .toList();

        // Submitted evaluations by mentor
        List<MentorEvaluationDto> submitted = mentorEvaluationRepository.findByMentorIdOrderByCreatedAtDesc(mentor.getId()).stream()
                .map(this::toEvaluationDto)
                .toList();

        double avg = received.stream().mapToInt(MentorshipFeedback::getRating).average().orElse(0.0);
        Map<Integer, Long> dist = received.stream()
                .collect(Collectors.groupingBy(MentorshipFeedback::getRating, Collectors.counting()));

        return new MentorFeedbackOverviewDto(
                Math.round(avg * 10.0) / 10.0,
                received.size(),
                dist,
                receivedDto,
                submitted
        );
    }

    @Transactional
    public MentorEvaluationDto createEvaluation(String mentorEmail, CreateMentorEvaluationRequest req) {
        User mentor = getUserByEmail(mentorEmail);
        User emp = userRepository.findById(req.employeeId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Employee not found."));

        // Verify assignment exists between mentor and employee
        boolean exists = mentorAssignmentRepository.existsByMentorIdAndEmployeeIdAndStatus(mentor.getId(), emp.getId(), AssignmentStatus.ACTIVE)
                || mentorAssignmentRepository.existsByMentorIdAndEmployeeIdAndStatus(mentor.getId(), emp.getId(), AssignmentStatus.COMPLETED);
        if (!exists && mentor.getRole() != Role.ADMIN) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Access denied: You can only evaluate employees you have mentored.");
        }

        MentorshipSession session = null;
        if (req.sessionId() != null) {
            session = mentorshipSessionRepository.findById(req.sessionId()).orElse(null);
        }

        MentorEvaluation evaluation = new MentorEvaluation(
                mentor,
                emp,
                session,
                req.rating(),
                req.strengths() != null ? req.strengths().trim() : null,
                req.areasForGrowth() != null ? req.areasForGrowth().trim() : null,
                req.comments().trim()
        );
        MentorEvaluation saved = mentorEvaluationRepository.save(evaluation);

        notificationService.createNotification(
                emp,
                "MENTOR_EVALUATION",
                "New Mentor Evaluation",
                mentor.getFullName() + " has submitted a development evaluation for you.",
                "/dashboard/employee"
        );

        return toEvaluationDto(saved);
    }

    // -------------------------------------------------------------------------
    // 11. Notifications
    // -------------------------------------------------------------------------

    @Transactional(readOnly = true)
    public List<NotificationDto> getNotifications(String mentorEmail, Boolean unreadOnly) {
        User mentor = getUserByEmail(mentorEmail);
        List<NotificationDto> list = notificationService.getUserNotifications(mentor.getEmail());
        if (Boolean.TRUE.equals(unreadOnly)) {
            list = list.stream().filter(n -> !n.isRead()).toList();
        }
        return list;
    }

    @Transactional
    public void markNotificationAsRead(String mentorEmail, Long notificationId) {
        User mentor = getUserByEmail(mentorEmail);
        notificationService.markAsRead(mentor.getEmail(), notificationId);
    }

    @Transactional
    public void markAllNotificationsAsRead(String mentorEmail) {
        User mentor = getUserByEmail(mentorEmail);
        notificationService.markAllAsRead(mentor.getEmail());
    }

    // -------------------------------------------------------------------------
    // DTO Mappers
    // -------------------------------------------------------------------------

    private MentorProfileDto toProfileDto(User mentor, MentorProfile mp) {
        EmployeeProfile ep = employeeProfileRepository.findByUserId(mentor.getId()).orElse(null);
        long activeCount = mentorAssignmentRepository.countByMentorIdAndStatus(mentor.getId(), AssignmentStatus.ACTIVE);

        List<String> tags = mp.getExpertiseAreas() != null
                ? Arrays.stream(mp.getExpertiseAreas().split("[,;\\n]")).map(String::trim).filter(s -> !s.isBlank()).toList()
                : List.of();

        List<MentorAvailabilitySlotDto> slots = mentorAvailabilityRepository.findByMentorIdOrderByDayOfWeekAscStartTimeAsc(mentor.getId()).stream()
                .map(s -> new MentorAvailabilitySlotDto(
                        s.getId(),
                        s.getDayOfWeek(),
                        s.getStartTime().toString(),
                        s.getEndTime().toString(),
                        s.getSessionFormat(),
                        s.isAvailable()
                ))
                .toList();

        return new MentorProfileDto(
                mp.getId(),
                mentor.getId(),
                mentor.getFullName(),
                mentor.getEmail(),
                mp.getHeadline(),
                mp.getBio() != null ? mp.getBio() : (ep != null ? ep.getBio() : ""),
                ep != null && ep.getDepartment() != null ? ep.getDepartment() : "Engineering",
                ep != null && ep.getJobRole() != null ? ep.getJobRole() : "Senior Specialist",
                ep != null && ep.getExperienceLevel() != null ? ep.getExperienceLevel() : "Experienced",
                mp.getExpertiseAreas(),
                tags,
                mp.getMaxMentees(),
                (int) activeCount,
                mp.isActive(),
                mp.getPreferredFormat() != null ? mp.getPreferredFormat() : "VIRTUAL",
                slots
        );
    }

    private MenteeOverviewItemDto toMenteeOverviewDto(MentorAssignment a) {
        User emp = a.getEmployee();
        EmployeeProfile ep = employeeProfileRepository.findByUserId(emp.getId()).orElse(null);

        List<MentorshipGoal> goals = mentorshipGoalRepository.findByAssignmentIdOrderByCreatedAtDesc(a.getId());
        int totalGoals = goals.size();
        int completedGoals = (int) goals.stream().filter(g -> g.getStatus() == GoalStatus.ACHIEVED || g.getStatus() == GoalStatus.COMPLETED).count();

        List<MentorshipSession> sessions = mentorshipSessionRepository.findByAssignmentIdOrderByScheduledAtDesc(a.getId());
        int totalSessions = sessions.size();

        Instant now = Instant.now();
        Instant nextSession = sessions.stream()
                .filter(s -> s.getStatus() == SessionStatus.SCHEDULED && s.getScheduledAt().isAfter(now))
                .map(MentorshipSession::getScheduledAt)
                .min(Instant::compareTo)
                .orElse(null);

        return new MenteeOverviewItemDto(
                a.getId(),
                emp.getId(),
                emp.getFullName(),
                emp.getEmail(),
                emp.getEmployeeId() != null ? emp.getEmployeeId() : ("EMP-" + emp.getId()),
                ep != null && ep.getJobRole() != null ? ep.getJobRole() : "Software Developer",
                ep != null && ep.getDepartment() != null ? ep.getDepartment() : "Engineering",
                a.getStatus().name(),
                a.getAssignedAt(),
                totalGoals,
                completedGoals,
                totalSessions,
                nextSession
        );
    }

    private MentorshipRequestResponseDto toRequestResponseDto(MentorshipRequest r) {
        String mentorHeadline = mentorProfileRepository.findByUserId(r.getMentor().getId())
                .map(MentorProfile::getHeadline)
                .orElse("Expert Mentor");
        String empRole = employeeProfileRepository.findByUserId(r.getEmployee().getId())
                .map(EmployeeProfile::getJobRole)
                .orElse("Software Developer");

        return new MentorshipRequestResponseDto(
                r.getId(),
                r.getMentor().getId(),
                r.getMentor().getFullName(),
                r.getMentor().getEmail(),
                mentorHeadline,
                r.getEmployee().getId(),
                r.getEmployee().getFullName(),
                r.getEmployee().getEmail(),
                empRole,
                r.getMessage(),
                r.getStatus(),
                r.getCreatedAt(),
                r.getRespondedAt(),
                r.getResponseNote()
        );
    }

    private MentorGoalDetailDto toGoalDetailDto(MentorshipGoal g) {
        List<GoalActionItemDto> actionItems = goalActionItemRepository.findByGoalIdOrderByCreatedAtAsc(g.getId()).stream()
                .map(this::toActionItemDto)
                .toList();

        return new MentorGoalDetailDto(
                g.getId(),
                g.getAssignment().getId(),
                g.getAssignment().getEmployee().getId(),
                g.getAssignment().getEmployee().getFullName(),
                g.getTitle(),
                g.getDescription(),
                g.getTargetDate(),
                g.getStatus().name(),
                g.getPriority() != null ? g.getPriority() : "MEDIUM",
                g.getSkill() != null ? g.getSkill().getId() : null,
                g.getSkill() != null ? g.getSkill().getName() : null,
                g.getCreatedAt(),
                actionItems
        );
    }

    private GoalActionItemDto toActionItemDto(GoalActionItem item) {
        return new GoalActionItemDto(
                item.getId(),
                item.getGoal().getId(),
                item.getTitle(),
                item.getDescription(),
                item.isCompleted(),
                item.getDueDate(),
                item.getCompletedAt()
        );
    }

    private MentorSessionSummaryDto toSessionSummaryDto(MentorshipSession s) {
        return new MentorSessionSummaryDto(
                s.getId(),
                s.getAssignment().getId(),
                s.getAssignment().getEmployee().getId(),
                s.getAssignment().getEmployee().getFullName(),
                s.getTitle(),
                s.getDescription(),
                s.getScheduledAt(),
                s.getDurationMinutes(),
                s.getMeetingLink(),
                s.getStatus().name(),
                s.getSharedNotes(),
                s.getMentorPrepNotes()
        );
    }

    private MentorSharedResourceDto toSharedResourceDto(SharedResource r) {
        return new MentorSharedResourceDto(
                r.getId(),
                r.getAssignment().getId(),
                r.getAssignment().getEmployee().getId(),
                r.getAssignment().getEmployee().getFullName(),
                r.getTitle(),
                r.getUrlOrContent(),
                r.getResourceType() != null ? r.getResourceType() : "ARTICLE",
                r.getDescription(),
                r.getSkill() != null ? r.getSkill().getId() : null,
                r.getSkill() != null ? r.getSkill().getName() : null,
                r.getCreatedAt()
        );
    }

    private MentorEvaluationDto toEvaluationDto(MentorEvaluation ev) {
        return new MentorEvaluationDto(
                ev.getId(),
                ev.getMentor().getId(),
                ev.getMentor().getFullName(),
                ev.getEmployee().getId(),
                ev.getEmployee().getFullName(),
                ev.getSession() != null ? ev.getSession().getId() : null,
                ev.getSession() != null ? ev.getSession().getTitle() : null,
                ev.getRating(),
                ev.getStrengths(),
                ev.getAreasForGrowth(),
                ev.getComments(),
                ev.getCreatedAt()
        );
    }

    private MentorArticleDto toArticleDto(KnowledgeArticle a) {
        return new MentorArticleDto(
                a.getId(),
                a.getCategory() != null ? a.getCategory().getId() : null,
                a.getCategory() != null ? a.getCategory().getName() : "General",
                a.getTitle(),
                a.getContent(),
                a.getTags(),
                a.getStatus(),
                a.getCreatedAt(),
                a.getUpdatedAt()
        );
    }
}
