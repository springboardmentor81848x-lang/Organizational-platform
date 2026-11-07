package com.skillsphere.service;

import com.skillsphere.dto.*;
import com.skillsphere.dto.RecommendationRequest.SkillResult;
import com.skillsphere.entity.*;
import com.skillsphere.exception.ApiException;
import com.skillsphere.repository.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ManagerService {

    private final UserRepository userRepository;
    private final EmployeeProfileRepository profileRepository;
    private final TeamRepository teamRepository;
    private final TeamMembershipRepository membershipRepository;
    private final SkillRepository skillRepository;
    private final SkillCategoryRepository categoryRepository;
    private final RoleSkillRequirementRepository requirementRepository;
    private final EmployeeSkillRepository employeeSkillRepository;
    private final SkillGapRepository skillGapRepository;
    private final AssessmentAttemptRepository assessmentAttemptRepository;
    private final CourseRepository courseRepository;
    private final EmployeeLearningRepository learningRepository;
    private final MentorAssignmentRepository mentorAssignmentRepository;
    private final AdminMentorshipService adminMentorshipService;
    private final ReassessmentRequestRepository reassessmentRequestRepository;
    private final ManagerCourseRecommendationRepository managerCourseRecommendationRepository;
    private final NotificationRepository notificationRepository;
    private final NotificationService notificationService;
    private final GeminiRecommendationService geminiRecommendationService;

    public ManagerService(UserRepository userRepository,
                          EmployeeProfileRepository profileRepository,
                          TeamRepository teamRepository,
                          TeamMembershipRepository membershipRepository,
                          SkillRepository skillRepository,
                          SkillCategoryRepository categoryRepository,
                          RoleSkillRequirementRepository requirementRepository,
                          EmployeeSkillRepository employeeSkillRepository,
                          SkillGapRepository skillGapRepository,
                          AssessmentAttemptRepository assessmentAttemptRepository,
                          CourseRepository courseRepository,
                          EmployeeLearningRepository learningRepository,
                          MentorAssignmentRepository mentorAssignmentRepository,
                          AdminMentorshipService adminMentorshipService,
                          ReassessmentRequestRepository reassessmentRequestRepository,
                          ManagerCourseRecommendationRepository managerCourseRecommendationRepository,
                          NotificationRepository notificationRepository,
                          NotificationService notificationService,
                          GeminiRecommendationService geminiRecommendationService) {
        this.userRepository = userRepository;
        this.profileRepository = profileRepository;
        this.teamRepository = teamRepository;
        this.membershipRepository = membershipRepository;
        this.skillRepository = skillRepository;
        this.categoryRepository = categoryRepository;
        this.requirementRepository = requirementRepository;
        this.employeeSkillRepository = employeeSkillRepository;
        this.skillGapRepository = skillGapRepository;
        this.assessmentAttemptRepository = assessmentAttemptRepository;
        this.courseRepository = courseRepository;
        this.learningRepository = learningRepository;
        this.mentorAssignmentRepository = mentorAssignmentRepository;
        this.adminMentorshipService = adminMentorshipService;
        this.reassessmentRequestRepository = reassessmentRequestRepository;
        this.managerCourseRecommendationRepository = managerCourseRecommendationRepository;
        this.notificationRepository = notificationRepository;
        this.notificationService = notificationService;
        this.geminiRecommendationService = geminiRecommendationService;
    }

    // -------------------------------------------------------------
    // Security & Scope Helpers
    // -------------------------------------------------------------

    private User getUserByEmail(String email) {
        return userRepository.findByEmail(email.trim().toLowerCase())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "User not found."));
    }

    public List<Team> getManagedTeams(User caller) {
        if (caller.getRole() == Role.ADMIN) {
            return teamRepository.findAll();
        }
        return teamRepository.findByManagerId(caller.getId());
    }

    public void validateTeamAccess(User caller, Long teamId) {
        if (caller.getRole() == Role.ADMIN) return;
        List<Team> managed = teamRepository.findByManagerId(caller.getId());
        boolean owns = managed.stream().anyMatch(t -> t.getId().equals(teamId));
        if (!owns) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Access denied. You can only access teams assigned to you.");
        }
    }

    public void validateEmployeeAccess(User caller, Long employeeId) {
        if (caller.getRole() == Role.ADMIN) return;
        List<TeamMembership> memberships = membershipRepository.findByManagerUserId(caller.getId());
        boolean belongs = memberships.stream().anyMatch(m -> m.getUser().getId().equals(employeeId));
        if (!belongs) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Access denied. You can only access employees in your managed teams.");
        }
    }

    private List<TeamMembership> getScopedMemberships(User caller, Long teamIdFilter) {
        if (teamIdFilter != null) {
            validateTeamAccess(caller, teamIdFilter);
            return membershipRepository.findByTeamId(teamIdFilter);
        }
        if (caller.getRole() == Role.ADMIN) {
            return membershipRepository.findAll();
        }
        return membershipRepository.findByManagerUserId(caller.getId());
    }

    private List<User> getScopedEmployees(User caller, Long teamIdFilter) {
        List<TeamMembership> memberships = getScopedMemberships(caller, teamIdFilter);
        return memberships.stream()
                .map(TeamMembership::getUser)
                .filter(u -> u.getRole() == Role.EMPLOYEE)
                .distinct()
                .toList();
    }

    private List<SkillGap> getSkillGapsForUsers(List<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) return List.of();
        return skillGapRepository.findByUserIds(userIds);
    }

    private List<EmployeeSkill> getEmployeeSkillsForUsers(List<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) return List.of();
        return employeeSkillRepository.findByUserIds(userIds);
    }

    private List<EmployeeLearning> getLearningsForUsers(List<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) return List.of();
        return learningRepository.findByUserIds(userIds);
    }

    private List<ReassessmentRequest> getReassessmentsForUsers(List<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) return List.of();
        return reassessmentRequestRepository.findByEmployeeIdIn(userIds);
    }

    private List<ManagerCourseRecommendation> getCourseRecommendationsForUsers(List<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) return List.of();
        return managerCourseRecommendationRepository.findByEmployeeIdIn(userIds);
    }

    private List<AssessmentAttempt> getAssessmentAttemptsForUsers(List<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) return List.of();
        return assessmentAttemptRepository.findByUserIdsOrderByStartedAtDesc(userIds);
    }

    // -------------------------------------------------------------
    // 1. Teams List
    // -------------------------------------------------------------

    @Transactional(readOnly = true)
    public List<TeamDto> getManagedTeamsDto(String callerEmail) {
        User caller = getUserByEmail(callerEmail);
        List<Team> teams = getManagedTeams(caller);
        return teams.stream().map(t -> {
            List<TeamMembership> members = membershipRepository.findByTeamId(t.getId());
            return new TeamDto(
                    t.getId(),
                    t.getDepartment() != null ? t.getDepartment().getId() : null,
                    t.getDepartment() != null ? t.getDepartment().getName() : "Unassigned",
                    t.getName(),
                    t.getManager() != null ? t.getManager().getId() : null,
                    t.getManager() != null ? t.getManager().getFullName() : "Unassigned",
                    members.size(),
                    List.of()
            );
        }).toList();
    }

    // -------------------------------------------------------------
    // 2. Dashboard Overview
    // -------------------------------------------------------------

    @Transactional(readOnly = true)
    public ManagerDashboardOverviewDto getDashboardOverview(String callerEmail, Long teamIdFilter) {
        User caller = getUserByEmail(callerEmail);
        List<Team> managedTeams = (teamIdFilter != null)
                ? List.of(teamRepository.findById(teamIdFilter).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Team not found.")))
                : getManagedTeams(caller);

        List<TeamMembership> scopedMemberships = getScopedMemberships(caller, teamIdFilter);
        List<User> employees = scopedMemberships.stream()
                .map(TeamMembership::getUser)
                .filter(u -> u.getRole() == Role.EMPLOYEE)
                .distinct()
                .toList();
        List<Long> employeeIds = employees.stream().map(User::getId).toList();

        if (employeeIds.isEmpty()) {
            return new ManagerDashboardOverviewDto(
                    managedTeams.size(), 0, 0, 0.0,
                    0, 0, 0, 0, 0,
                    0, 0, 0, 0.0,
                    0,
                    Map.of("Unaware", 0, "Beginner", 0, "Intermediate", 0, "Advanced", 0, "Expert", 0),
                    List.of(), List.of(), List.of()
            );
        }

        List<EmployeeSkill> employeeSkills = getEmployeeSkillsForUsers(employeeIds);
        List<SkillGap> skillGaps = getSkillGapsForUsers(employeeIds);
        List<EmployeeLearning> learnings = getLearningsForUsers(employeeIds);
        List<ReassessmentRequest> reassessments = getReassessmentsForUsers(employeeIds);

        int totalAssessedSkills = (int) employeeSkills.stream().map(es -> es.getSkill().getId()).distinct().count();
        long totalPossibleAssessments = (long) employees.size() * Math.max(1, skillRepository.count());
        double coveragePercentage = totalPossibleAssessments > 0
                ? Math.min(100.0, Math.round(((double) employeeSkills.size() / totalPossibleAssessments) * 10000.0) / 100.0)
                : 0.0;

        int criticalGaps = (int) skillGaps.stream().filter(g -> g.getGapSize() >= 2 || "HIGH".equalsIgnoreCase(g.getPriority())).count();
        int highGaps = (int) skillGaps.stream().filter(g -> "HIGH".equalsIgnoreCase(g.getPriority()) && g.getGapSize() < 2).count();
        int mediumGaps = (int) skillGaps.stream().filter(g -> "MEDIUM".equalsIgnoreCase(g.getPriority())).count();
        int lowGaps = (int) skillGaps.stream().filter(g -> "LOW".equalsIgnoreCase(g.getPriority()) || "ON_TRACK".equalsIgnoreCase(g.getPriority())).count();

        int pendingReassessments = (int) reassessments.stream().filter(r -> "PENDING".equalsIgnoreCase(r.getStatus())).count();

        int activeEnrollments = (int) learnings.stream().filter(l -> "IN_PROGRESS".equalsIgnoreCase(l.getStatus()) || "ENROLLED".equalsIgnoreCase(l.getStatus())).count();
        int completedCourses = (int) learnings.stream().filter(l -> "COMPLETED".equalsIgnoreCase(l.getStatus()) || "CERTIFIED".equalsIgnoreCase(l.getStatus())).count();
        int totalLearnings = learnings.size();
        double courseCompletionRate = totalLearnings > 0
                ? Math.round(((double) completedCourses / totalLearnings) * 10000.0) / 100.0
                : 0.0;

        long activeMentorships = 0;
        for (Long empId : employeeIds) {
            if (mentorAssignmentRepository.existsByEmployeeIdAndStatus(empId, AssignmentStatus.ACTIVE)) {
                activeMentorships++;
            }
        }

        // Proficiency distribution
        Map<String, Integer> profDist = new HashMap<>();
        profDist.put("Unaware", 0);
        profDist.put("Beginner", 0);
        profDist.put("Intermediate", 0);
        profDist.put("Advanced", 0);
        profDist.put("Expert", 0);
        for (EmployeeSkill es : employeeSkills) {
            String p = es.getProficiencyLevel();
            if (p != null && profDist.containsKey(p)) {
                profDist.put(p, profDist.get(p) + 1);
            }
        }

        // Skill coverage summaries
        List<Skill> allSkills = skillRepository.findAll();
        List<ManagerDashboardOverviewDto.SkillCoverageSummary> summaries = new ArrayList<>();
        for (Skill s : allSkills) {
            List<EmployeeSkill> matchingSkills = employeeSkills.stream().filter(es -> es.getSkill().getId().equals(s.getId())).toList();
            if (matchingSkills.isEmpty()) continue;
            int countWithSkill = matchingSkills.size();
            double cov = Math.round(((double) countWithSkill / employees.size()) * 10000.0) / 100.0;
            int avgProf = (int) Math.round(matchingSkills.stream()
                    .mapToInt(es -> SkillService.PROFICIENCY_TO_NUMERIC.getOrDefault(es.getProficiencyLevel(), 1))
                    .average().orElse(1));
            summaries.add(new ManagerDashboardOverviewDto.SkillCoverageSummary(
                    s.getId(),
                    s.getName(),
                    s.getCategory() != null ? s.getCategory().getName() : "General",
                    countWithSkill,
                    employees.size(),
                    cov,
                    avgProf
            ));
        }
        summaries.sort((a, b) -> Double.compare(b.coveragePercentage(), a.coveragePercentage()));
        if (summaries.size() > 10) {
            summaries = summaries.subList(0, 10);
        }

        // Recent team activity
        List<ManagerDashboardOverviewDto.TeamActivityItem> activities = new ArrayList<>();
        List<AssessmentAttempt> attempts = assessmentAttemptRepository.findByUserIdsOrderByStartedAtDesc(employeeIds);
        for (AssessmentAttempt a : attempts.stream().limit(5).toList()) {
            activities.add(new ManagerDashboardOverviewDto.TeamActivityItem(
                    "asm-" + a.getId(),
                    "ASSESSMENT",
                    "Assessment Attempted: " + a.getSkill().getName(),
                    "Target: " + a.getTargetLevel() + " | Status: " + a.getStatus(),
                    a.getUser().getFullName(),
                    a.getStartedAt()
            ));
        }
        for (EmployeeLearning l : learnings.stream().sorted((a, b) -> b.getEnrolledAt().compareTo(a.getEnrolledAt())).limit(5).toList()) {
            activities.add(new ManagerDashboardOverviewDto.TeamActivityItem(
                    "lrn-" + l.getId(),
                    "ENROLLMENT",
                    "Course " + ( "COMPLETED".equalsIgnoreCase(l.getStatus()) ? "Completed" : "Enrolled" ) + ": " + l.getCourse().getTitle(),
                    "Progress: " + l.getProgressPct() + "% | Provider: " + l.getCourse().getProvider(),
                    l.getUser().getFullName(),
                    l.getEnrolledAt()
            ));
        }
        for (ReassessmentRequest r : reassessments.stream().limit(5).toList()) {
            activities.add(new ManagerDashboardOverviewDto.TeamActivityItem(
                    "reasm-" + r.getId(),
                    "REASSESSMENT",
                    "Reassessment " + r.getStatus() + ": " + r.getSkill().getName(),
                    "Reason: " + r.getReason(),
                    r.getEmployee().getFullName(),
                    r.getCreatedAt()
            ));
        }
        activities.sort((a, b) -> b.timestamp().compareTo(a.timestamp()));
        if (activities.size() > 10) {
            activities = activities.subList(0, 10);
        }

        // Recent alerts
        List<NotificationDto> alerts = notificationRepository.findByRecipientIdOrderByCreatedAtDesc(caller.getId())
                .stream().limit(10).map(n -> new NotificationDto(
                        n.getId(),
                        n.getType(),
                        n.getTitle(),
                        n.getMessage(),
                        n.getLink(),
                        n.isRead(),
                        n.getCreatedAt()
                )).toList();

        return new ManagerDashboardOverviewDto(
                managedTeams.size(),
                employees.size(),
                totalAssessedSkills,
                coveragePercentage,
                skillGaps.size(),
                criticalGaps,
                highGaps,
                mediumGaps,
                lowGaps,
                pendingReassessments,
                activeEnrollments,
                completedCourses,
                courseCompletionRate,
                (int) activeMentorships,
                profDist,
                summaries,
                activities,
                alerts
        );
    }

    // -------------------------------------------------------------
    // 3. Team Members Roster & Details
    // -------------------------------------------------------------

    @Transactional(readOnly = true)
    public List<ManagerTeamMemberDto> getTeamMembers(String callerEmail, Long teamIdFilter, String search) {
        User caller = getUserByEmail(callerEmail);
        List<TeamMembership> scopedMemberships = getScopedMemberships(caller, teamIdFilter);

        Map<Long, TeamMembership> userToMembership = new HashMap<>();
        for (TeamMembership tm : scopedMemberships) {
            if (tm.getUser().getRole() == Role.EMPLOYEE) {
                userToMembership.putIfAbsent(tm.getUser().getId(), tm);
            }
        }

        List<User> employees = new ArrayList<>(userToMembership.keySet().stream()
                .map(id -> userToMembership.get(id).getUser())
                .toList());

        if (search != null && !search.trim().isBlank()) {
            String q = search.trim().toLowerCase();
            employees = employees.stream().filter(u ->
                    u.getFullName().toLowerCase().contains(q) ||
                    u.getEmail().toLowerCase().contains(q) ||
                    (u.getEmployeeId() != null && u.getEmployeeId().toLowerCase().contains(q))
            ).toList();
        }

        List<Long> empIds = employees.stream().map(User::getId).toList();
        if (empIds.isEmpty()) return List.of();

        List<EmployeeSkill> skills = getEmployeeSkillsForUsers(empIds);
        List<SkillGap> gaps = getSkillGapsForUsers(empIds);
        List<EmployeeLearning> learnings = getLearningsForUsers(empIds);

        List<ManagerTeamMemberDto> result = new ArrayList<>();
        for (User u : employees) {
            TeamMembership tm = userToMembership.get(u.getId());
            EmployeeProfile p = profileRepository.findByUserId(u.getId()).orElse(null);

            int assessedCount = (int) skills.stream().filter(s -> s.getUser().getId().equals(u.getId())).count();
            List<SkillGap> userGaps = gaps.stream().filter(g -> g.getUser().getId().equals(u.getId())).toList();
            int totalGaps = userGaps.size();
            int criticalGaps = (int) userGaps.stream().filter(g -> g.getGapSize() >= 2 || "HIGH".equalsIgnoreCase(g.getPriority())).count();

            List<EmployeeLearning> userLearnings = learnings.stream().filter(l -> l.getUser().getId().equals(u.getId())).toList();
            int completed = (int) userLearnings.stream().filter(l -> "COMPLETED".equalsIgnoreCase(l.getStatus()) || "CERTIFIED".equalsIgnoreCase(l.getStatus())).count();
            int progressPct = userLearnings.isEmpty() ? 0 : (int) Math.round(userLearnings.stream().mapToInt(EmployeeLearning::getProgressPct).average().orElse(0));

            Optional<MentorAssignment> activeAssignment = mentorAssignmentRepository.findByEmployeeIdAndStatus(u.getId(), AssignmentStatus.ACTIVE);
            String mentorName = activeAssignment.map(a -> a.getMentor().getFullName()).orElse(null);

            result.add(new ManagerTeamMemberDto(
                    u.getId(),
                    u.getFullName(),
                    u.getEmail(),
                    u.getEmployeeId(),
                    p != null && p.getJobRole() != null ? p.getJobRole() : "Software Developer",
                    p != null && p.getDepartment() != null ? p.getDepartment() : (tm.getTeam().getDepartment() != null ? tm.getTeam().getDepartment().getName() : "General"),
                    tm.getTeam().getId(),
                    tm.getTeam().getName(),
                    tm.getRoleInTeam(),
                    assessedCount,
                    totalGaps,
                    criticalGaps,
                    progressPct,
                    completed,
                    mentorName,
                    u.getAccountStatus() == AccountStatus.ACTIVE
            ));
        }
        return result;
    }

    @Transactional(readOnly = true)
    public ManagerEmployeeDetailDto getEmployeeDetail(String callerEmail, Long employeeId) {
        User caller = getUserByEmail(callerEmail);
        validateEmployeeAccess(caller, employeeId);

        User employee = userRepository.findById(employeeId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Employee not found."));
        EmployeeProfile p = profileRepository.findByUserId(employeeId).orElse(null);

        List<TeamMembership> memberships = membershipRepository.findByUserId(employeeId);
        TeamMembership tm = memberships.isEmpty() ? null : memberships.get(0);

        List<EmployeeSkill> skills = employeeSkillRepository.findByUserId(employeeId);
        List<EmployeeSkillDto> skillDtos = skills.stream().map(this::toEmployeeSkillDto).toList();

        List<SkillGap> gaps = skillGapRepository.findByUserId(employeeId);
        List<SkillGapDto> gapDtos = gaps.stream().map(this::toSkillGapDto).toList();

        List<AssessmentAttempt> attempts = assessmentAttemptRepository.findByUserIdOrderByStartedAtDesc(employeeId);
        List<AssessmentAttemptDto> attemptDtos = attempts.stream().map(this::toAssessmentAttemptDto).toList();

        List<EmployeeLearning> learnings = learningRepository.findByUserId(employeeId);
        List<EmployeeLearningDto> learningDtos = learnings.stream().map(this::toEmployeeLearningDto).toList();

        Optional<MentorAssignment> activeAssignment = mentorAssignmentRepository.findByEmployeeIdAndStatus(employeeId, AssignmentStatus.ACTIVE);
        String mentorName = activeAssignment.map(a -> a.getMentor().getFullName()).orElse(null);
        Long mentorId = activeAssignment.map(a -> a.getMentor().getId()).orElse(null);

        List<ReassessmentRequest> reassessments = reassessmentRequestRepository.findByEmployeeIdOrderByCreatedAtDesc(employeeId);
        List<ReassessmentRequestDto> reassessmentDtos = reassessments.stream().map(this::toReassessmentRequestDto).toList();

        List<ManagerCourseRecommendation> courseRecs = managerCourseRecommendationRepository.findByEmployeeIdOrderByCreatedAtDesc(employeeId);
        List<ManagerCourseRecommendationDto> recDtos = courseRecs.stream().map(this::toCourseRecommendationDto).toList();

        return new ManagerEmployeeDetailDto(
                employee.getId(),
                employee.getFullName(),
                employee.getEmail(),
                employee.getEmployeeId(),
                p != null && p.getJobRole() != null ? p.getJobRole() : "Software Developer",
                p != null && p.getDepartment() != null ? p.getDepartment() : (tm != null && tm.getTeam().getDepartment() != null ? tm.getTeam().getDepartment().getName() : "General"),
                tm != null ? tm.getTeam().getId() : null,
                tm != null ? tm.getTeam().getName() : "Unassigned",
                tm != null ? tm.getRoleInTeam() : "Member",
                p != null ? p.getExperienceLevel() : "Not specified",
                p != null ? p.getBio() : "",
                skillDtos,
                gapDtos,
                attemptDtos,
                learningDtos,
                mentorName,
                mentorId,
                reassessmentDtos,
                recDtos
        );
    }

    // -------------------------------------------------------------
    // 4. Team Skill Inventory
    // -------------------------------------------------------------

    @Transactional(readOnly = true)
    public List<TeamSkillInventoryDto> getSkillInventory(String callerEmail, Long teamIdFilter, Long categoryId, String search) {
        User caller = getUserByEmail(callerEmail);
        List<User> employees = getScopedEmployees(caller, teamIdFilter);
        if (employees.isEmpty()) return List.of();

        List<Long> employeeIds = employees.stream().map(User::getId).toList();
        List<EmployeeSkill> employeeSkills = getEmployeeSkillsForUsers(employeeIds);
        List<Skill> allSkills = skillRepository.findAll();

        if (categoryId != null) {
            allSkills = allSkills.stream().filter(s -> s.getCategory() != null && s.getCategory().getId().equals(categoryId)).toList();
        }
        if (search != null && !search.trim().isBlank()) {
            String q = search.trim().toLowerCase();
            allSkills = allSkills.stream().filter(s -> s.getName().toLowerCase().contains(q)).toList();
        }

        List<TeamSkillInventoryDto> result = new ArrayList<>();
        for (Skill s : allSkills) {
            List<EmployeeSkill> matching = employeeSkills.stream().filter(es -> es.getSkill().getId().equals(s.getId())).toList();

            int unaware = 0, beginner = 0, intermediate = 0, advanced = 0, expert = 0;
            for (EmployeeSkill es : matching) {
                switch (es.getProficiencyLevel()) {
                    case "Unaware" -> unaware++;
                    case "Beginner" -> beginner++;
                    case "Intermediate" -> intermediate++;
                    case "Advanced" -> advanced++;
                    case "Expert" -> expert++;
                }
            }

            int count = matching.size();
            double avgLevel = count > 0 ? matching.stream()
                    .mapToInt(es -> SkillService.PROFICIENCY_TO_NUMERIC.getOrDefault(es.getProficiencyLevel(), 1))
                    .average().orElse(1.0) : 1.0;
            avgLevel = Math.round(avgLevel * 10.0) / 10.0;
            int roundedLevel = (int) Math.round(avgLevel);
            String avgName = SkillService.NUMERIC_TO_PROFICIENCY.getOrDefault(roundedLevel, "Unaware");

            // Look up standard requirement if available, else default to Intermediate (3)
            int reqLevel = 3;
            String reqName = "Intermediate";

            String status = (avgLevel >= reqLevel + 0.5) ? "EXCEEDED" : (avgLevel >= reqLevel ? "MET" : "GAP");

            result.add(new TeamSkillInventoryDto(
                    s.getId(),
                    s.getName(),
                    s.getCategory() != null ? s.getCategory().getName() : "General",
                    count,
                    avgLevel,
                    avgName,
                    reqLevel,
                    reqName,
                    unaware,
                    beginner,
                    intermediate,
                    advanced,
                    expert,
                    status
            ));
        }
        return result;
    }

    // -------------------------------------------------------------
    // 5. Team Gap Analysis
    // -------------------------------------------------------------

    @Transactional(readOnly = true)
    public List<TeamGapAnalysisDto> getGapAnalysis(String callerEmail, Long teamIdFilter, String priority, Long categoryId, String search) {
        User caller = getUserByEmail(callerEmail);
        List<TeamMembership> memberships = getScopedMemberships(caller, teamIdFilter);
        Map<Long, Team> userTeamMap = new HashMap<>();
        for (TeamMembership tm : memberships) {
            userTeamMap.putIfAbsent(tm.getUser().getId(), tm.getTeam());
        }

        List<Long> employeeIds = userTeamMap.keySet().stream().toList();
        if (employeeIds.isEmpty()) return List.of();

        List<SkillGap> gaps = getSkillGapsForUsers(employeeIds);
        List<TeamGapAnalysisDto> result = new ArrayList<>();

        for (SkillGap g : gaps) {
            if (priority != null && !priority.isBlank() && !priority.equalsIgnoreCase(g.getPriority())) {
                continue;
            }
            if (categoryId != null && (g.getSkill().getCategory() == null || !g.getSkill().getCategory().getId().equals(categoryId))) {
                continue;
            }
            if (search != null && !search.isBlank()) {
                String q = search.trim().toLowerCase();
                boolean matches = g.getUser().getFullName().toLowerCase().contains(q) ||
                                  g.getSkill().getName().toLowerCase().contains(q);
                if (!matches) continue;
            }

            Team t = userTeamMap.get(g.getUser().getId());
            result.add(new TeamGapAnalysisDto(
                    g.getUser().getId(),
                    g.getUser().getFullName(),
                    t != null ? t.getId() : null,
                    t != null ? t.getName() : "Unassigned",
                    g.getSkill().getId(),
                    g.getSkill().getName(),
                    g.getSkill().getCategory() != null ? g.getSkill().getCategory().getName() : "General",
                    g.getCurrentLevel(),
                    SkillService.NUMERIC_TO_PROFICIENCY.getOrDefault(g.getCurrentLevel(), "Unaware"),
                    g.getRequiredLevel(),
                    SkillService.NUMERIC_TO_PROFICIENCY.getOrDefault(g.getRequiredLevel(), "Beginner"),
                    g.getGapSize(),
                    g.getPriority(),
                    g.getCalculatedAt()
            ));
        }

        result.sort((a, b) -> Integer.compare(b.gapSize(), a.gapSize()));
        return result;
    }

    // -------------------------------------------------------------
    // 6. Skill Gap Heatmap
    // -------------------------------------------------------------

    @Transactional(readOnly = true)
    public ManagerHeatmapDto getHeatmap(String callerEmail, Long teamIdFilter) {
        User caller = getUserByEmail(callerEmail);
        List<TeamMembership> scopedMemberships = getScopedMemberships(caller, teamIdFilter);

        Map<Long, TeamMembership> userToMembership = new HashMap<>();
        for (TeamMembership tm : scopedMemberships) {
            if (tm.getUser().getRole() == Role.EMPLOYEE) {
                userToMembership.putIfAbsent(tm.getUser().getId(), tm);
            }
        }

        List<User> employees = new ArrayList<>(userToMembership.keySet().stream().map(id -> userToMembership.get(id).getUser()).toList());
        List<Long> employeeIds = employees.stream().map(User::getId).toList();

        List<Skill> allSkills = skillRepository.findAll();
        List<ManagerHeatmapDto.HeatmapSkill> heatmapSkills = allSkills.stream()
                .map(s -> new ManagerHeatmapDto.HeatmapSkill(s.getId(), s.getName(), s.getCategory() != null ? s.getCategory().getName() : "General"))
                .toList();

        if (employeeIds.isEmpty()) {
            return new ManagerHeatmapDto(
                    new ManagerHeatmapDto.EmployeeSkillMatrix(List.of(), heatmapSkills, List.of()),
                    new ManagerHeatmapDto.SkillTeamMatrix(List.of(), heatmapSkills, List.of())
            );
        }

        List<EmployeeSkill> employeeSkills = getEmployeeSkillsForUsers(employeeIds);
        List<SkillGap> gaps = getSkillGapsForUsers(employeeIds);

        Map<String, EmployeeSkill> empSkillLookup = new HashMap<>();
        for (EmployeeSkill es : employeeSkills) {
            empSkillLookup.put(es.getUser().getId() + "_" + es.getSkill().getId(), es);
        }

        Map<String, SkillGap> gapLookup = new HashMap<>();
        for (SkillGap g : gaps) {
            gapLookup.put(g.getUser().getId() + "_" + g.getSkill().getId(), g);
        }

        // View A: Employee vs Skill Cells
        List<ManagerHeatmapDto.HeatmapEmployee> heatmapEmployees = employees.stream().map(u -> {
            TeamMembership tm = userToMembership.get(u.getId());
            return new ManagerHeatmapDto.HeatmapEmployee(u.getId(), u.getFullName(), tm.getTeam().getId(), tm.getTeam().getName());
        }).toList();

        List<ManagerHeatmapDto.EmployeeSkillCell> empSkillCells = new ArrayList<>();
        for (User u : employees) {
            for (Skill s : allSkills) {
                String key = u.getId() + "_" + s.getId();
                EmployeeSkill es = empSkillLookup.get(key);
                SkillGap sg = gapLookup.get(key);

                if (es == null && sg == null) {
                    empSkillCells.add(new ManagerHeatmapDto.EmployeeSkillCell(
                            u.getId(), s.getId(), 0, "Not Assessed", 0, "N/A", 0, "NO_DATA"
                    ));
                    continue;
                }

                int currentLevel = es != null ? SkillService.PROFICIENCY_TO_NUMERIC.getOrDefault(es.getProficiencyLevel(), 1) : (sg != null ? sg.getCurrentLevel() : 1);
                String currentName = SkillService.NUMERIC_TO_PROFICIENCY.getOrDefault(currentLevel, "Unaware");
                int reqLevel = sg != null ? sg.getRequiredLevel() : 3;
                String reqName = SkillService.NUMERIC_TO_PROFICIENCY.getOrDefault(reqLevel, "Intermediate");
                int gapSize = sg != null ? sg.getGapSize() : Math.max(0, reqLevel - currentLevel);

                String severity;
                if (gapSize >= 2) severity = "CRITICAL";
                else if (gapSize == 1) severity = "MODERATE";
                else if (currentLevel > reqLevel) severity = "EXCEEDED";
                else severity = "MET";

                empSkillCells.add(new ManagerHeatmapDto.EmployeeSkillCell(
                        u.getId(), s.getId(), currentLevel, currentName, reqLevel, reqName, gapSize, severity
                ));
            }
        }

        // View B: Skill vs Team Cells
        List<Team> managedTeams = (teamIdFilter != null)
                ? List.of(teamRepository.findById(teamIdFilter).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Team not found.")))
                : getManagedTeams(caller);

        List<ManagerHeatmapDto.HeatmapTeam> heatmapTeams = managedTeams.stream()
                .map(t -> new ManagerHeatmapDto.HeatmapTeam(t.getId(), t.getName()))
                .toList();

        List<ManagerHeatmapDto.SkillTeamCell> skillTeamCells = new ArrayList<>();
        for (Team t : managedTeams) {
            List<TeamMembership> teamMemberships = membershipRepository.findByTeamId(t.getId());
            List<Long> memberIds = teamMemberships.stream()
                    .map(tm -> tm.getUser().getId())
                    .filter(id -> employeeIds.contains(id))
                    .toList();

            for (Skill s : allSkills) {
                if (memberIds.isEmpty()) {
                    skillTeamCells.add(new ManagerHeatmapDto.SkillTeamCell(t.getId(), s.getId(), 0.0, "N/A", 0.0, 0));
                    continue;
                }

                List<EmployeeSkill> teamSkillRecords = employeeSkills.stream()
                        .filter(es -> memberIds.contains(es.getUser().getId()) && es.getSkill().getId().equals(s.getId()))
                        .toList();

                int assessedMembers = teamSkillRecords.size();
                double coveragePct = Math.round(((double) assessedMembers / memberIds.size()) * 10000.0) / 100.0;
                double avgLevel = assessedMembers > 0
                        ? teamSkillRecords.stream().mapToInt(es -> SkillService.PROFICIENCY_TO_NUMERIC.getOrDefault(es.getProficiencyLevel(), 1)).average().orElse(1.0)
                        : 0.0;
                avgLevel = Math.round(avgLevel * 10.0) / 10.0;
                String avgProf = SkillService.NUMERIC_TO_PROFICIENCY.getOrDefault((int) Math.round(avgLevel), "Unaware");

                int teamGaps = (int) gaps.stream()
                        .filter(g -> memberIds.contains(g.getUser().getId()) && g.getSkill().getId().equals(s.getId()) && g.getGapSize() > 0)
                        .count();

                skillTeamCells.add(new ManagerHeatmapDto.SkillTeamCell(
                        t.getId(), s.getId(), avgLevel, avgProf, coveragePct, teamGaps
                ));
            }
        }

        return new ManagerHeatmapDto(
                new ManagerHeatmapDto.EmployeeSkillMatrix(heatmapEmployees, heatmapSkills, empSkillCells),
                new ManagerHeatmapDto.SkillTeamMatrix(heatmapTeams, heatmapSkills, skillTeamCells)
        );
    }

    // -------------------------------------------------------------
    // 7. Employee Assessments & Reassessment Requests
    // -------------------------------------------------------------

    @Transactional(readOnly = true)
    public List<AssessmentAttemptDto> getTeamAssessments(String callerEmail, Long teamIdFilter) {
        User caller = getUserByEmail(callerEmail);
        List<User> employees = getScopedEmployees(caller, teamIdFilter);
        List<Long> employeeIds = employees.stream().map(User::getId).toList();
        if (employeeIds.isEmpty()) return List.of();

        List<AssessmentAttempt> attempts = assessmentAttemptRepository.findByUserIdsOrderByStartedAtDesc(employeeIds);
        return attempts.stream().map(this::toAssessmentAttemptDto).toList();
    }

    @Transactional(readOnly = true)
    public List<ReassessmentRequestDto> getReassessmentRequests(String callerEmail, Long teamIdFilter) {
        User caller = getUserByEmail(callerEmail);
        List<User> employees = getScopedEmployees(caller, teamIdFilter);
        List<Long> employeeIds = employees.stream().map(User::getId).toList();
        if (employeeIds.isEmpty()) return List.of();

        List<ReassessmentRequest> requests = reassessmentRequestRepository.findByEmployeeIdIn(employeeIds);
        return requests.stream().map(this::toReassessmentRequestDto).toList();
    }

    @Transactional
    public ReassessmentRequestDto createReassessmentRequest(String callerEmail, CreateReassessmentRequest req) {
        User caller = getUserByEmail(callerEmail);
        validateEmployeeAccess(caller, req.employeeId());

        User employee = userRepository.findById(req.employeeId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Employee not found."));
        Skill skill = skillRepository.findById(req.skillId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Skill not found."));

        ReassessmentRequest r = new ReassessmentRequest(caller, employee, skill, req.reason().trim());
        ReassessmentRequest saved = reassessmentRequestRepository.save(r);

        notificationService.createNotification(
                employee,
                "REASSESSMENT_REQUESTED",
                "Reassessment Requested",
                "Your manager " + caller.getFullName() + " requested a skill reassessment for " + skill.getName() + ". Note: " + req.reason().trim(),
                "/dashboard/employee?tab=assessment"
        );

        return toReassessmentRequestDto(saved);
    }

    @Transactional
    public void cancelReassessmentRequest(String callerEmail, Long requestId) {
        User caller = getUserByEmail(callerEmail);
        ReassessmentRequest r = reassessmentRequestRepository.findById(requestId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Reassessment request not found."));

        validateEmployeeAccess(caller, r.getEmployee().getId());
        r.setStatus("CANCELLED");
        r.setUpdatedAt(Instant.now());
        reassessmentRequestRepository.save(r);
    }

    // -------------------------------------------------------------
    // 8. Learning Recommendations & Manager Course Recommendations
    // -------------------------------------------------------------

    @Transactional(readOnly = true)
    public RecommendationResponse getTeamAiRecommendations(String callerEmail, Long teamIdFilter) {
        User caller = getUserByEmail(callerEmail);
        List<User> employees = getScopedEmployees(caller, teamIdFilter);
        List<Long> employeeIds = employees.stream().map(User::getId).toList();

        if (employeeIds.isEmpty()) {
            return new RecommendationResponse("No recommendations available.", List.of(), "rules", "No team members found for learning recommendations.");
        }

        List<SkillGap> gaps = getSkillGapsForUsers(employeeIds);
        if (gaps.isEmpty()) {
            return new RecommendationResponse("Team capability analysis complete.", List.of(), "rules", "No active team skill gaps identified.");
        }

        // Group gaps by skill to see top gap areas
        Map<Skill, Integer> skillDeficits = new HashMap<>();
        for (SkillGap g : gaps) {
            skillDeficits.put(g.getSkill(), skillDeficits.getOrDefault(g.getSkill(), 0) + g.getGapSize());
        }

        List<SkillResult> skillResults = skillDeficits.entrySet().stream()
                .sorted((a, b) -> Integer.compare(b.getValue(), a.getValue()))
                .limit(5)
                .map(e -> new SkillResult(
                        e.getKey().getName(),
                        1,
                        5,
                        2,
                        4
                )).toList();

        RecommendationRequest req = new RecommendationRequest(
                "Software Developer",
                "Intermediate",
                "Engineering",
                70,
                skillResults
        );

        return geminiRecommendationService.recommend(caller.getEmail(), req);
    }

    @Transactional(readOnly = true)
    public List<ManagerCourseRecommendationDto> getCourseRecommendations(String callerEmail, Long teamIdFilter) {
        User caller = getUserByEmail(callerEmail);
        List<User> employees = getScopedEmployees(caller, teamIdFilter);
        List<Long> employeeIds = employees.stream().map(User::getId).toList();
        if (employeeIds.isEmpty()) return List.of();

        List<ManagerCourseRecommendation> recs = managerCourseRecommendationRepository.findByEmployeeIdIn(employeeIds);
        return recs.stream().map(this::toCourseRecommendationDto).toList();
    }

    @Transactional
    public ManagerCourseRecommendationDto recommendCourse(String callerEmail, CreateManagerCourseRecommendationRequest req) {
        User caller = getUserByEmail(callerEmail);
        validateEmployeeAccess(caller, req.employeeId());

        User employee = userRepository.findById(req.employeeId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Employee not found."));
        Course course = courseRepository.findById(req.courseId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Course not found."));
        Skill skill = req.skillId() != null
                ? skillRepository.findById(req.skillId()).orElse(course.getSkill())
                : course.getSkill();

        ManagerCourseRecommendation r = new ManagerCourseRecommendation(caller, employee, course, skill, req.reason().trim());
        ManagerCourseRecommendation saved = managerCourseRecommendationRepository.save(r);

        notificationService.createNotification(
                employee,
                "COURSE_RECOMMENDED",
                "Course Recommended by Manager",
                caller.getFullName() + " recommended the course '" + course.getTitle() + "'. Note: " + req.reason().trim(),
                "/dashboard/employee?tab=learning"
        );

        return toCourseRecommendationDto(saved);
    }

    // -------------------------------------------------------------
    // 9. Learning Progress
    // -------------------------------------------------------------

    @Transactional(readOnly = true)
    public List<EmployeeLearningDto> getLearningProgress(String callerEmail, Long teamIdFilter, String status, String search) {
        User caller = getUserByEmail(callerEmail);
        List<User> employees = getScopedEmployees(caller, teamIdFilter);
        List<Long> employeeIds = employees.stream().map(User::getId).toList();
        if (employeeIds.isEmpty()) return List.of();

        List<EmployeeLearning> learnings = learningRepository.findByUserIds(employeeIds);

        if (status != null && !status.isBlank()) {
            learnings = learnings.stream().filter(l -> status.equalsIgnoreCase(l.getStatus())).toList();
        }
        if (search != null && !search.isBlank()) {
            String q = search.trim().toLowerCase();
            learnings = learnings.stream().filter(l ->
                    l.getUser().getFullName().toLowerCase().contains(q) ||
                    l.getCourse().getTitle().toLowerCase().contains(q) ||
                    (l.getCourse().getSkill() != null && l.getCourse().getSkill().getName().toLowerCase().contains(q))
            ).toList();
        }

        return learnings.stream().map(this::toEmployeeLearningDto).toList();
    }

    // -------------------------------------------------------------
    // 10. Mentorship Support
    // -------------------------------------------------------------

    @Transactional(readOnly = true)
    public ManagerMentorshipStatusDto getMentorshipSupport(String callerEmail, Long teamIdFilter) {
        User caller = getUserByEmail(callerEmail);
        List<User> employees = getScopedEmployees(caller, teamIdFilter);
        List<Long> employeeIds = employees.stream().map(User::getId).toList();

        List<AdminMentorDto> availableMentors = adminMentorshipService.listMentors();

        if (employeeIds.isEmpty()) {
            return new ManagerMentorshipStatusDto(0, 0, 0, List.of(), List.of(), availableMentors);
        }

        List<ManagerMentorshipStatusDto.ActiveMentorshipItem> activeItems = new ArrayList<>();
        Set<Long> mentoredEmpIds = new HashSet<>();

        for (User u : employees) {
            Optional<MentorAssignment> a = mentorAssignmentRepository.findByEmployeeIdAndStatus(u.getId(), AssignmentStatus.ACTIVE);
            if (a.isPresent()) {
                MentorAssignment ma = a.get();
                mentoredEmpIds.add(u.getId());
                activeItems.add(new ManagerMentorshipStatusDto.ActiveMentorshipItem(
                        ma.getId(),
                        u.getId(),
                        u.getFullName(),
                        u.getEmail(),
                        ma.getMentor().getId(),
                        ma.getMentor().getFullName(),
                        ma.getMentor().getEmail(),
                        ma.getNotes() != null ? ma.getNotes() : "Skill Development",
                        ma.getStatus().name(),
                        ma.getAssignedAt()
                ));
            }
        }

        int totalEmployees = employees.size();
        int mentoredCount = mentoredEmpIds.size();
        int unmentoredCount = totalEmployees - mentoredCount;

        // Find mentorship opportunities: employees with skill gaps matched with mentors who have that expertise
        List<SkillGap> gaps = getSkillGapsForUsers(employeeIds);
        List<ManagerMentorshipStatusDto.MentorshipOpportunityItem> opportunities = new ArrayList<>();

        for (SkillGap g : gaps) {
            if (g.getGapSize() <= 0) continue;
            String skillName = g.getSkill().getName().toLowerCase();
            List<AdminMentorDto> matching = availableMentors.stream()
                    .filter(m -> m.isActive() && m.expertiseAreas().stream().anyMatch(area -> area.toLowerCase().contains(skillName)))
                    .toList();

            opportunities.add(new ManagerMentorshipStatusDto.MentorshipOpportunityItem(
                    g.getUser().getId(),
                    g.getUser().getFullName(),
                    g.getSkill().getId(),
                    g.getSkill().getName(),
                    g.getCurrentLevel(),
                    g.getRequiredLevel(),
                    g.getPriority(),
                    matching
            ));
        }

        return new ManagerMentorshipStatusDto(
                totalEmployees,
                mentoredCount,
                unmentoredCount,
                activeItems,
                opportunities,
                availableMentors
        );
    }

    @Transactional
    public void referMentor(String callerEmail, ManagerMentorReferralRequest req) {
        User caller = getUserByEmail(callerEmail);
        validateEmployeeAccess(caller, req.employeeId());

        User employee = userRepository.findById(req.employeeId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Employee not found."));
        User mentor = userRepository.findById(req.mentorId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Mentor not found."));
        Skill skill = req.skillId() != null ? skillRepository.findById(req.skillId()).orElse(null) : null;

        String skillText = (skill != null) ? skill.getName() : "skill advancement";

        // Notify mentor
        notificationService.createNotification(
                mentor,
                "MENTOR_REFERRAL",
                "Manager Mentorship Referral",
                caller.getFullName() + " referred employee " + employee.getFullName() + " for mentorship in " + skillText + ". Note: " + req.note(),
                "/dashboard/mentor"
        );

        // Notify employee
        notificationService.createNotification(
                employee,
                "MENTOR_REFERRAL",
                "Mentor Referral from Manager",
                "Your manager " + caller.getFullName() + " recommended connecting with mentor " + mentor.getFullName() + " for " + skillText + ". Note: " + req.note(),
                "/dashboard/employee?tab=mentors"
        );
    }

    // -------------------------------------------------------------
    // 11. Reports (8 Types + CSV)
    // -------------------------------------------------------------

    @Transactional(readOnly = true)
    public ManagerReportDto getReport(String callerEmail, String reportType, Long teamIdFilter) {
        User caller = getUserByEmail(callerEmail);
        Team team = teamIdFilter != null
                ? teamRepository.findById(teamIdFilter).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Team not found."))
                : null;
        if (team != null) validateTeamAccess(caller, team.getId());

        String teamName = team != null ? team.getName() : "All Managed Teams";
        List<User> employees = getScopedEmployees(caller, teamIdFilter);
        List<Long> employeeIds = employees.stream().map(User::getId).toList();

        String type = (reportType == null || reportType.isBlank()) ? "TEAM_SKILL_GAP_SUMMARY" : reportType.toUpperCase();

        return switch (type) {
            case "TEAM_SKILL_GAP_SUMMARY" -> generateSkillGapSummaryReport(teamIdFilter, teamName, caller, employees, employeeIds);
            case "TEAM_PROFICIENCY_MATRIX" -> generateProficiencyMatrixReport(teamIdFilter, teamName, caller, employees, employeeIds);
            case "ASSESSMENT_COMPLETION_PERFORMANCE" -> generateAssessmentReport(teamIdFilter, teamName, caller, employees, employeeIds);
            case "COURSE_PROGRESS_COMPLIANCE" -> generateCourseComplianceReport(teamIdFilter, teamName, caller, employees, employeeIds);
            case "MENTORSHIP_ENGAGEMENT" -> generateMentorshipEngagementReport(teamIdFilter, teamName, caller, employees, employeeIds);
            case "SKILL_INVENTORY_CATEGORY" -> generateSkillInventoryCategoryReport(teamIdFilter, teamName, caller, employees, employeeIds);
            case "CRITICAL_SKILL_GAPS" -> generateCriticalSkillGapsReport(teamIdFilter, teamName, caller, employees, employeeIds);
            case "TEAM_DEVELOPMENT_LOG" -> generateTeamDevelopmentLogReport(teamIdFilter, teamName, caller, employees, employeeIds);
            default -> generateSkillGapSummaryReport(teamIdFilter, teamName, caller, employees, employeeIds);
        };
    }

    private ManagerReportDto generateSkillGapSummaryReport(Long teamId, String teamName, User caller, List<User> employees, List<Long> employeeIds) {
        List<SkillGap> gaps = employeeIds.isEmpty() ? List.of() : getSkillGapsForUsers(employeeIds);
        List<String> cols = List.of("Employee", "Employee ID", "Skill", "Category", "Current Level", "Required Level", "Gap Size", "Priority", "Calculated Date");
        List<Map<String, Object>> rows = new ArrayList<>();

        int critical = 0;
        int high = 0;
        int medium = 0;
        int low = 0;
        double totalGapSize = 0;

        DateTimeFormatter fmt = DateTimeFormatter.ISO_INSTANT;
        for (SkillGap g : gaps) {
            if (g.getGapSize() >= 2) critical++;
            else if ("HIGH".equalsIgnoreCase(g.getPriority())) high++;
            else if ("MEDIUM".equalsIgnoreCase(g.getPriority())) medium++;
            else low++;
            totalGapSize += g.getGapSize();

            rows.add(Map.of(
                    "Employee", g.getUser().getFullName(),
                    "Employee ID", g.getUser().getEmployeeId() != null ? g.getUser().getEmployeeId() : "N/A",
                    "Skill", g.getSkill().getName(),
                    "Category", g.getSkill().getCategory() != null ? g.getSkill().getCategory().getName() : "General",
                    "Current Level", SkillService.NUMERIC_TO_PROFICIENCY.getOrDefault(g.getCurrentLevel(), "Unaware"),
                    "Required Level", SkillService.NUMERIC_TO_PROFICIENCY.getOrDefault(g.getRequiredLevel(), "Beginner"),
                    "Gap Size", g.getGapSize(),
                    "Priority", g.getPriority(),
                    "Calculated Date", fmt.format(g.getCalculatedAt())
            ));
        }

        Map<String, Object> summary = Map.of(
                "totalGaps", gaps.size(),
                "criticalGaps", critical,
                "highGaps", high,
                "mediumGaps", medium,
                "lowGaps", low,
                "averageGapSize", gaps.isEmpty() ? 0.0 : Math.round((totalGapSize / gaps.size()) * 10.0) / 10.0
        );

        return new ManagerReportDto(
                "TEAM_SKILL_GAP_SUMMARY",
                "Team Skill Gap Summary",
                Instant.now(),
                teamId,
                teamName,
                caller.getFullName(),
                rows.size(),
                summary,
                cols,
                rows
        );
    }

    private ManagerReportDto generateProficiencyMatrixReport(Long teamId, String teamName, User caller, List<User> employees, List<Long> employeeIds) {
        List<EmployeeSkill> skills = employeeIds.isEmpty() ? List.of() : employeeSkillRepository.findByUserIds(employeeIds);
        List<String> cols = List.of("Employee", "Employee ID", "Job Role", "Assessed Skills Count", "Average Level", "Proficiency Status");
        List<Map<String, Object>> rows = new ArrayList<>();

        double teamTotalLevel = 0;
        int totalAssessedRecords = 0;

        for (User u : employees) {
            EmployeeProfile p = profileRepository.findByUserId(u.getId()).orElse(null);
            List<EmployeeSkill> uSkills = skills.stream().filter(s -> s.getUser().getId().equals(u.getId())).toList();
            int count = uSkills.size();
            double avg = count > 0 ? uSkills.stream().mapToInt(s -> SkillService.PROFICIENCY_TO_NUMERIC.getOrDefault(s.getProficiencyLevel(), 1)).average().orElse(1.0) : 1.0;
            avg = Math.round(avg * 10.0) / 10.0;
            String status = SkillService.NUMERIC_TO_PROFICIENCY.getOrDefault((int) Math.round(avg), "Unaware");

            teamTotalLevel += (avg * count);
            totalAssessedRecords += count;

            rows.add(Map.of(
                    "Employee", u.getFullName(),
                    "Employee ID", u.getEmployeeId() != null ? u.getEmployeeId() : "N/A",
                    "Job Role", p != null && p.getJobRole() != null ? p.getJobRole() : "Software Developer",
                    "Assessed Skills Count", count,
                    "Average Level", avg,
                    "Proficiency Status", status
            ));
        }

        Map<String, Object> summary = Map.of(
                "totalEmployees", employees.size(),
                "totalAssessedRecords", totalAssessedRecords,
                "teamAverageProficiency", totalAssessedRecords > 0 ? Math.round((teamTotalLevel / totalAssessedRecords) * 10.0) / 10.0 : 0.0
        );

        return new ManagerReportDto(
                "TEAM_PROFICIENCY_MATRIX",
                "Team Proficiency Matrix",
                Instant.now(),
                teamId,
                teamName,
                caller.getFullName(),
                rows.size(),
                summary,
                cols,
                rows
        );
    }

    private ManagerReportDto generateAssessmentReport(Long teamId, String teamName, User caller, List<User> employees, List<Long> employeeIds) {
        List<AssessmentAttempt> attempts = employeeIds.isEmpty() ? List.of() : assessmentAttemptRepository.findByUserIdsOrderByStartedAtDesc(employeeIds);
        List<String> cols = List.of("Employee", "Skill", "Category", "Target Level", "Status", "Source", "Questions", "Started At");
        List<Map<String, Object>> rows = new ArrayList<>();

        int completed = 0;
        DateTimeFormatter fmt = DateTimeFormatter.ISO_INSTANT;
        for (AssessmentAttempt a : attempts) {
            if ("COMPLETED".equalsIgnoreCase(a.getStatus())) completed++;
            rows.add(Map.of(
                    "Employee", a.getUser().getFullName(),
                    "Skill", a.getSkill().getName(),
                    "Category", a.getSkill().getCategory() != null ? a.getSkill().getCategory().getName() : "General",
                    "Target Level", a.getTargetLevel(),
                    "Status", a.getStatus(),
                    "Source", a.getGenerationSource() != null ? a.getGenerationSource() : "AI_DYNAMIC",
                    "Questions", a.getTotalQuestions(),
                    "Started At", fmt.format(a.getStartedAt())
            ));
        }

        Map<String, Object> summary = Map.of(
                "totalAttempts", attempts.size(),
                "completedAttempts", completed,
                "completionRate", attempts.isEmpty() ? 0.0 : Math.round(((double) completed / attempts.size()) * 10000.0) / 100.0
        );

        return new ManagerReportDto(
                "ASSESSMENT_COMPLETION_PERFORMANCE",
                "Assessment Completion & Performance Report",
                Instant.now(),
                teamId,
                teamName,
                caller.getFullName(),
                rows.size(),
                summary,
                cols,
                rows
        );
    }

    private ManagerReportDto generateCourseComplianceReport(Long teamId, String teamName, User caller, List<User> employees, List<Long> employeeIds) {
        List<EmployeeLearning> learnings = employeeIds.isEmpty() ? List.of() : learningRepository.findByUserIds(employeeIds);
        List<String> cols = List.of("Employee", "Course Title", "Skill", "Provider", "Level", "Status", "Progress Pct", "Enrolled Date");
        List<Map<String, Object>> rows = new ArrayList<>();

        int completed = 0;
        int inProgress = 0;
        DateTimeFormatter fmt = DateTimeFormatter.ISO_INSTANT;
        for (EmployeeLearning l : learnings) {
            if ("COMPLETED".equalsIgnoreCase(l.getStatus()) || "CERTIFIED".equalsIgnoreCase(l.getStatus())) completed++;
            else inProgress++;

            rows.add(Map.of(
                    "Employee", l.getUser().getFullName(),
                    "Course Title", l.getCourse().getTitle(),
                    "Skill", l.getCourse().getSkill() != null ? l.getCourse().getSkill().getName() : "General",
                    "Provider", l.getCourse().getProvider(),
                    "Level", l.getCourse().getLevel(),
                    "Status", l.getStatus(),
                    "Progress Pct", l.getProgressPct() + "%",
                    "Enrolled Date", fmt.format(l.getEnrolledAt())
            ));
        }

        Map<String, Object> summary = Map.of(
                "totalEnrollments", learnings.size(),
                "completedCourses", completed,
                "inProgressCourses", inProgress,
                "complianceRate", learnings.isEmpty() ? 0.0 : Math.round(((double) completed / learnings.size()) * 10000.0) / 100.0
        );

        return new ManagerReportDto(
                "COURSE_PROGRESS_COMPLIANCE",
                "Course Progress & Compliance Report",
                Instant.now(),
                teamId,
                teamName,
                caller.getFullName(),
                rows.size(),
                summary,
                cols,
                rows
        );
    }

    private ManagerReportDto generateMentorshipEngagementReport(Long teamId, String teamName, User caller, List<User> employees, List<Long> employeeIds) {
        List<String> cols = List.of("Employee", "Employee Email", "Mentor Name", "Mentor Email", "Status", "Focus Area", "Assigned Date");
        List<Map<String, Object>> rows = new ArrayList<>();

        int activeCount = 0;
        DateTimeFormatter fmt = DateTimeFormatter.ISO_INSTANT;
        for (User u : employees) {
            Optional<MentorAssignment> a = mentorAssignmentRepository.findByEmployeeIdAndStatus(u.getId(), AssignmentStatus.ACTIVE);
            if (a.isPresent()) {
                activeCount++;
                MentorAssignment ma = a.get();
                rows.add(Map.of(
                        "Employee", u.getFullName(),
                        "Employee Email", u.getEmail(),
                        "Mentor Name", ma.getMentor().getFullName(),
                        "Mentor Email", ma.getMentor().getEmail(),
                        "Status", ma.getStatus().name(),
                        "Focus Area", ma.getNotes() != null ? ma.getNotes() : "Mentorship",
                        "Assigned Date", fmt.format(ma.getAssignedAt())
                ));
            }
        }

        Map<String, Object> summary = Map.of(
                "totalEmployees", employees.size(),
                "activeMentorships", activeCount,
                "unmentoredCount", employees.size() - activeCount
        );

        return new ManagerReportDto(
                "MENTORSHIP_ENGAGEMENT",
                "Mentorship Engagement Report",
                Instant.now(),
                teamId,
                teamName,
                caller.getFullName(),
                rows.size(),
                summary,
                cols,
                rows
        );
    }

    private ManagerReportDto generateSkillInventoryCategoryReport(Long teamId, String teamName, User caller, List<User> employees, List<Long> employeeIds) {
        List<Skill> allSkills = skillRepository.findAll();
        List<EmployeeSkill> employeeSkills = employeeIds.isEmpty() ? List.of() : employeeSkillRepository.findByUserIds(employeeIds);
        List<String> cols = List.of("Skill Name", "Category", "Employees Assessed", "Average Level", "Proficiency Name", "Status");
        List<Map<String, Object>> rows = new ArrayList<>();

        for (Skill s : allSkills) {
            List<EmployeeSkill> matching = employeeSkills.stream().filter(es -> es.getSkill().getId().equals(s.getId())).toList();
            int count = matching.size();
            double avg = count > 0 ? matching.stream().mapToInt(es -> SkillService.PROFICIENCY_TO_NUMERIC.getOrDefault(es.getProficiencyLevel(), 1)).average().orElse(1.0) : 1.0;
            avg = Math.round(avg * 10.0) / 10.0;
            String status = avg >= 3.0 ? "MET" : "GAP";

            rows.add(Map.of(
                    "Skill Name", s.getName(),
                    "Category", s.getCategory() != null ? s.getCategory().getName() : "General",
                    "Employees Assessed", count,
                    "Average Level", avg,
                    "Proficiency Name", SkillService.NUMERIC_TO_PROFICIENCY.getOrDefault((int) Math.round(avg), "Unaware"),
                    "Status", status
            ));
        }

        Map<String, Object> summary = Map.of(
                "totalSkills", allSkills.size(),
                "totalEmployees", employees.size()
        );

        return new ManagerReportDto(
                "SKILL_INVENTORY_CATEGORY",
                "Skill Inventory by Category Report",
                Instant.now(),
                teamId,
                teamName,
                caller.getFullName(),
                rows.size(),
                summary,
                cols,
                rows
        );
    }

    private ManagerReportDto generateCriticalSkillGapsReport(Long teamId, String teamName, User caller, List<User> employees, List<Long> employeeIds) {
        List<SkillGap> gaps = employeeIds.isEmpty() ? List.of() : getSkillGapsForUsers(employeeIds);
        List<SkillGap> criticalGaps = gaps.stream().filter(g -> g.getGapSize() >= 2 || "HIGH".equalsIgnoreCase(g.getPriority())).toList();

        List<String> cols = List.of("Employee", "Employee ID", "Critical Skill", "Category", "Current Level", "Required Level", "Gap Deficit", "Priority");
        List<Map<String, Object>> rows = new ArrayList<>();

        for (SkillGap g : criticalGaps) {
            rows.add(Map.of(
                    "Employee", g.getUser().getFullName(),
                    "Employee ID", g.getUser().getEmployeeId() != null ? g.getUser().getEmployeeId() : "N/A",
                    "Critical Skill", g.getSkill().getName(),
                    "Category", g.getSkill().getCategory() != null ? g.getSkill().getCategory().getName() : "General",
                    "Current Level", SkillService.NUMERIC_TO_PROFICIENCY.getOrDefault(g.getCurrentLevel(), "Unaware"),
                    "Required Level", SkillService.NUMERIC_TO_PROFICIENCY.getOrDefault(g.getRequiredLevel(), "Beginner"),
                    "Gap Deficit", g.getGapSize(),
                    "Priority", g.getPriority()
            ));
        }

        Map<String, Object> summary = Map.of(
                "totalCriticalGaps", criticalGaps.size(),
                "affectedEmployees", criticalGaps.stream().map(g -> g.getUser().getId()).distinct().count()
        );

        return new ManagerReportDto(
                "CRITICAL_SKILL_GAPS",
                "At-Risk / Critical Skill Gaps Report",
                Instant.now(),
                teamId,
                teamName,
                caller.getFullName(),
                rows.size(),
                summary,
                cols,
                rows
        );
    }

    private ManagerReportDto generateTeamDevelopmentLogReport(Long teamId, String teamName, User caller, List<User> employees, List<Long> employeeIds) {
        List<AssessmentAttempt> attempts = employeeIds.isEmpty() ? List.of() : assessmentAttemptRepository.findByUserIdsOrderByStartedAtDesc(employeeIds);
        List<EmployeeLearning> learnings = employeeIds.isEmpty() ? List.of() : learningRepository.findByUserIds(employeeIds);
        List<ReassessmentRequest> reassessments = employeeIds.isEmpty() ? List.of() : reassessmentRequestRepository.findByEmployeeIdIn(employeeIds);

        List<String> cols = List.of("Activity Type", "Employee", "Action / Details", "Status", "Timestamp");
        List<Map<String, Object>> rows = new ArrayList<>();
        DateTimeFormatter fmt = DateTimeFormatter.ISO_INSTANT;

        for (AssessmentAttempt a : attempts.stream().limit(15).toList()) {
            rows.add(Map.of(
                    "Activity Type", "ASSESSMENT",
                    "Employee", a.getUser().getFullName(),
                    "Action / Details", "Assessment on " + a.getSkill().getName() + " (Target: " + a.getTargetLevel() + ")",
                    "Status", a.getStatus(),
                    "Timestamp", fmt.format(a.getStartedAt())
            ));
        }
        for (EmployeeLearning l : learnings.stream().limit(15).toList()) {
            rows.add(Map.of(
                    "Activity Type", "COURSE_ENROLLMENT",
                    "Employee", l.getUser().getFullName(),
                    "Action / Details", "Course: " + l.getCourse().getTitle() + " (" + l.getProgressPct() + "%)",
                    "Status", l.getStatus(),
                    "Timestamp", fmt.format(l.getEnrolledAt())
            ));
        }
        for (ReassessmentRequest r : reassessments.stream().limit(15).toList()) {
            rows.add(Map.of(
                    "Activity Type", "REASSESSMENT_REQUEST",
                    "Employee", r.getEmployee().getFullName(),
                    "Action / Details", "Reassessment for " + r.getSkill().getName() + ": " + r.getReason(),
                    "Status", r.getStatus(),
                    "Timestamp", fmt.format(r.getCreatedAt())
            ));
        }

        Map<String, Object> summary = Map.of(
                "totalActivities", rows.size()
        );

        return new ManagerReportDto(
                "TEAM_DEVELOPMENT_LOG",
                "Team Development Activity Log",
                Instant.now(),
                teamId,
                teamName,
                caller.getFullName(),
                rows.size(),
                summary,
                cols,
                rows
        );
    }

    @Transactional(readOnly = true)
    public String getReportCsv(String callerEmail, String reportType, Long teamIdFilter) {
        ManagerReportDto report = getReport(callerEmail, reportType, teamIdFilter);
        StringBuilder sb = new StringBuilder();

        // Header comments
        sb.append("# Report: ").append(report.title()).append("\n");
        sb.append("# Team: ").append(report.teamName()).append("\n");
        sb.append("# Generated By: ").append(report.managerName()).append("\n");
        sb.append("# Generated At: ").append(report.generatedAt()).append("\n");
        sb.append("# Total Records: ").append(report.totalRecords()).append("\n");
        sb.append("\n");

        // Column headers
        for (int i = 0; i < report.columns().size(); i++) {
            if (i > 0) sb.append(",");
            sb.append(escapeCsv(report.columns().get(i)));
        }
        sb.append("\n");

        // Data rows
        for (Map<String, Object> row : report.rows()) {
            for (int i = 0; i < report.columns().size(); i++) {
                if (i > 0) sb.append(",");
                String col = report.columns().get(i);
                Object val = row.get(col);
                sb.append(escapeCsv(val != null ? val.toString() : ""));
            }
            sb.append("\n");
        }

        return sb.toString();
    }

    private String escapeCsv(String s) {
        if (s == null) return "";
        if (s.contains(",") || s.contains("\"") || s.contains("\n") || s.contains("\r")) {
            return "\"" + s.replace("\"", "\"\"") + "\"";
        }
        return s;
    }

    // -------------------------------------------------------------
    // Mapping Helpers
    // -------------------------------------------------------------

    private EmployeeSkillDto toEmployeeSkillDto(EmployeeSkill es) {
        return new EmployeeSkillDto(
                es.getId(),
                es.getUser().getId(),
                es.getUser().getFullName(),
                es.getSkill().getId(),
                es.getSkill().getName(),
                es.getSkill().getCategory() != null ? es.getSkill().getCategory().getName() : "General",
                es.getProficiencyLevel(),
                SkillService.PROFICIENCY_TO_NUMERIC.getOrDefault(es.getProficiencyLevel(), 1),
                es.isVerified(),
                es.getVerifiedBy() != null ? es.getVerifiedBy().getFullName() : null,
                es.getUpdatedAt()
        );
    }

    private SkillGapDto toSkillGapDto(SkillGap g) {
        return new SkillGapDto(
                g.getSkill().getId(),
                g.getSkill().getName(),
                g.getSkill().getCategory() != null ? g.getSkill().getCategory().getName() : "General",
                g.getCurrentLevel(),
                SkillService.NUMERIC_TO_PROFICIENCY.getOrDefault(g.getCurrentLevel(), "Unaware"),
                g.getRequiredLevel(),
                SkillService.NUMERIC_TO_PROFICIENCY.getOrDefault(g.getRequiredLevel(), "Beginner"),
                g.getGapSize(),
                g.getPriority(),
                g.getCalculatedAt()
        );
    }

    private AssessmentAttemptDto toAssessmentAttemptDto(AssessmentAttempt a) {
        int timeLimit = a.getAssessment() != null ? a.getAssessment().getTimeLimitMinutes() : 30;
        int passScore = a.getAssessment() != null ? a.getAssessment().getPassScore() : 70;
        return new AssessmentAttemptDto(
                a.getId(),
                a.getSkill().getId(),
                a.getSkill().getName(),
                a.getSkill().getCategory() != null ? a.getSkill().getCategory().getName() : "General",
                a.getTargetLevel(),
                a.getCurrentLevel(),
                a.getStatus(),
                a.getGenerationSource(),
                a.getTotalQuestions(),
                timeLimit,
                passScore,
                a.getStartedAt(),
                Map.of(),
                List.of()
        );
    }

    private EmployeeLearningDto toEmployeeLearningDto(EmployeeLearning l) {
        return new EmployeeLearningDto(
                l.getId(),
                l.getCourse().getId(),
                l.getCourse().getTitle(),
                l.getCourse().getSkill() != null ? l.getCourse().getSkill().getName() : "General",
                l.getCourse().getLevel(),
                l.getCourse().getDurationHours(),
                l.getCourse().getProvider(),
                l.getCourse().getCourseUrl(),
                l.getStatus(),
                l.getProgressPct(),
                l.getEnrolledAt(),
                l.getCompletedAt(),
                l.getExpiresAt()
        );
    }

    private ReassessmentRequestDto toReassessmentRequestDto(ReassessmentRequest r) {
        return new ReassessmentRequestDto(
                r.getId(),
                r.getManager().getId(),
                r.getManager().getFullName(),
                r.getEmployee().getId(),
                r.getEmployee().getFullName(),
                r.getEmployee().getEmail(),
                r.getSkill().getId(),
                r.getSkill().getName(),
                r.getSkill().getCategory() != null ? r.getSkill().getCategory().getName() : "General",
                r.getReason(),
                r.getStatus(),
                r.getCreatedAt(),
                r.getUpdatedAt()
        );
    }

    private ManagerCourseRecommendationDto toCourseRecommendationDto(ManagerCourseRecommendation r) {
        return new ManagerCourseRecommendationDto(
                r.getId(),
                r.getManager().getId(),
                r.getManager().getFullName(),
                r.getEmployee().getId(),
                r.getEmployee().getFullName(),
                r.getCourse().getId(),
                r.getCourse().getTitle(),
                r.getCourse().getLevel(),
                r.getCourse().getProvider(),
                r.getCourse().getCourseUrl(),
                r.getSkill() != null ? r.getSkill().getId() : null,
                r.getSkill() != null ? r.getSkill().getName() : (r.getCourse().getSkill() != null ? r.getCourse().getSkill().getName() : "General"),
                r.getReason(),
                r.getStatus(),
                r.getCreatedAt(),
                r.getUpdatedAt()
        );
    }
}
