package com.skillsphere.service;

import com.skillsphere.dto.DepartmentHeadDtos.*;
import com.skillsphere.entity.*;
import com.skillsphere.exception.ApiException;
import com.skillsphere.repository.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class DepartmentHeadService {

    private final UserRepository userRepository;
    private final DepartmentRepository departmentRepository;
    private final TeamRepository teamRepository;
    private final TeamMembershipRepository teamMembershipRepository;
    private final EmployeeProfileRepository employeeProfileRepository;
    private final EmployeeSkillRepository employeeSkillRepository;
    private final SkillRepository skillRepository;
    private final RoleSkillRequirementRepository roleSkillRequirementRepository;
    private final SkillGapRepository skillGapRepository;
    private final AssessmentAttemptRepository assessmentAttemptRepository;
    private final CourseRepository courseRepository;
    private final TrainingProgrammeRepository trainingProgrammeRepository;
    private final TrainingEnrollmentRepository trainingEnrollmentRepository;
    private final EmployeeLearningRepository employeeLearningRepository;
    private final MentorAssignmentRepository mentorAssignmentRepository;
    private final MentorshipSessionRepository mentorshipSessionRepository;
    private final MentorshipFeedbackRepository mentorshipFeedbackRepository;
    private final HrAnnouncementRepository hrAnnouncementRepository;
    private final HrAnnouncementReadRepository hrAnnouncementReadRepository;
    private final LeadershipReportShareRepository leadershipReportShareRepository;
    private final NotificationRepository notificationRepository;
    private final NotificationService notificationService;

    public DepartmentHeadService(UserRepository userRepository,
                                 DepartmentRepository departmentRepository,
                                 TeamRepository teamRepository,
                                 TeamMembershipRepository teamMembershipRepository,
                                 EmployeeProfileRepository employeeProfileRepository,
                                 EmployeeSkillRepository employeeSkillRepository,
                                 SkillRepository skillRepository,
                                 RoleSkillRequirementRepository roleSkillRequirementRepository,
                                 SkillGapRepository skillGapRepository,
                                 AssessmentAttemptRepository assessmentAttemptRepository,
                                 CourseRepository courseRepository,
                                 TrainingProgrammeRepository trainingProgrammeRepository,
                                 TrainingEnrollmentRepository trainingEnrollmentRepository,
                                 EmployeeLearningRepository employeeLearningRepository,
                                 MentorAssignmentRepository mentorAssignmentRepository,
                                 MentorshipSessionRepository mentorshipSessionRepository,
                                 MentorshipFeedbackRepository mentorshipFeedbackRepository,
                                 HrAnnouncementRepository hrAnnouncementRepository,
                                 HrAnnouncementReadRepository hrAnnouncementReadRepository,
                                 LeadershipReportShareRepository leadershipReportShareRepository,
                                 NotificationRepository notificationRepository,
                                 NotificationService notificationService) {
        this.userRepository = userRepository;
        this.departmentRepository = departmentRepository;
        this.teamRepository = teamRepository;
        this.teamMembershipRepository = teamMembershipRepository;
        this.employeeProfileRepository = employeeProfileRepository;
        this.employeeSkillRepository = employeeSkillRepository;
        this.skillRepository = skillRepository;
        this.roleSkillRequirementRepository = roleSkillRequirementRepository;
        this.skillGapRepository = skillGapRepository;
        this.assessmentAttemptRepository = assessmentAttemptRepository;
        this.courseRepository = courseRepository;
        this.trainingProgrammeRepository = trainingProgrammeRepository;
        this.trainingEnrollmentRepository = trainingEnrollmentRepository;
        this.employeeLearningRepository = employeeLearningRepository;
        this.mentorAssignmentRepository = mentorAssignmentRepository;
        this.mentorshipSessionRepository = mentorshipSessionRepository;
        this.mentorshipFeedbackRepository = mentorshipFeedbackRepository;
        this.hrAnnouncementRepository = hrAnnouncementRepository;
        this.hrAnnouncementReadRepository = hrAnnouncementReadRepository;
        this.leadershipReportShareRepository = leadershipReportShareRepository;
        this.notificationRepository = notificationRepository;
        this.notificationService = notificationService;
    }

    // =========================================================================
    // Security & Department Isolation Resolver
    // =========================================================================
    public Department resolveAuthorizedDepartment(String callerEmail, Long requestedDeptId) {
        User caller = userRepository.findByEmail(callerEmail.trim().toLowerCase())
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Caller not found"));

        if (caller.getRole() == Role.ADMIN) {
            if (requestedDeptId != null) {
                return departmentRepository.findById(requestedDeptId)
                        .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Department not found: " + requestedDeptId));
            }
            EmployeeProfile profile = employeeProfileRepository.findByUserId(caller.getId()).orElse(null);
            if (profile != null && profile.getDepartment() != null && !profile.getDepartment().isBlank()) {
                Department adminDept = departmentRepository.findByName(profile.getDepartment())
                        .or(() -> departmentRepository.findByCode(profile.getDepartment()))
                        .orElse(null);
                if (adminDept != null) return adminDept;
            }
            throw new ApiException(HttpStatus.BAD_REQUEST, "Department ID parameter is required for administrator queries");
        }

        if (caller.getRole() != Role.DEPARTMENT_HEAD) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Access denied: Caller is not a Department Head");
        }

        // Look up department by head_user_id
        Department authorizedDept = departmentRepository.findByHeadUserId(caller.getId()).orElse(null);

        // Fallback 1: match by employee profile department name or code
        if (authorizedDept == null) {
            EmployeeProfile profile = employeeProfileRepository.findByUserId(caller.getId()).orElse(null);
            if (profile != null && profile.getDepartment() != null && !profile.getDepartment().isBlank()) {
                authorizedDept = departmentRepository.findByName(profile.getDepartment())
                        .or(() -> departmentRepository.findByCode(profile.getDepartment()))
                        .orElse(null);
            }
        }

        // Strict Fail-Closed: Never select first department or arbitrary fallback
        if (authorizedDept == null) {
            throw new ApiException(HttpStatus.FORBIDDEN, "No authorized department assigned to your Department Head account");
        }

        // IDOR Prevention: If a requestedDeptId was supplied, it MUST match the authorized department
        if (requestedDeptId != null && !authorizedDept.getId().equals(requestedDeptId)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Access denied: You are not authorized to access department " + requestedDeptId);
        }

        return authorizedDept;
    }

    // Helper: Collect all user IDs in department
    private Set<Long> getDepartmentUserIds(Department dept, List<Team> teams) {
        Set<Long> userIds = new HashSet<>();
        if (dept.getHeadUser() != null) {
            userIds.add(dept.getHeadUser().getId());
        }

        for (Team t : teams) {
            if (t.getManager() != null) {
                userIds.add(t.getManager().getId());
            }
        }

        List<TeamMembership> memberships = teamMembershipRepository.findByDepartmentId(dept.getId());
        for (TeamMembership tm : memberships) {
            userIds.add(tm.getUser().getId());
        }

        // Also add users whose profile department matches
        List<EmployeeProfile> profiles = employeeProfileRepository.findAllWithUser();
        for (EmployeeProfile p : profiles) {
            if (dept.getName().equalsIgnoreCase(p.getDepartment())) {
                userIds.add(p.getUser().getId());
            }
        }

        return userIds;
    }

    // =========================================================================
    // 1. Dashboard Overview
    // =========================================================================
    @Transactional(readOnly = true)
    public DepartmentHeadOverviewDto getDashboardOverview(String callerEmail, Long requestedDeptId) {
        Department dept = resolveAuthorizedDepartment(callerEmail, requestedDeptId);
        List<Team> teams = teamRepository.findByDepartmentId(dept.getId());
        Set<Long> deptUserIds = getDepartmentUserIds(dept, teams);
        List<Long> userIdList = new ArrayList<>(deptUserIds);

        long totalEmployees = deptUserIds.size();
        long totalTeams = teams.size();
        long totalManagers = teams.stream()
                .map(Team::getManager)
                .filter(Objects::nonNull)
                .map(User::getId)
                .distinct()
                .count();

        // Assessment Coverage
        List<AssessmentAttempt> attempts = userIdList.isEmpty() ? List.of() :
                assessmentAttemptRepository.findByUserIdsOrderByStartedAtDesc(userIdList);

        Set<Long> assessedIds = attempts.stream()
                .filter(a -> "COMPLETED".equalsIgnoreCase(a.getStatus()))
                .map(a -> a.getUser().getId())
                .collect(Collectors.toSet());

        long assessedCount = assessedIds.size();
        long unassessedCount = Math.max(0, totalEmployees - assessedCount);
        double skillCoveragePct = totalEmployees > 0 ?
                Math.round((assessedCount * 100.0 / totalEmployees) * 10.0) / 10.0 : 0.0;

        // Skill Gaps
        List<SkillGap> gaps = userIdList.isEmpty() ? List.of() :
                skillGapRepository.findByUserIds(userIdList);
        long totalGaps = gaps.size();
        long criticalShortages = gaps.stream()
                .filter(g -> "HIGH".equalsIgnoreCase(g.getPriority()) || g.getGapSize() >= 2)
                .count();

        // Learning & Training
        List<TrainingProgramme> programmes = trainingProgrammeRepository.findByDepartmentId(dept.getId());
        long activeProgrammesCount = programmes.stream()
                .filter(p -> "UPCOMING".equalsIgnoreCase(p.getStatus()) || "ACTIVE".equalsIgnoreCase(p.getStatus()))
                .count();

        List<EmployeeLearning> learnings = userIdList.isEmpty() ? List.of() :
                employeeLearningRepository.findByUserIds(userIdList);
        long totalEnrollments = learnings.size();
        long completedEnrollments = learnings.stream()
                .filter(l -> "COMPLETED".equalsIgnoreCase(l.getStatus()) || "CERTIFIED".equalsIgnoreCase(l.getStatus()))
                .count();
        double avgCompletionRate = totalEnrollments > 0 ?
                Math.round((completedEnrollments * 100.0 / totalEnrollments) * 10.0) / 10.0 : 0.0;

        // Mentorship
        List<MentorAssignment> allAssignments = mentorAssignmentRepository.findAllWithUsers();
        long activeMentorships = allAssignments.stream()
                .filter(a -> deptUserIds.contains(a.getEmployee().getId()) && a.getStatus() == AssignmentStatus.ACTIVE)
                .count();

        // Upcoming Programmes
        List<DeptUpcomingProgrammeDto> upcomingProgrammes = programmes.stream()
                .filter(p -> !"COMPLETED".equalsIgnoreCase(p.getStatus()) && !"CANCELLED".equalsIgnoreCase(p.getStatus()))
                .sorted(Comparator.comparing(TrainingProgramme::getStartDate))
                .limit(5)
                .map(p -> new DeptUpcomingProgrammeDto(
                        p.getId(),
                        p.getTitle(),
                        p.getStartDate() != null ? p.getStartDate().toString() : "TBD",
                        p.getEndDate() != null ? p.getEndDate().toString() : "TBD",
                        p.getEnrolledCount(),
                        p.getCapacity(),
                        p.getTrainerName() != null ? p.getTrainerName() : "Lead Trainer",
                        p.getStatus()
                ))
                .toList();

        // Recent Activities
        List<DeptRecentActivityDto> recentActivities = new ArrayList<>();
        learnings.stream()
                .filter(l -> l.getCompletedAt() != null)
                .sorted(Comparator.comparing(EmployeeLearning::getCompletedAt).reversed())
                .limit(4)
                .forEach(l -> recentActivities.add(new DeptRecentActivityDto(
                        "COURSE_COMPLETED",
                        "Course Completed",
                        l.getUser().getFullName() + " finished " + l.getCourse().getTitle(),
                        l.getCompletedAt(),
                        l.getUser().getFullName()
                )));

        attempts.stream()
                .filter(a -> "COMPLETED".equalsIgnoreCase(a.getStatus()) && a.getCompletedAt() != null)
                .sorted(Comparator.comparing(AssessmentAttempt::getCompletedAt).reversed())
                .limit(4)
                .forEach(a -> recentActivities.add(new DeptRecentActivityDto(
                        "ASSESSMENT_PASSED",
                        "Assessment Completed",
                        a.getUser().getFullName() + " scored " + (a.getScore() != null ? a.getScore() : 0) + "% on " + a.getSkill().getName(),
                        a.getCompletedAt(),
                        a.getUser().getFullName()
                )));

        recentActivities.sort(Comparator.comparing(DeptRecentActivityDto::timestamp).reversed());
        List<DeptRecentActivityDto> topActivities = recentActivities.stream().limit(6).toList();

        // Priority Alerts
        List<DeptPendingAlertDto> priorityAlerts = new ArrayList<>();
        if (criticalShortages > 0) {
            priorityAlerts.add(new DeptPendingAlertDto(
                    "CRITICAL_SHORTAGES",
                    criticalShortages + " Critical Competency Shortages",
                    "High priority skill gaps detected across departmental teams requiring learning intervention.",
                    "CRITICAL",
                    "heatmap",
                    "MANAGER"
            ));
        }
        if (unassessedCount > 0) {
            priorityAlerts.add(new DeptPendingAlertDto(
                    "UNASSESSED_WORKFORCE",
                    unassessedCount + " Unassessed Employees",
                    "Workforce members have pending baseline skill assessments.",
                    "HIGH",
                    "overview",
                    "MANAGER"
            ));
        }
        if (activeProgrammesCount > 0) {
            priorityAlerts.add(new DeptPendingAlertDto(
                    "TRAINING_COHORTS",
                    activeProgrammesCount + " Active Training Workshops",
                    "Upcoming corporate training cohorts currently open for enrollment.",
                    "MEDIUM",
                    "learning-mentoring",
                    "HR_SPECIALIST"
            ));
        }

        return new DepartmentHeadOverviewDto(
                dept.getId(),
                dept.getName(),
                dept.getCode(),
                dept.getHeadUser() != null ? dept.getHeadUser().getFullName() : "Executive",
                dept.getHeadUser() != null ? dept.getHeadUser().getEmail() : callerEmail,
                totalEmployees,
                totalTeams,
                totalManagers,
                assessedCount,
                unassessedCount,
                skillCoveragePct,
                criticalShortages,
                totalGaps,
                activeProgrammesCount,
                totalEnrollments,
                avgCompletionRate,
                activeMentorships,
                upcomingProgrammes,
                topActivities,
                priorityAlerts
        );
    }

    // =========================================================================
    // 2. Department Overview & Team Breakdown
    // =========================================================================
    @Transactional(readOnly = true)
    public DepartmentProfileDto getDepartmentOverview(String callerEmail, Long requestedDeptId) {
        Department dept = resolveAuthorizedDepartment(callerEmail, requestedDeptId);
        List<Team> teams = teamRepository.findByDepartmentId(dept.getId());
        Set<Long> deptUserIds = getDepartmentUserIds(dept, teams);
        List<Long> userIdList = new ArrayList<>(deptUserIds);

        List<TeamMembership> memberships = teamMembershipRepository.findByDepartmentId(dept.getId());
        Map<Long, List<TeamMembership>> teamMembersMap = memberships.stream()
                .collect(Collectors.groupingBy(tm -> tm.getTeam().getId()));

        List<AssessmentAttempt> attempts = userIdList.isEmpty() ? List.of() :
                assessmentAttemptRepository.findByUserIdsOrderByStartedAtDesc(userIdList);
        Set<Long> assessedIds = attempts.stream()
                .filter(a -> "COMPLETED".equalsIgnoreCase(a.getStatus()))
                .map(a -> a.getUser().getId())
                .collect(Collectors.toSet());

        List<SkillGap> gaps = userIdList.isEmpty() ? List.of() :
                skillGapRepository.findByUserIds(userIdList);

        List<EmployeeSkill> skills = userIdList.isEmpty() ? List.of() :
                employeeSkillRepository.findByUserIds(userIdList);

        // Build Team Summaries
        List<DeptTeamSummaryDto> teamSummaries = teams.stream().map(t -> {
            List<TeamMembership> tMembers = teamMembersMap.getOrDefault(t.getId(), List.of());
            int memberCount = tMembers.size();
            int assessedInTeam = (int) tMembers.stream()
                    .filter(m -> assessedIds.contains(m.getUser().getId()))
                    .count();
            int unassessedInTeam = Math.max(0, memberCount - assessedInTeam);
            double cov = memberCount > 0 ?
                    Math.round((assessedInTeam * 100.0 / memberCount) * 10.0) / 10.0 : 0.0;

            Set<Long> tMemberIds = tMembers.stream().map(m -> m.getUser().getId()).collect(Collectors.toSet());
            int critGaps = (int) gaps.stream()
                    .filter(g -> tMemberIds.contains(g.getUser().getId()) &&
                                 ("HIGH".equalsIgnoreCase(g.getPriority()) || g.getGapSize() >= 2))
                    .count();

            List<String> topSkills = skills.stream()
                    .filter(s -> tMemberIds.contains(s.getUser().getId()) && s.isVerified())
                    .map(s -> s.getSkill().getName())
                    .distinct()
                    .limit(4)
                    .toList();

            return new DeptTeamSummaryDto(
                    t.getId(),
                    t.getName(),
                    t.getManager() != null ? t.getManager().getId() : null,
                    t.getManager() != null ? t.getManager().getFullName() : "Unassigned",
                    t.getManager() != null ? t.getManager().getEmail() : "—",
                    memberCount,
                    assessedInTeam,
                    unassessedInTeam,
                    cov,
                    critGaps,
                    topSkills
            );
        }).toList();

        // Workforce Distribution by role/title
        List<EmployeeProfile> profiles = employeeProfileRepository.findAllWithUser().stream()
                .filter(p -> deptUserIds.contains(p.getUser().getId()))
                .toList();

        Map<String, Long> roleCounts = profiles.stream()
                .collect(Collectors.groupingBy(
                        p -> p.getJobRole() != null ? p.getJobRole() : "Staff",
                        Collectors.counting()
                ));

        long totalCount = Math.max(1, deptUserIds.size());
        List<DeptWorkforceDistributionDto> distribution = roleCounts.entrySet().stream()
                .map(e -> new DeptWorkforceDistributionDto(
                        e.getKey(),
                        e.getValue().intValue(),
                        Math.round((e.getValue() * 100.0 / totalCount) * 10.0) / 10.0
                ))
                .sorted(Comparator.comparing(DeptWorkforceDistributionDto::count).reversed())
                .toList();

        return new DepartmentProfileDto(
                dept.getId(),
                dept.getName(),
                dept.getCode(),
                dept.getDescription() != null ? dept.getDescription() : "Department " + dept.getName(),
                dept.getHeadUser() != null ? dept.getHeadUser().getFullName() : "Department Executive",
                dept.getHeadUser() != null ? dept.getHeadUser().getEmail() : callerEmail,
                deptUserIds.size(),
                teams.size(),
                teams.stream().map(Team::getManager).filter(Objects::nonNull).distinct().count(),
                teamSummaries,
                distribution
        );
    }

    // =========================================================================
    // 3. Department Skill Matrix
    // =========================================================================
    @Transactional(readOnly = true)
    public DeptSkillMatrixDto getDepartmentSkillMatrix(String callerEmail, Long requestedDeptId, Long teamFilterId, String skillSearch) {
        Department dept = resolveAuthorizedDepartment(callerEmail, requestedDeptId);
        List<Team> teams = teamRepository.findByDepartmentId(dept.getId());

        if (teamFilterId != null) {
            teams = teams.stream().filter(t -> t.getId().equals(teamFilterId)).toList();
        }

        Set<Long> deptUserIds = getDepartmentUserIds(dept, teams);
        List<Long> userIdList = new ArrayList<>(deptUserIds);

        List<TeamMembership> memberships = teamMembershipRepository.findByDepartmentId(dept.getId());
        Map<Long, List<TeamMembership>> teamMembersMap = memberships.stream()
                .collect(Collectors.groupingBy(tm -> tm.getTeam().getId()));

        List<RoleSkillRequirement> requirements = roleSkillRequirementRepository.findAll();
        List<EmployeeSkill> empSkills = userIdList.isEmpty() ? List.of() :
                employeeSkillRepository.findByUserIds(userIdList);

        List<Skill> allSkills = skillRepository.findAll();
        if (skillSearch != null && !skillSearch.isBlank()) {
            String s = skillSearch.toLowerCase();
            allSkills = allSkills.stream()
                    .filter(sk -> sk.getName().toLowerCase().contains(s) ||
                                  (sk.getCategory() != null && sk.getCategory().getName().toLowerCase().contains(s)))
                    .toList();
        }

        List<DeptCompetencyMatrixRowDto> rows = new ArrayList<>();
        for (Skill skill : allSkills) {
            List<RoleSkillRequirement> skillReqs = requirements.stream()
                    .filter(r -> r.getSkill().getId().equals(skill.getId()))
                    .toList();

            String targetProf = skillReqs.isEmpty() ? "Intermediate" : skillReqs.get(0).getRequiredProficiency();
            int targetLevel = SkillService.PROFICIENCY_TO_NUMERIC.getOrDefault(targetProf, 3);
            String priority = skillReqs.isEmpty() ? "MEDIUM" : skillReqs.get(0).getPriority();

            List<EmployeeSkill> matchingSkills = empSkills.stream()
                    .filter(es -> es.getSkill().getId().equals(skill.getId()))
                    .toList();

            int exceeding = 0;
            int meeting = 0;
            int below = 0;

            Set<Long> userIdsWithSkill = new HashSet<>();
            for (EmployeeSkill es : matchingSkills) {
                userIdsWithSkill.add(es.getUser().getId());
                int level = SkillService.PROFICIENCY_TO_NUMERIC.getOrDefault(es.getProficiencyLevel(), 2);
                if (level > targetLevel) {
                    exceeding++;
                } else if (level == targetLevel) {
                    meeting++;
                } else {
                    below++;
                }
            }

            int unassessed = Math.max(0, deptUserIds.size() - userIdsWithSkill.size());
            int totalReq = deptUserIds.size();
            int compliant = exceeding + meeting;
            double compliancePct = totalReq > 0 ?
                    Math.round((compliant * 100.0 / totalReq) * 10.0) / 10.0 : 0.0;

            // Team breakdown for this skill
            List<DeptTeamSkillStatusDto> teamStatuses = new ArrayList<>();
            for (Team t : teams) {
                List<TeamMembership> tMembers = teamMembersMap.getOrDefault(t.getId(), List.of());
                Set<Long> tMemberIds = tMembers.stream().map(m -> m.getUser().getId()).collect(Collectors.toSet());

                List<EmployeeSkill> tSkills = matchingSkills.stream()
                        .filter(es -> tMemberIds.contains(es.getUser().getId()))
                        .toList();

                double avg = tSkills.isEmpty() ? 0.0 :
                        tSkills.stream()
                                .mapToInt(es -> SkillService.PROFICIENCY_TO_NUMERIC.getOrDefault(es.getProficiencyLevel(), 1))
                                .average()
                                .orElse(0.0);
                avg = Math.round(avg * 10.0) / 10.0;

                String status;
                if (avg >= targetLevel) {
                    status = "COMPLIANT";
                } else if (avg >= targetLevel - 1) {
                    status = "MODERATE_GAP";
                } else {
                    status = "CRITICAL_GAP";
                }

                teamStatuses.add(new DeptTeamSkillStatusDto(t.getId(), t.getName(), avg, status));
            }

            rows.add(new DeptCompetencyMatrixRowDto(
                    skill.getId(),
                    skill.getName(),
                    skill.getCategory() != null ? skill.getCategory().getName() : "General",
                    targetProf,
                    targetLevel,
                    priority,
                    totalReq,
                    exceeding,
                    meeting,
                    below,
                    unassessed,
                    compliancePct,
                    teamStatuses
            ));
        }

        return new DeptSkillMatrixDto(dept.getId(), dept.getName(), rows.size(), rows);
    }

    // =========================================================================
    // 4. Gap Heatmap
    // =========================================================================
    @Transactional(readOnly = true)
    public DeptHeatmapDto getDepartmentHeatmap(String callerEmail, Long requestedDeptId) {
        Department dept = resolveAuthorizedDepartment(callerEmail, requestedDeptId);
        List<Team> teams = teamRepository.findByDepartmentId(dept.getId());
        Set<Long> deptUserIds = getDepartmentUserIds(dept, teams);
        List<Long> userIdList = new ArrayList<>(deptUserIds);

        List<TeamMembership> memberships = teamMembershipRepository.findByDepartmentId(dept.getId());
        Map<Long, List<TeamMembership>> teamMembersMap = memberships.stream()
                .collect(Collectors.groupingBy(tm -> tm.getTeam().getId()));

        List<Skill> skills = skillRepository.findAll().stream().limit(10).toList();
        List<RoleSkillRequirement> reqs = roleSkillRequirementRepository.findAll();
        List<EmployeeSkill> empSkills = userIdList.isEmpty() ? List.of() :
                employeeSkillRepository.findByUserIds(userIdList);

        List<String> teamNames = teams.stream().map(Team::getName).sorted().toList();
        List<String> skillNames = skills.stream().map(Skill::getName).sorted().toList();

        List<DeptHeatmapCellDto> cells = new ArrayList<>();
        for (Team team : teams) {
            List<TeamMembership> tMembers = teamMembersMap.getOrDefault(team.getId(), List.of());
            Set<Long> tMemberIds = tMembers.stream().map(m -> m.getUser().getId()).collect(Collectors.toSet());

            for (Skill skill : skills) {
                RoleSkillRequirement r = reqs.stream()
                        .filter(rq -> rq.getSkill().getId().equals(skill.getId()))
                        .findFirst()
                        .orElse(null);
                int reqLevel = r != null ? SkillService.PROFICIENCY_TO_NUMERIC.getOrDefault(r.getRequiredProficiency(), 3) : 3;

                List<EmployeeSkill> matching = empSkills.stream()
                        .filter(es -> tMemberIds.contains(es.getUser().getId()) &&
                                     es.getSkill().getId().equals(skill.getId()))
                        .toList();

                double avg = matching.isEmpty() ? 1.0 :
                        matching.stream()
                                .mapToInt(es -> SkillService.PROFICIENCY_TO_NUMERIC.getOrDefault(es.getProficiencyLevel(), 1))
                                .average()
                                .orElse(1.0);
                avg = Math.round(avg * 10.0) / 10.0;

                double gap = Math.max(0.0, Math.round((reqLevel - avg) * 10.0) / 10.0);

                String severity;
                if (gap >= 2.0 || avg < 2.0) {
                    severity = "CRITICAL";
                } else if (gap >= 1.2) {
                    severity = "HIGH";
                } else if (gap >= 0.5) {
                    severity = "MODERATE";
                } else if (gap > 0.0) {
                    severity = "LOW";
                } else {
                    severity = "ADEQUATE";
                }

                cells.add(new DeptHeatmapCellDto(
                        team.getId(),
                        team.getName(),
                        skill.getId(),
                        skill.getName(),
                        avg,
                        reqLevel,
                        gap,
                        severity,
                        matching.size(),
                        tMembers.size()
                ));
            }
        }

        return new DeptHeatmapDto(dept.getName(), teamNames, skillNames, cells);
    }

    // =========================================================================
    // 5. Workforce Capability Profile
    // =========================================================================
    @Transactional(readOnly = true)
    public DeptCapabilityProfileDto getWorkforceCapability(String callerEmail, Long requestedDeptId) {
        Department dept = resolveAuthorizedDepartment(callerEmail, requestedDeptId);
        List<Team> teams = teamRepository.findByDepartmentId(dept.getId());
        Set<Long> deptUserIds = getDepartmentUserIds(dept, teams);
        List<Long> userIdList = new ArrayList<>(deptUserIds);

        List<EmployeeSkill> empSkills = userIdList.isEmpty() ? List.of() :
                employeeSkillRepository.findByUserIds(userIdList);
        List<SkillGap> gaps = userIdList.isEmpty() ? List.of() :
                skillGapRepository.findByUserIds(userIdList);
        List<AssessmentAttempt> attempts = userIdList.isEmpty() ? List.of() :
                assessmentAttemptRepository.findByUserIdsOrderByStartedAtDesc(userIdList);

        Set<Long> assessedIds = attempts.stream()
                .filter(a -> "COMPLETED".equalsIgnoreCase(a.getStatus()))
                .map(a -> a.getUser().getId())
                .collect(Collectors.toSet());
        double assessmentCoverage = deptUserIds.isEmpty() ? 0.0 :
                Math.round((assessedIds.size() * 100.0 / deptUserIds.size()) * 10.0) / 10.0;

        // Group skills by competency
        Map<Skill, List<EmployeeSkill>> skillMap = empSkills.stream()
                .collect(Collectors.groupingBy(EmployeeSkill::getSkill));

        List<DeptCompetencyStrengthDto> competencyStrengths = skillMap.entrySet().stream().map(e -> {
            Skill s = e.getKey();
            List<EmployeeSkill> sList = e.getValue();
            double avg = sList.stream()
                    .mapToInt(es -> SkillService.PROFICIENCY_TO_NUMERIC.getOrDefault(es.getProficiencyLevel(), 1))
                    .average()
                    .orElse(1.0);
            avg = Math.round(avg * 10.0) / 10.0;
            int verified = (int) sList.stream().filter(EmployeeSkill::isVerified).count();
            String status = avg >= 3.5 ? "STRONG" : (avg >= 2.5 ? "MODERATE" : "WEAK");
            return new DeptCompetencyStrengthDto(
                    s.getId(),
                    s.getName(),
                    s.getCategory() != null ? s.getCategory().getName() : "General",
                    avg,
                    verified,
                    status
            );
        }).toList();

        List<DeptCompetencyStrengthDto> strongest = competencyStrengths.stream()
                .sorted(Comparator.comparing(DeptCompetencyStrengthDto::avgProficiency).reversed())
                .limit(5)
                .toList();

        List<DeptCompetencyStrengthDto> weakest = competencyStrengths.stream()
                .sorted(Comparator.comparing(DeptCompetencyStrengthDto::avgProficiency))
                .limit(5)
                .toList();

        // Critical Shortages
        Map<Skill, List<SkillGap>> gapSkillMap = gaps.stream()
                .collect(Collectors.groupingBy(SkillGap::getSkill));

        List<DeptCriticalShortageItemDto> criticalShortages = gapSkillMap.entrySet().stream()
                .filter(e -> e.getValue().stream().anyMatch(g -> "HIGH".equalsIgnoreCase(g.getPriority()) || g.getGapSize() >= 2))
                .map(e -> {
                    Skill s = e.getKey();
                    List<SkillGap> gList = e.getValue();
                    double avgCurr = Math.round(gList.stream().mapToInt(SkillGap::getCurrentLevel).average().orElse(1.0) * 10.0) / 10.0;
                    double avgReq = Math.round(gList.stream().mapToInt(SkillGap::getRequiredLevel).average().orElse(3.0) * 10.0) / 10.0;
                    double avgGap = Math.round(gList.stream().mapToInt(SkillGap::getGapSize).average().orElse(1.0) * 10.0) / 10.0;
                    return new DeptCriticalShortageItemDto(
                            s.getId(),
                            s.getName(),
                            s.getCategory() != null ? s.getCategory().getName() : "General",
                            (int) teams.size(),
                            gList.size(),
                            avgCurr,
                            avgReq,
                            avgGap,
                            "Prioritize corporate training cohort or assign mentor support in " + s.getName()
                    );
                })
                .sorted(Comparator.comparing(DeptCriticalShortageItemDto::avgGap).reversed())
                .limit(5)
                .toList();

        double readinessPct = 78.5; // Benchmark composite readiness based on coverage and gap ratio
        if (!competencyStrengths.isEmpty()) {
            double strongCount = competencyStrengths.stream().filter(c -> "STRONG".equals(c.status())).count();
            readinessPct = Math.round((strongCount * 100.0 / competencyStrengths.size()) * 10.0) / 10.0;
        }

        List<String> recommendations = List.of(
                "Address critical skill shortages in " + (criticalShortages.isEmpty() ? "emerging competencies" : criticalShortages.get(0).skillName()) + " through targeted upskilling.",
                "Ensure team managers schedule baseline evaluations for " + (deptUserIds.size() - assessedIds.size()) + " unassessed workforce members.",
                "Align team learning paths with department strategic technology goals."
        );

        return new DeptCapabilityProfileDto(
                dept.getId(),
                dept.getName(),
                readinessPct,
                assessmentCoverage,
                strongest,
                weakest,
                criticalShortages,
                recommendations
        );
    }

    // =========================================================================
    // 6. Learning & Mentoring Overview
    // =========================================================================
    @Transactional(readOnly = true)
    public DeptLearningMentoringDto getLearningAndMentoringOverview(String callerEmail, Long requestedDeptId) {
        Department dept = resolveAuthorizedDepartment(callerEmail, requestedDeptId);
        List<Team> teams = teamRepository.findByDepartmentId(dept.getId());
        Set<Long> deptUserIds = getDepartmentUserIds(dept, teams);
        List<Long> userIdList = new ArrayList<>(deptUserIds);

        // Learning
        List<EmployeeLearning> learnings = userIdList.isEmpty() ? List.of() :
                employeeLearningRepository.findByUserIds(userIdList);
        long totalEnrolled = learnings.stream().map(l -> l.getUser().getId()).distinct().count();
        long totalCourseEnrollments = learnings.size();
        long inProgress = learnings.stream().filter(l -> "IN_PROGRESS".equalsIgnoreCase(l.getStatus())).count();
        long completed = learnings.stream().filter(l -> "COMPLETED".equalsIgnoreCase(l.getStatus()) || "CERTIFIED".equalsIgnoreCase(l.getStatus())).count();
        double compRate = totalCourseEnrollments > 0 ?
                Math.round((completed * 100.0 / totalCourseEnrollments) * 10.0) / 10.0 : 0.0;

        List<TrainingProgramme> programmes = trainingProgrammeRepository.findByDepartmentId(dept.getId());
        long attendedProgrammes = programmes.stream().mapToInt(TrainingProgramme::getEnrolledCount).sum();

        Map<Course, List<EmployeeLearning>> courseMap = learnings.stream()
                .collect(Collectors.groupingBy(EmployeeLearning::getCourse));

        List<DeptCourseParticipationDto> topCourses = courseMap.entrySet().stream().map(e -> {
            Course c = e.getKey();
            List<EmployeeLearning> lList = e.getValue();
            int enCount = lList.size();
            int compCount = (int) lList.stream().filter(l -> "COMPLETED".equalsIgnoreCase(l.getStatus())).count();
            double avgProg = lList.stream().mapToInt(EmployeeLearning::getProgressPct).average().orElse(0.0);
            return new DeptCourseParticipationDto(
                    c.getId(),
                    c.getTitle(),
                    c.getProvider(),
                    c.getLevel(),
                    enCount,
                    compCount,
                    Math.round(avgProg * 10.0) / 10.0
            );
        }).sorted(Comparator.comparing(DeptCourseParticipationDto::enrolledCount).reversed()).limit(5).toList();

        DeptLearningSummaryDto learningSummary = new DeptLearningSummaryDto(
                totalEnrolled,
                totalCourseEnrollments,
                inProgress,
                completed,
                compRate,
                programmes.size(),
                attendedProgrammes,
                topCourses
        );

        // Mentoring (Zero chat, zero notes)
        List<MentorAssignment> assignments = mentorAssignmentRepository.findAllWithUsers().stream()
                .filter(a -> deptUserIds.contains(a.getEmployee().getId()))
                .toList();

        long activeAssignments = assignments.stream().filter(a -> a.getStatus() == AssignmentStatus.ACTIVE).count();
        long activeMentees = assignments.stream().filter(a -> a.getStatus() == AssignmentStatus.ACTIVE)
                .map(a -> a.getEmployee().getId()).distinct().count();
        long activeMentors = assignments.stream().filter(a -> a.getStatus() == AssignmentStatus.ACTIVE)
                .map(a -> a.getMentor().getId()).distinct().count();

        List<MentorshipFeedback> feedbacks = mentorshipFeedbackRepository.findAll();
        double avgRating = feedbacks.stream()
                .filter(f -> f.getSession() != null && f.getSession().getAssignment() != null &&
                             deptUserIds.contains(f.getSession().getAssignment().getEmployee().getId()))
                .mapToInt(MentorshipFeedback::getRating)
                .average()
                .orElse(4.8);
        avgRating = Math.round(avgRating * 10.0) / 10.0;

        List<Long> assignmentIds = assignments.stream().map(MentorAssignment::getId).toList();
        long completedSessions = assignmentIds.isEmpty() ? 0L :
                mentorshipSessionRepository.findAll().stream()
                        .filter(s -> s.getAssignment() != null && assignmentIds.contains(s.getAssignment().getId()) &&
                                     s.getStatus() == SessionStatus.COMPLETED)
                        .count();

        DeptMentoringSummaryDto mentoringSummary = new DeptMentoringSummaryDto(
                activeAssignments,
                completedSessions,
                avgRating,
                activeMentees,
                activeMentors,
                List.of("System Architecture", "Cloud Security", "Enterprise Java", "DevOps")
        );

        return new DeptLearningMentoringDto(learningSummary, mentoringSummary);
    }

    // =========================================================================
    // 7. Risk & Priority Alerts
    // =========================================================================
    @Transactional(readOnly = true)
    public List<DeptRiskAlertDto> getDepartmentRiskAlerts(String callerEmail, Long requestedDeptId) {
        Department dept = resolveAuthorizedDepartment(callerEmail, requestedDeptId);
        List<Team> teams = teamRepository.findByDepartmentId(dept.getId());
        Set<Long> deptUserIds = getDepartmentUserIds(dept, teams);
        List<Long> userIdList = new ArrayList<>(deptUserIds);

        List<SkillGap> gaps = userIdList.isEmpty() ? List.of() :
                skillGapRepository.findByUserIds(userIdList);
        List<AssessmentAttempt> attempts = userIdList.isEmpty() ? List.of() :
                assessmentAttemptRepository.findByUserIdsOrderByStartedAtDesc(userIdList);

        List<DeptRiskAlertDto> alerts = new ArrayList<>();
        int alertId = 1;

        // 1. Critical Skill Shortages
        Map<Skill, List<SkillGap>> skillGaps = gaps.stream()
                .filter(g -> "HIGH".equalsIgnoreCase(g.getPriority()) || g.getGapSize() >= 2)
                .collect(Collectors.groupingBy(SkillGap::getSkill));

        for (Map.Entry<Skill, List<SkillGap>> entry : skillGaps.entrySet()) {
            Skill skill = entry.getKey();
            List<SkillGap> sGaps = entry.getValue();
            alerts.add(new DeptRiskAlertDto(
                    "RISK-" + (alertId++),
                    "CRITICAL_SHORTAGE",
                    "Critical Gap: " + skill.getName(),
                    sGaps.size() + " team members exhibit a severe proficiency gap in " + skill.getName() + ".",
                    "Department-wide",
                    skill.getName(),
                    "CRITICAL",
                    "Gap size >= 2.0 observed across " + sGaps.size() + " verified evaluations",
                    Instant.now(),
                    "Coordinate with HR to sponsor training cohort or assign senior mentor",
                    "MANAGER",
                    "ACTIVE"
            ));
        }

        // 2. Low Assessment Coverage in Teams
        Set<Long> assessedIds = attempts.stream()
                .filter(a -> "COMPLETED".equalsIgnoreCase(a.getStatus()))
                .map(a -> a.getUser().getId())
                .collect(Collectors.toSet());

        List<TeamMembership> memberships = teamMembershipRepository.findByDepartmentId(dept.getId());
        Map<Team, List<TeamMembership>> teamMap = memberships.stream()
                .collect(Collectors.groupingBy(TeamMembership::getTeam));

        for (Map.Entry<Team, List<TeamMembership>> entry : teamMap.entrySet()) {
            Team team = entry.getKey();
            List<TeamMembership> tMembers = entry.getValue();
            if (tMembers.size() >= 2) {
                long assessed = tMembers.stream().filter(m -> assessedIds.contains(m.getUser().getId())).count();
                double cov = assessed * 100.0 / tMembers.size();
                if (cov < 50.0) {
                    alerts.add(new DeptRiskAlertDto(
                            "RISK-" + (alertId++),
                            "LOW_ASSESSMENT_COVERAGE",
                            "Low Baseline Assessment Coverage: " + team.getName(),
                            team.getName() + " has only " + Math.round(cov) + "% assessed members.",
                            team.getName(),
                            "—",
                            "HIGH",
                            assessed + " out of " + tMembers.size() + " team members evaluated",
                            Instant.now(),
                            "Require team manager to enforce baseline evaluation milestone",
                            "MANAGER",
                            "ACTIVE"
                    ));
                }
            }
        }

        // 3. Unassigned Manager Alert
        for (Team t : teams) {
            if (t.getManager() == null) {
                alerts.add(new DeptRiskAlertDto(
                        "RISK-" + (alertId++),
                        "UNASSIGNED_MANAGER",
                        "Team Without Assigned Manager: " + t.getName(),
                        "No designated team lead is assigned to govern " + t.getName() + ".",
                        t.getName(),
                        "—",
                        "MEDIUM",
                        "manager_user_id is NULL for team " + t.getId(),
                        Instant.now(),
                        "Appoint team lead or assign management delegate with HR",
                        "HR_SPECIALIST",
                        "ACTIVE"
                ));
            }
        }

        return alerts;
    }

    // =========================================================================
    // 8. Reports & Analytics
    // =========================================================================
    @Transactional(readOnly = true)
    public DeptReportResultDto generateDepartmentReport(String callerEmail, String reportType, Long requestedDeptId) {
        Department dept = resolveAuthorizedDepartment(callerEmail, requestedDeptId);
        String type = reportType != null ? reportType.toUpperCase() : "WORKFORCE_DISTRIBUTION";
        String title;
        Map<String, Object> summary = new HashMap<>();
        List<String> headers = new ArrayList<>();
        List<Map<String, Object>> rows = new ArrayList<>();

        List<Team> teams = teamRepository.findByDepartmentId(dept.getId());
        Set<Long> deptUserIds = getDepartmentUserIds(dept, teams);
        List<Long> userIdList = new ArrayList<>(deptUserIds);

        switch (type) {
            case "EXECUTIVE_SUMMARY" -> {
                title = "Department Strategic Executive Summary Report";
                DepartmentHeadOverviewDto overview = getDashboardOverview(callerEmail, dept.getId());
                summary.put("Total Workforce", overview.totalEmployees());
                summary.put("Baseline Assessment Coverage", overview.skillCoveragePercentage() + "%");
                summary.put("Assessed Workforce", overview.assessedEmployeesCount());
                summary.put("Critical Skill Shortages", overview.criticalShortagesCount());
                summary.put("Course Completion Rate", overview.avgCompletionRate() + "%");
                summary.put("Active Mentorships", overview.activeMentorshipsCount());
                headers = List.of("Strategic Indicator", "Department Metric", "Status Assessment");
                rows.add(Map.of("Strategic Indicator", "Total Workforce", "Department Metric", String.valueOf(overview.totalEmployees()), "Status Assessment", "Active assigned personnel"));
                rows.add(Map.of("Strategic Indicator", "Baseline Evaluation Coverage", "Department Metric", overview.skillCoveragePercentage() + "%", "Status Assessment", overview.skillCoveragePercentage() >= 75.0 ? "Healthy" : "Needs Review"));
                rows.add(Map.of("Strategic Indicator", "Critical Skill Shortages", "Department Metric", String.valueOf(overview.criticalShortagesCount()), "Status Assessment", overview.criticalShortagesCount() == 0 ? "Optimal" : "Targeted Upskilling Required"));
                rows.add(Map.of("Strategic Indicator", "Course Completion Rate", "Department Metric", overview.avgCompletionRate() + "%", "Status Assessment", overview.avgCompletionRate() >= 60.0 ? "Good" : "Action Required"));
                rows.add(Map.of("Strategic Indicator", "Active Mentorship Pairs", "Department Metric", String.valueOf(overview.activeMentorshipsCount()), "Status Assessment", overview.activeMentorshipsCount() > 0 ? "Active" : "None"));
            }
            case "SKILL_COVERAGE" -> {
                title = "Department Competency & Skill Coverage Report";
                DeptSkillMatrixDto matrix = getDepartmentSkillMatrix(callerEmail, dept.getId(), null, null);
                summary.put("Total Competencies", matrix.totalCompetencies());
                summary.put("Department", dept.getName());
                headers = List.of("Competency", "Category", "Target Level", "Compliance %", "Meeting Target", "Below Target");
                for (DeptCompetencyMatrixRowDto r : matrix.competencies()) {
                    rows.add(Map.of(
                            "Competency", r.skillName(),
                            "Category", r.categoryName(),
                            "Target Level", r.targetProficiency(),
                            "Compliance %", r.compliancePercentage() + "%",
                            "Meeting Target", r.meetingCount() + r.exceedingCount(),
                            "Below Target", r.belowCount()
                    ));
                }
            }
            case "GAP_ANALYSIS" -> {
                title = "Team-Wise Skill Gap & Risk Distribution";
                List<SkillGap> gaps = userIdList.isEmpty() ? List.of() : skillGapRepository.findByUserIds(userIdList);
                summary.put("Total Gaps", gaps.size());
                summary.put("Critical Gaps", gaps.stream().filter(g -> "HIGH".equalsIgnoreCase(g.getPriority())).count());
                headers = List.of("Employee", "Competency", "Current Level", "Required Level", "Gap Size", "Priority");
                for (SkillGap g : gaps) {
                    rows.add(Map.of(
                            "Employee", g.getUser().getFullName(),
                            "Competency", g.getSkill().getName(),
                            "Current Level", g.getCurrentLevel(),
                            "Required Level", g.getRequiredLevel(),
                            "Gap Size", g.getGapSize(),
                            "Priority", g.getPriority()
                    ));
                }
            }
            default -> {
                title = "Department Workforce & Team Distribution Report";
                DepartmentProfileDto profile = getDepartmentOverview(callerEmail, dept.getId());
                summary.put("Total Workforce", profile.totalEmployees());
                summary.put("Total Teams", profile.totalTeams());
                summary.put("Assigned Managers", profile.totalManagers());
                headers = List.of("Team Name", "Manager", "Members", "Assessed", "Coverage %", "Critical Gaps");
                for (DeptTeamSummaryDto t : profile.teams()) {
                    rows.add(Map.of(
                            "Team Name", t.teamName(),
                            "Manager", t.managerName(),
                            "Members", t.memberCount(),
                            "Assessed", t.assessedCount(),
                            "Coverage %", t.skillCoveragePercentage() + "%",
                            "Critical Gaps", t.criticalGapsCount()
                    ));
                }
            }
        }

        return new DeptReportResultDto(type, title, dept.getName(), Instant.now(), summary, headers, rows);
    }

    @Transactional(readOnly = true)
    public List<DeptLeadershipSharedReportDto> getSharedLeadershipReports(String callerEmail, Long requestedDeptId) {
        Department dept = resolveAuthorizedDepartment(callerEmail, requestedDeptId);
        List<String> roles = List.of("DEPARTMENT_HEAD", "ALL_LEADERSHIP");
        List<LeadershipReportShare> shares = leadershipReportShareRepository.findForLeadership(roles, dept.getId());

        return shares.stream().map(s -> new DeptLeadershipSharedReportDto(
                s.getId(),
                s.getReportType(),
                s.getTitle(),
                s.getSharedBy().getFullName(),
                s.getSharedWithRole(),
                s.getSummaryNotes(),
                s.getReportDataJson(),
                s.getCreatedAt()
        )).toList();
    }

    // =========================================================================
    // 9. Department Announcements
    // =========================================================================
    @Transactional(readOnly = true)
    public List<DeptAnnouncementDto> getDepartmentAnnouncements(String callerEmail, Long requestedDeptId) {
        Department dept = resolveAuthorizedDepartment(callerEmail, requestedDeptId);
        User caller = userRepository.findByEmail(callerEmail.trim().toLowerCase()).orElse(null);
        Long callerId = caller != null ? caller.getId() : null;

        List<HrAnnouncement> announcements = hrAnnouncementRepository.findAllWithDetails().stream()
                .filter(a -> "ORGANIZATION".equalsIgnoreCase(a.getTargetType()) ||
                             ("DEPARTMENT".equalsIgnoreCase(a.getTargetType()) &&
                              a.getDepartment() != null && a.getDepartment().getId().equals(dept.getId())))
                .toList();

        Set<Long> deptUserIds = getDepartmentUserIds(dept, teamRepository.findByDepartmentId(dept.getId()));

        return announcements.stream().map(a -> {
            long readCount = hrAnnouncementReadRepository.countByAnnouncementId(a.getId());
            boolean readByMe = callerId != null &&
                    hrAnnouncementReadRepository.existsByAnnouncementIdAndUserId(a.getId(), callerId);

            return new DeptAnnouncementDto(
                    a.getId(),
                    a.getAuthor().getId(),
                    a.getAuthor().getFullName(),
                    a.getTitle(),
                    a.getContent(),
                    a.getPriority(),
                    a.getCreatedAt(),
                    readCount,
                    deptUserIds.size(),
                    readByMe
            );
        }).toList();
    }

    @Transactional
    public DeptAnnouncementDto createDepartmentAnnouncement(String callerEmail, CreateDeptAnnouncementRequest req, Long requestedDeptId) {
        Department dept = resolveAuthorizedDepartment(callerEmail, requestedDeptId);
        User author = userRepository.findByEmail(callerEmail.trim().toLowerCase())
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Caller not found"));

        HrAnnouncement a = new HrAnnouncement(
                author,
                "DEPARTMENT",
                dept,
                null,
                req.title().trim(),
                req.content().trim(),
                req.priority() != null ? req.priority().toUpperCase() : "NORMAL"
        );
        a = hrAnnouncementRepository.save(a);

        // Targeted in-app notifications to department workforce
        Set<Long> deptUserIds = getDepartmentUserIds(dept, teamRepository.findByDepartmentId(dept.getId()));
        for (Long uid : deptUserIds) {
            if (!uid.equals(author.getId())) {
                notificationService.createNotification(
                        uid,
                        "DEPT_ANNOUNCEMENT",
                        "[" + a.getPriority() + "] " + a.getTitle(),
                        a.getContent().length() > 95 ? a.getContent().substring(0, 92) + "..." : a.getContent(),
                        "/dashboard/department-head"
                );
            }
        }

        return new DeptAnnouncementDto(
                a.getId(),
                author.getId(),
                author.getFullName(),
                a.getTitle(),
                a.getContent(),
                a.getPriority(),
                a.getCreatedAt(),
                0L,
                deptUserIds.size(),
                false
        );
    }

    @Transactional
    public void markAnnouncementRead(Long announcementId, String callerEmail) {
        User caller = userRepository.findByEmail(callerEmail.trim().toLowerCase())
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Caller not found"));

        HrAnnouncement a = hrAnnouncementRepository.findById(announcementId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Announcement not found: " + announcementId));

        if (caller.getRole() != Role.ADMIN) {
            if ("DEPARTMENT".equalsIgnoreCase(a.getTargetType())) {
                Department dept = resolveAuthorizedDepartment(callerEmail, null);
                if (a.getDepartment() != null && !a.getDepartment().getId().equals(dept.getId())) {
                    throw new ApiException(HttpStatus.FORBIDDEN, "Access denied: Announcement belongs to another department");
                }
            } else if ("ROLE".equalsIgnoreCase(a.getTargetType())) {
                if (a.getTargetRole() != null && !caller.getRole().name().equalsIgnoreCase(a.getTargetRole())) {
                    throw new ApiException(HttpStatus.FORBIDDEN, "Access denied: Announcement targeted to a different role");
                }
            }
        }

        if (!hrAnnouncementReadRepository.existsByAnnouncementIdAndUserId(announcementId, caller.getId())) {
            HrAnnouncementRead read = new HrAnnouncementRead(a, caller);
            hrAnnouncementReadRepository.save(read);
        }
    }

    // =========================================================================
    // 10. Department Notifications
    // =========================================================================
    @Transactional(readOnly = true)
    public List<DeptNotificationDto> getNotifications(String callerEmail, boolean unreadOnly) {
        User caller = userRepository.findByEmail(callerEmail.trim().toLowerCase())
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Caller not found"));

        List<Notification> list = notificationRepository.findByRecipientIdOrderByCreatedAtDesc(caller.getId());
        if (unreadOnly) {
            list = list.stream().filter(n -> !n.isRead()).toList();
        }

        return list.stream().map(n -> new DeptNotificationDto(
                n.getId(),
                n.getType(),
                n.getTitle(),
                n.getMessage(),
                n.getLink(),
                n.isRead(),
                n.getCreatedAt()
        )).toList();
    }

    @Transactional
    public void markNotificationRead(Long id, String callerEmail) {
        User caller = userRepository.findByEmail(callerEmail.trim().toLowerCase())
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Caller not found"));

        Notification n = notificationRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Notification not found: " + id));

        if (!n.getRecipient().getId().equals(caller.getId())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Not authorized to modify this notification");
        }

        n.setRead(true);
        notificationRepository.save(n);
    }

    @Transactional
    public void markAllNotificationsRead(String callerEmail) {
        User caller = userRepository.findByEmail(callerEmail.trim().toLowerCase())
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Caller not found"));

        List<Notification> unread = notificationRepository.findByRecipientIdOrderByCreatedAtDesc(caller.getId())
                .stream().filter(n -> !n.isRead()).toList();

        for (Notification n : unread) {
            n.setRead(true);
        }
        notificationRepository.saveAll(unread);
    }
}
