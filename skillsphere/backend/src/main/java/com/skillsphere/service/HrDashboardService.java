package com.skillsphere.service;

import com.skillsphere.dto.HrDtos.*;
import com.skillsphere.entity.*;
import com.skillsphere.exception.ApiException;
import com.skillsphere.repository.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class HrDashboardService {

    private final UserRepository userRepository;
    private final EmployeeProfileRepository employeeProfileRepository;
    private final EmployeeSkillRepository employeeSkillRepository;
    private final SkillRepository skillRepository;
    private final SkillCategoryRepository categoryRepository;
    private final RoleSkillRequirementRepository requirementRepository;
    private final SkillGapRepository skillGapRepository;
    private final CourseRepository courseRepository;
    private final LearningPathRepository learningPathRepository;
    private final LearningPathItemRepository learningPathItemRepository;
    private final TrainingProgrammeRepository trainingProgrammeRepository;
    private final TrainingEnrollmentRepository trainingEnrollmentRepository;
    private final EmployeeLearningRepository employeeLearningRepository;
    private final MentorProfileRepository mentorProfileRepository;
    private final MentorAssignmentRepository mentorAssignmentRepository;
    private final MentorshipSessionRepository mentorshipSessionRepository;
    private final MentorshipFeedbackRepository mentorshipFeedbackRepository;
    private final PlatformFeedbackRepository platformFeedbackRepository;
    private final AssessmentAttemptRepository assessmentAttemptRepository;
    private final DepartmentRepository departmentRepository;
    private final HrAnnouncementRepository hrAnnouncementRepository;
    private final HrAnnouncementReadRepository hrAnnouncementReadRepository;
    private final LeadershipReportShareRepository leadershipReportShareRepository;
    private final NotificationService notificationService;

    public HrDashboardService(UserRepository userRepository,
                              EmployeeProfileRepository employeeProfileRepository,
                              EmployeeSkillRepository employeeSkillRepository,
                              SkillRepository skillRepository,
                              SkillCategoryRepository categoryRepository,
                              RoleSkillRequirementRepository requirementRepository,
                              SkillGapRepository skillGapRepository,
                              CourseRepository courseRepository,
                              LearningPathRepository learningPathRepository,
                              LearningPathItemRepository learningPathItemRepository,
                              TrainingProgrammeRepository trainingProgrammeRepository,
                              TrainingEnrollmentRepository trainingEnrollmentRepository,
                              EmployeeLearningRepository employeeLearningRepository,
                              MentorProfileRepository mentorProfileRepository,
                              MentorAssignmentRepository mentorAssignmentRepository,
                              MentorshipSessionRepository mentorshipSessionRepository,
                              MentorshipFeedbackRepository mentorshipFeedbackRepository,
                              PlatformFeedbackRepository platformFeedbackRepository,
                              AssessmentAttemptRepository assessmentAttemptRepository,
                              DepartmentRepository departmentRepository,
                              HrAnnouncementRepository hrAnnouncementRepository,
                              HrAnnouncementReadRepository hrAnnouncementReadRepository,
                              LeadershipReportShareRepository leadershipReportShareRepository,
                              NotificationService notificationService) {
        this.userRepository = userRepository;
        this.employeeProfileRepository = employeeProfileRepository;
        this.employeeSkillRepository = employeeSkillRepository;
        this.skillRepository = skillRepository;
        this.categoryRepository = categoryRepository;
        this.requirementRepository = requirementRepository;
        this.skillGapRepository = skillGapRepository;
        this.courseRepository = courseRepository;
        this.learningPathRepository = learningPathRepository;
        this.learningPathItemRepository = learningPathItemRepository;
        this.trainingProgrammeRepository = trainingProgrammeRepository;
        this.trainingEnrollmentRepository = trainingEnrollmentRepository;
        this.employeeLearningRepository = employeeLearningRepository;
        this.mentorProfileRepository = mentorProfileRepository;
        this.mentorAssignmentRepository = mentorAssignmentRepository;
        this.mentorshipSessionRepository = mentorshipSessionRepository;
        this.mentorshipFeedbackRepository = mentorshipFeedbackRepository;
        this.platformFeedbackRepository = platformFeedbackRepository;
        this.assessmentAttemptRepository = assessmentAttemptRepository;
        this.departmentRepository = departmentRepository;
        this.hrAnnouncementRepository = hrAnnouncementRepository;
        this.hrAnnouncementReadRepository = hrAnnouncementReadRepository;
        this.leadershipReportShareRepository = leadershipReportShareRepository;
        this.notificationService = notificationService;
    }

    // =========================================================================
    // 1. Overview / Executive Summary
    // =========================================================================
    @Transactional(readOnly = true)
    public HrDashboardOverviewDto getOverview() {
        List<User> allUsers = userRepository.findAll();
        List<User> employees = allUsers.stream()
                .filter(u -> u.getRole() == Role.EMPLOYEE)
                .toList();
        long totalEmployees = employees.size();

        List<AssessmentAttempt> attempts = assessmentAttemptRepository.findAllWithDetails();
        Set<Long> assessedUserIds = attempts.stream()
                .filter(a -> "COMPLETED".equalsIgnoreCase(a.getStatus()))
                .map(a -> a.getUser().getId())
                .collect(Collectors.toSet());

        long assessedCount = employees.stream().filter(e -> assessedUserIds.contains(e.getId())).count();
        long unassessedCount = Math.max(0, totalEmployees - assessedCount);
        double skillCoveragePct = totalEmployees > 0 ? Math.round((assessedCount * 100.0 / totalEmployees) * 10.0) / 10.0 : 0.0;

        List<SkillGap> allGaps = skillGapRepository.findAll();
        long totalGaps = allGaps.size();
        long criticalShortages = allGaps.stream()
                .filter(g -> "HIGH".equalsIgnoreCase(g.getPriority()) || g.getGapSize() >= 2)
                .count();

        List<TrainingProgramme> programmes = trainingProgrammeRepository.findAllWithDetails();
        long activeProgrammesCount = programmes.stream()
                .filter(p -> "UPCOMING".equalsIgnoreCase(p.getStatus()) || "ACTIVE".equalsIgnoreCase(p.getStatus()))
                .count();

        long totalCourses = courseRepository.count();

        List<EmployeeLearning> learnings = employeeLearningRepository.findAllWithDetails();
        long totalEnrollments = learnings.size();
        long completedEnrollments = learnings.stream()
                .filter(l -> "COMPLETED".equalsIgnoreCase(l.getStatus()) || "CERTIFIED".equalsIgnoreCase(l.getStatus()))
                .count();
        double avgCompletionRate = totalEnrollments > 0 ? Math.round((completedEnrollments * 100.0 / totalEnrollments) * 10.0) / 10.0 : 0.0;

        List<HrUpcomingProgrammeDto> upcomingProgrammes = programmes.stream()
                .filter(p -> !"COMPLETED".equalsIgnoreCase(p.getStatus()) && !"CANCELLED".equalsIgnoreCase(p.getStatus()))
                .sorted(Comparator.comparing(TrainingProgramme::getStartDate))
                .limit(5)
                .map(p -> new HrUpcomingProgrammeDto(
                        p.getId(),
                        p.getTitle(),
                        p.getDepartment() != null ? p.getDepartment().getName() : "All Departments",
                        p.getStartDate() != null ? p.getStartDate().toString() : "TBD",
                        p.getEndDate() != null ? p.getEndDate().toString() : "TBD",
                        p.getEnrolledCount(),
                        p.getCapacity(),
                        p.getStatus()
                ))
                .toList();

        List<HrRecentActivityDto> recentActivities = new ArrayList<>();
        learnings.stream()
                .filter(l -> l.getCompletedAt() != null)
                .sorted(Comparator.comparing(EmployeeLearning::getCompletedAt).reversed())
                .limit(4)
                .forEach(l -> recentActivities.add(new HrRecentActivityDto(
                        "COURSE_COMPLETION",
                        "Course Completed",
                        l.getUser().getFullName() + " completed " + l.getCourse().getTitle(),
                        l.getCompletedAt(),
                        l.getUser().getFullName()
                )));

        attempts.stream()
                .filter(a -> "COMPLETED".equalsIgnoreCase(a.getStatus()) && a.getCompletedAt() != null)
                .sorted(Comparator.comparing(AssessmentAttempt::getCompletedAt).reversed())
                .limit(4)
                .forEach(a -> recentActivities.add(new HrRecentActivityDto(
                        "ASSESSMENT_COMPLETED",
                        "Assessment Completed",
                        a.getUser().getFullName() + " scored " + (a.getScore() != null ? a.getScore() : 0) + "% on " + a.getSkill().getName(),
                        a.getCompletedAt(),
                        a.getUser().getFullName()
                )));

        recentActivities.sort(Comparator.comparing(HrRecentActivityDto::timestamp).reversed());
        List<HrRecentActivityDto> topActivities = recentActivities.stream().limit(6).toList();

        List<HrPendingActionDto> pendingActions = new ArrayList<>();
        if (unassessedCount > 0) {
            pendingActions.add(new HrPendingActionDto(
                    "UNASSESSED_WORKFORCE",
                    "Unassessed Employees",
                    unassessedCount + " employees have not yet completed skill baseline assessments",
                    "employees",
                    "HIGH"
            ));
        }
        if (criticalShortages > 0) {
            pendingActions.add(new HrPendingActionDto(
                    "CRITICAL_SHORTAGES",
                    "Critical Skill Shortages",
                    criticalShortages + " high-priority skill gaps identified across the organization",
                    "skill-gaps",
                    "HIGH"
            ));
        }
        long draftOrUpcomingProgrammes = programmes.stream().filter(p -> "UPCOMING".equalsIgnoreCase(p.getStatus())).count();
        if (draftOrUpcomingProgrammes > 0) {
            pendingActions.add(new HrPendingActionDto(
                    "UPCOMING_TRAINING",
                    "Programmes Ready for Enrollment",
                    draftOrUpcomingProgrammes + " training programmes scheduled for upcoming delivery",
                    "training-programmes",
                    "MEDIUM"
            ));
        }

        return new HrDashboardOverviewDto(
                totalEmployees,
                assessedCount,
                unassessedCount,
                skillCoveragePct,
                criticalShortages,
                totalGaps,
                activeProgrammesCount,
                totalCourses,
                totalEnrollments,
                avgCompletionRate,
                upcomingProgrammes,
                topActivities,
                pendingActions
        );
    }

    // =========================================================================
    // 2. Employee Directory & Dossiers
    // =========================================================================
    @Transactional(readOnly = true)
    public List<HrEmployeeDirectoryItemDto> getEmployeeDirectory(String search, String dept, String roleFilter) {
        List<User> users = userRepository.findAll();
        Map<Long, EmployeeProfile> profileMap = employeeProfileRepository.findAllWithUser().stream()
                .collect(Collectors.toMap(p -> p.getUser().getId(), p -> p, (a, b) -> a));

        List<AssessmentAttempt> attempts = assessmentAttemptRepository.findAllWithDetails();
        Set<Long> assessedIds = attempts.stream()
                .filter(a -> "COMPLETED".equalsIgnoreCase(a.getStatus()))
                .map(a -> a.getUser().getId())
                .collect(Collectors.toSet());

        List<EmployeeSkill> skills = employeeSkillRepository.findAll();
        Map<Long, Long> verifiedCountMap = skills.stream()
                .filter(EmployeeSkill::isVerified)
                .collect(Collectors.groupingBy(s -> s.getUser().getId(), Collectors.counting()));

        List<EmployeeLearning> learnings = employeeLearningRepository.findAllWithDetails();
        Map<Long, Long> enrolledCountMap = learnings.stream()
                .collect(Collectors.groupingBy(l -> l.getUser().getId(), Collectors.counting()));

        return users.stream()
                .filter(u -> {
                    if (search != null && !search.isBlank()) {
                        String s = search.toLowerCase();
                        boolean matchName = u.getFullName() != null && u.getFullName().toLowerCase().contains(s);
                        boolean matchEmail = u.getEmail() != null && u.getEmail().toLowerCase().contains(s);
                        if (!matchName && !matchEmail) return false;
                    }
                    EmployeeProfile prof = profileMap.get(u.getId());
                    if (dept != null && !dept.isBlank() && !"ALL".equalsIgnoreCase(dept)) {
                        String userDept = prof != null ? prof.getDepartment() : null;
                        if (userDept == null || !userDept.equalsIgnoreCase(dept)) return false;
                    }
                    if (roleFilter != null && !roleFilter.isBlank() && !"ALL".equalsIgnoreCase(roleFilter)) {
                        if (!u.getRole().name().equalsIgnoreCase(roleFilter)) return false;
                    }
                    return true;
                })
                .sorted(Comparator.comparing(User::getFullName, Comparator.nullsLast(String::compareToIgnoreCase)))
                .map(u -> {
                    EmployeeProfile prof = profileMap.get(u.getId());
                    String department = prof != null && prof.getDepartment() != null ? prof.getDepartment() : "General";
                    String jobRole = prof != null && prof.getJobRole() != null ? prof.getJobRole() : "Staff";
                    String experienceLevel = prof != null && prof.getExperienceLevel() != null ? prof.getExperienceLevel() : "Entry level";
                    boolean isAssessed = assessedIds.contains(u.getId());
                    int verifiedCount = verifiedCountMap.getOrDefault(u.getId(), 0L).intValue();
                    int enrolledCount = enrolledCountMap.getOrDefault(u.getId(), 0L).intValue();
                    return new HrEmployeeDirectoryItemDto(
                            u.getId(),
                            u.getFullName(),
                            u.getEmail(),
                            u.getRole().name(),
                            department,
                            jobRole,
                            experienceLevel,
                            isAssessed,
                            verifiedCount,
                            enrolledCount,
                            u.getCreatedAt()
                    );
                })
                .toList();
    }

    @Transactional(readOnly = true)
    public HrEmployeeDossierDto getEmployeeDossier(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Employee not found: " + userId));

        EmployeeProfile profile = employeeProfileRepository.findByUserId(userId).orElse(null);
        String department = profile != null && profile.getDepartment() != null ? profile.getDepartment() : "General";
        String jobRole = profile != null && profile.getJobRole() != null ? profile.getJobRole() : "Staff";
        String experienceLevel = profile != null && profile.getExperienceLevel() != null ? profile.getExperienceLevel() : "Entry level";
        String bio = profile != null ? profile.getBio() : "";

        // Skills
        List<EmployeeSkill> employeeSkills = employeeSkillRepository.findByUserId(userId);
        List<HrEmployeeSkillDto> skillDtos = employeeSkills.stream()
                .map(es -> new HrEmployeeSkillDto(
                        es.getSkill().getId(),
                        es.getSkill().getName(),
                        es.getSkill().getCategory() != null ? es.getSkill().getCategory().getName() : "General",
                        es.getProficiencyLevel(),
                        es.isVerified(),
                        es.getUpdatedAt()
                ))
                .toList();

        // Assessment history (scores, dates, skill names ONLY — no question details or raw answers)
        List<AssessmentAttempt> attempts = assessmentAttemptRepository.findByUserIdOrderByStartedAtDesc(userId);
        List<HrAssessmentSummaryDto> assessmentDtos = attempts.stream()
                .map(a -> new HrAssessmentSummaryDto(
                        a.getId(),
                        a.getSkill().getName(),
                        a.getTargetLevel(),
                        a.getScore(),
                        a.getPassed(),
                        a.getStatus(),
                        a.getCompletedAt()
                ))
                .toList();

        // Course enrollments
        List<EmployeeLearning> learnings = employeeLearningRepository.findByUserId(userId);
        List<HrEmployeeLearningProgressDto> learningDtos = learnings.stream()
                .map(l -> new HrEmployeeLearningProgressDto(
                        l.getId(),
                        l.getCourse().getId(),
                        l.getCourse().getTitle(),
                        l.getStatus(),
                        l.getProgressPct(),
                        l.getEnrolledAt(),
                        l.getCompletedAt()
                ))
                .toList();

        // Training programmes
        List<TrainingEnrollment> programmeEnrollments = trainingEnrollmentRepository.findByUserId(userId);
        List<HrTrainingEnrollmentDto> trainingDtos = programmeEnrollments.stream()
                .map(te -> new HrTrainingEnrollmentDto(
                        te.getId(),
                        te.getProgramme().getId(),
                        te.getProgramme().getTitle(),
                        te.getStatus(),
                        te.getEnrolledAt()
                ))
                .toList();

        // Mentorship participations (Zero chat messages, Zero prep notes)
        List<MentorAssignment> assignments = mentorAssignmentRepository.findByEmployeeIdOrderByAssignedAtDesc(userId);
        List<HrMentorshipSummaryDto> mentorshipDtos = assignments.stream()
                .map(ma -> new HrMentorshipSummaryDto(
                        ma.getId(),
                        ma.getMentor().getFullName(),
                        ma.getEmployee().getFullName(),
                        "Mentee",
                        ma.getStatus().name(),
                        ma.getAssignedAt()
                ))
                .toList();

        return new HrEmployeeDossierDto(
                user.getId(),
                user.getFullName(),
                user.getEmail(),
                user.getRole().name(),
                department,
                jobRole,
                experienceLevel,
                bio,
                skillDtos,
                assessmentDtos,
                learningDtos,
                trainingDtos,
                mentorshipDtos
        );
    }

    // =========================================================================
    // 3. Competency Framework
    // =========================================================================
    @Transactional(readOnly = true)
    public List<HrCompetencyDto> getCompetencies() {
        List<Skill> skills = skillRepository.findAll();
        List<RoleSkillRequirement> requirements = requirementRepository.findAll();
        List<Course> courses = courseRepository.findAll();
        List<LearningPath> paths = learningPathRepository.findAll();

        Map<Long, Long> roleCountMap = requirements.stream()
                .filter(r -> r.getSkill() != null)
                .collect(Collectors.groupingBy(r -> r.getSkill().getId(), Collectors.counting()));

        Map<Long, Long> courseCountMap = courses.stream()
                .filter(c -> c.getSkill() != null)
                .collect(Collectors.groupingBy(c -> c.getSkill().getId(), Collectors.counting()));

        Map<Long, Long> pathCountMap = paths.stream()
                .filter(p -> p.getSkill() != null)
                .collect(Collectors.groupingBy(p -> p.getSkill().getId(), Collectors.counting()));

        return skills.stream()
                .sorted(Comparator.comparing(Skill::getName, String::compareToIgnoreCase))
                .map(s -> new HrCompetencyDto(
                        s.getId(),
                        s.getName(),
                        s.getCategory() != null ? s.getCategory().getName() : "Uncategorized",
                        s.getCategory() != null ? s.getCategory().getId() : null,
                        s.getDescription(),
                        s.isActive(),
                        roleCountMap.getOrDefault(s.getId(), 0L),
                        courseCountMap.getOrDefault(s.getId(), 0L),
                        pathCountMap.getOrDefault(s.getId(), 0L),
                        s.getCreatedAt()
                ))
                .toList();
    }

    @Transactional
    public HrCompetencyDto createCompetency(CreateCompetencyRequest req) {
        SkillCategory category = categoryRepository.findById(req.categoryId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Category not found: " + req.categoryId()));

        if (skillRepository.findByNameIgnoreCase(req.name().trim()).isPresent()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Skill competency with this name already exists: " + req.name());
        }

        Skill skill = new Skill(category, req.name().trim(), req.description());
        skill = skillRepository.save(skill);

        return new HrCompetencyDto(
                skill.getId(),
                skill.getName(),
                category.getName(),
                category.getId(),
                skill.getDescription(),
                skill.isActive(),
                0L,
                0L,
                0L,
                skill.getCreatedAt()
        );
    }

    @Transactional
    public HrCompetencyDto updateCompetency(Long id, UpdateCompetencyRequest req) {
        Skill skill = skillRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Skill not found: " + id));

        SkillCategory category = categoryRepository.findById(req.categoryId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Category not found: " + req.categoryId()));

        skillRepository.findByNameIgnoreCase(req.name().trim()).ifPresent(existing -> {
            if (!existing.getId().equals(id)) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "Another competency with this name already exists");
            }
        });

        skill.setName(req.name().trim());
        skill.setCategory(category);
        skill.setDescription(req.description());
        skill.setActive(req.isActive());
        skill = skillRepository.save(skill);

        return new HrCompetencyDto(
                skill.getId(),
                skill.getName(),
                category.getName(),
                category.getId(),
                skill.getDescription(),
                skill.isActive(),
                requirementRepository.findAll().stream().filter(r -> r.getSkill().getId().equals(id)).count(),
                courseRepository.findAll().stream().filter(c -> c.getSkill() != null && c.getSkill().getId().equals(id)).count(),
                learningPathRepository.findAll().stream().filter(p -> p.getSkill() != null && p.getSkill().getId().equals(id)).count(),
                skill.getCreatedAt()
        );
    }

    @Transactional
    public HrCompetencyDto toggleCompetencyActive(Long id) {
        Skill skill = skillRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Skill not found: " + id));
        skill.setActive(!skill.isActive());
        skill = skillRepository.save(skill);

        return new HrCompetencyDto(
                skill.getId(),
                skill.getName(),
                skill.getCategory() != null ? skill.getCategory().getName() : "General",
                skill.getCategory() != null ? skill.getCategory().getId() : null,
                skill.getDescription(),
                skill.isActive(),
                0L, 0L, 0L,
                skill.getCreatedAt()
        );
    }

    // =========================================================================
    // 4. Role Requirements
    // =========================================================================
    @Transactional(readOnly = true)
    public List<HrRoleRequirementDto> getRoleRequirements(String jobRole) {
        List<RoleSkillRequirement> list;
        if (jobRole != null && !jobRole.isBlank() && !"ALL".equalsIgnoreCase(jobRole)) {
            list = requirementRepository.findByJobRole(jobRole);
        } else {
            list = requirementRepository.findAll();
        }

        return list.stream()
                .sorted(Comparator.comparing(RoleSkillRequirement::getJobRole)
                        .thenComparing(r -> r.getSkill().getName()))
                .map(r -> new HrRoleRequirementDto(
                        r.getId(),
                        r.getJobRole(),
                        r.getSkill().getId(),
                        r.getSkill().getName(),
                        r.getSkill().getCategory() != null ? r.getSkill().getCategory().getName() : "General",
                        r.getRequiredProficiency(),
                        r.getPriority(),
                        r.isActive(),
                        r.getCreatedAt()
                ))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<RoleSkillMatrixDto> getRoleMatrix() {
        List<RoleSkillRequirement> requirements = requirementRepository.findAll();
        Map<String, List<RoleSkillRequirement>> grouped = requirements.stream()
                .collect(Collectors.groupingBy(RoleSkillRequirement::getJobRole));

        return grouped.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> {
                    String jobRole = entry.getKey();
                    List<RoleSkillRequirement> reqs = entry.getValue();
                    int total = reqs.size();
                    int highPriority = (int) reqs.stream().filter(r -> "HIGH".equalsIgnoreCase(r.getPriority())).count();
                    List<HrRoleRequirementDto> dtoList = reqs.stream()
                            .map(r -> new HrRoleRequirementDto(
                                    r.getId(),
                                    r.getJobRole(),
                                    r.getSkill().getId(),
                                    r.getSkill().getName(),
                                    r.getSkill().getCategory() != null ? r.getSkill().getCategory().getName() : "General",
                                    r.getRequiredProficiency(),
                                    r.getPriority(),
                                    r.isActive(),
                                    r.getCreatedAt()
                            ))
                            .toList();
                    return new RoleSkillMatrixDto(jobRole, total, highPriority, dtoList);
                })
                .toList();
    }

    @Transactional
    public HrRoleRequirementDto createRoleRequirement(CreateRoleRequirementRequest req) {
        Skill skill = skillRepository.findById(req.skillId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Skill not found: " + req.skillId()));

        requirementRepository.findByJobRoleAndSkillId(req.jobRole().trim(), req.skillId()).ifPresent(existing -> {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Requirement already exists for role " + req.jobRole() + " and skill " + skill.getName());
        });

        RoleSkillRequirement r = new RoleSkillRequirement(
                req.jobRole().trim(),
                skill,
                req.requiredProficiency(),
                req.priority()
        );
        r = requirementRepository.save(r);

        return new HrRoleRequirementDto(
                r.getId(),
                r.getJobRole(),
                skill.getId(),
                skill.getName(),
                skill.getCategory() != null ? skill.getCategory().getName() : "General",
                r.getRequiredProficiency(),
                r.getPriority(),
                r.isActive(),
                r.getCreatedAt()
        );
    }

    @Transactional
    public HrRoleRequirementDto updateRoleRequirement(Long id, UpdateRoleRequirementRequest req) {
        RoleSkillRequirement r = requirementRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Role requirement not found: " + id));

        r.setRequiredProficiency(req.requiredProficiency());
        r.setPriority(req.priority());
        r.setActive(req.isActive());
        r = requirementRepository.save(r);

        return new HrRoleRequirementDto(
                r.getId(),
                r.getJobRole(),
                r.getSkill().getId(),
                r.getSkill().getName(),
                r.getSkill().getCategory() != null ? r.getSkill().getCategory().getName() : "General",
                r.getRequiredProficiency(),
                r.getPriority(),
                r.isActive(),
                r.getCreatedAt()
        );
    }

    @Transactional
    public void deleteRoleRequirement(Long id) {
        if (!requirementRepository.existsById(id)) {
            throw new ApiException(HttpStatus.NOT_FOUND, "Role requirement not found: " + id);
        }
        requirementRepository.deleteById(id);
    }

    // =========================================================================
    // 5. Organisation Skill Gap Analysis
    // =========================================================================
    @Transactional(readOnly = true)
    public HrSkillGapSummaryDto getSkillGapSummary() {
        List<SkillGap> gaps = skillGapRepository.findAll();
        long totalGaps = gaps.size();
        long highPriority = gaps.stream().filter(g -> "HIGH".equalsIgnoreCase(g.getPriority())).count();
        long mediumPriority = gaps.stream().filter(g -> "MEDIUM".equalsIgnoreCase(g.getPriority())).count();
        long lowPriority = gaps.stream().filter(g -> "LOW".equalsIgnoreCase(g.getPriority())).count();
        double avgGapSize = totalGaps > 0 ? gaps.stream().mapToInt(SkillGap::getGapSize).average().orElse(0.0) : 0.0;
        avgGapSize = Math.round(avgGapSize * 10.0) / 10.0;

        // Department breakdown
        List<EmployeeProfile> profiles = employeeProfileRepository.findAllWithUser();
        Map<Long, String> userDeptMap = profiles.stream()
                .collect(Collectors.toMap(p -> p.getUser().getId(), p -> p.getDepartment() != null ? p.getDepartment() : "General", (a, b) -> a));

        Map<String, List<SkillGap>> deptGapsMap = gaps.stream()
                .collect(Collectors.groupingBy(g -> userDeptMap.getOrDefault(g.getUser().getId(), "General")));

        Map<String, Long> deptEmployeeCountMap = profiles.stream()
                .collect(Collectors.groupingBy(p -> p.getDepartment() != null ? p.getDepartment() : "General", Collectors.counting()));

        List<DepartmentSkillGapDto> deptList = deptGapsMap.entrySet().stream()
                .map(e -> {
                    String deptName = e.getKey();
                    List<SkillGap> dGaps = e.getValue();
                    int empCount = deptEmployeeCountMap.getOrDefault(deptName, 0L).intValue();
                    int gCount = dGaps.size();
                    int highCount = (int) dGaps.stream().filter(g -> "HIGH".equalsIgnoreCase(g.getPriority())).count();
                    List<String> topShortages = dGaps.stream()
                            .filter(g -> "HIGH".equalsIgnoreCase(g.getPriority()))
                            .map(g -> g.getSkill().getName())
                            .distinct()
                            .limit(3)
                            .toList();
                    return new DepartmentSkillGapDto(deptName, empCount, gCount, highCount, topShortages);
                })
                .sorted(Comparator.comparing(DepartmentSkillGapDto::highPriorityCount).reversed())
                .toList();

        // Critical Shortages
        Map<Skill, List<SkillGap>> skillGapsMap = gaps.stream()
                .collect(Collectors.groupingBy(SkillGap::getSkill));

        List<CriticalShortageDto> criticalShortages = skillGapsMap.entrySet().stream()
                .filter(e -> e.getValue().stream().anyMatch(g -> "HIGH".equalsIgnoreCase(g.getPriority()) || g.getGapSize() >= 2))
                .map(e -> {
                    Skill s = e.getKey();
                    List<SkillGap> sGaps = e.getValue();
                    long affected = sGaps.size();
                    double avgCurr = Math.round(sGaps.stream().mapToInt(SkillGap::getCurrentLevel).average().orElse(0.0) * 10.0) / 10.0;
                    double avgReq = Math.round(sGaps.stream().mapToInt(SkillGap::getRequiredLevel).average().orElse(0.0) * 10.0) / 10.0;
                    double avgGap = Math.round(sGaps.stream().mapToInt(SkillGap::getGapSize).average().orElse(0.0) * 10.0) / 10.0;
                    return new CriticalShortageDto(
                            s.getId(),
                            s.getName(),
                            s.getCategory() != null ? s.getCategory().getName() : "General",
                            affected,
                            avgCurr,
                            avgReq,
                            avgGap
                    );
                })
                .sorted(Comparator.comparing(CriticalShortageDto::avgGap).reversed())
                .limit(8)
                .toList();

        // Top Gap Skills
        List<TopGapSkillDto> topGapSkills = skillGapsMap.entrySet().stream()
                .map(e -> {
                    Skill s = e.getKey();
                    List<SkillGap> sGaps = e.getValue();
                    double avgGap = Math.round(sGaps.stream().mapToInt(SkillGap::getGapSize).average().orElse(0.0) * 10.0) / 10.0;
                    return new TopGapSkillDto(
                            s.getId(),
                            s.getName(),
                            s.getCategory() != null ? s.getCategory().getName() : "General",
                            sGaps.size(),
                            avgGap
                    );
                })
                .sorted(Comparator.comparing(TopGapSkillDto::gapCount).reversed())
                .limit(10)
                .toList();

        return new HrSkillGapSummaryDto(
                totalGaps,
                highPriority,
                mediumPriority,
                lowPriority,
                avgGapSize,
                deptList,
                criticalShortages,
                topGapSkills
        );
    }

    @Transactional(readOnly = true)
    public OrgHeatmapDto getOrgHeatmap() {
        List<EmployeeProfile> profiles = employeeProfileRepository.findAllWithUser();
        List<Skill> skills = skillRepository.findAll();
        List<SkillGap> gaps = skillGapRepository.findAll();

        Map<Long, String> userDeptMap = profiles.stream()
                .collect(Collectors.toMap(p -> p.getUser().getId(), p -> p.getDepartment() != null ? p.getDepartment() : "General", (a, b) -> a));

        List<String> departments = profiles.stream()
                .map(p -> p.getDepartment() != null ? p.getDepartment() : "General")
                .distinct()
                .sorted()
                .toList();

        List<String> skillNames = skills.stream()
                .map(Skill::getName)
                .sorted()
                .limit(12)
                .toList();

        List<OrgHeatmapCellDto> cells = new ArrayList<>();
        for (String dept : departments) {
            for (String skillName : skillNames) {
                List<SkillGap> match = gaps.stream()
                        .filter(g -> dept.equalsIgnoreCase(userDeptMap.get(g.getUser().getId())) &&
                                     skillName.equalsIgnoreCase(g.getSkill().getName()))
                        .toList();
                if (match.isEmpty()) {
                    cells.add(new OrgHeatmapCellDto(dept, skillName, 3.0, 0, "OPTIMAL"));
                } else {
                    double avgCurr = match.stream().mapToInt(SkillGap::getCurrentLevel).average().orElse(2.0);
                    int count = match.size();
                    String status = count >= 3 ? "CRITICAL" : (count >= 1 ? "MODERATE" : "LOW");
                    cells.add(new OrgHeatmapCellDto(dept, skillName, Math.round(avgCurr * 10.0) / 10.0, count, status));
                }
            }
        }

        return new OrgHeatmapDto(departments, skillNames, cells);
    }

    // =========================================================================
    // 6. Learning Catalogue
    // =========================================================================
    @Transactional(readOnly = true)
    public List<HrCourseDto> getCourses() {
        List<Course> courses = courseRepository.findAll();
        List<EmployeeLearning> learnings = employeeLearningRepository.findAllWithDetails();

        Map<Long, Long> enrolledMap = learnings.stream()
                .collect(Collectors.groupingBy(l -> l.getCourse().getId(), Collectors.counting()));

        Map<Long, Long> completedMap = learnings.stream()
                .filter(l -> "COMPLETED".equalsIgnoreCase(l.getStatus()) || "CERTIFIED".equalsIgnoreCase(l.getStatus()))
                .collect(Collectors.groupingBy(l -> l.getCourse().getId(), Collectors.counting()));

        return courses.stream()
                .sorted(Comparator.comparing(Course::getTitle, String::compareToIgnoreCase))
                .map(c -> new HrCourseDto(
                        c.getId(),
                        c.getTitle(),
                        c.getDescription(),
                        c.getLevel(),
                        c.getDurationHours(),
                        c.getProvider(),
                        c.getCourseUrl(),
                        c.getThumbnailUrl(),
                        c.getSkill() != null ? c.getSkill().getId() : null,
                        c.getSkill() != null ? c.getSkill().getName() : null,
                        c.isActive(),
                        enrolledMap.getOrDefault(c.getId(), 0L),
                        completedMap.getOrDefault(c.getId(), 0L),
                        c.getCreatedAt()
                ))
                .toList();
    }

    @Transactional
    public HrCourseDto createCourse(CreateOrUpdateCourseRequest req) {
        Skill skill = null;
        if (req.skillId() != null) {
            skill = skillRepository.findById(req.skillId()).orElse(null);
        }

        Course course = new Course(
                req.title().trim(),
                req.description(),
                skill,
                req.level(),
                req.durationHours(),
                req.provider().trim(),
                req.courseUrl(),
                req.thumbnailUrl()
        );
        course.setActive(req.isActive());
        course = courseRepository.save(course);

        return new HrCourseDto(
                course.getId(),
                course.getTitle(),
                course.getDescription(),
                course.getLevel(),
                course.getDurationHours(),
                course.getProvider(),
                course.getCourseUrl(),
                course.getThumbnailUrl(),
                skill != null ? skill.getId() : null,
                skill != null ? skill.getName() : null,
                course.isActive(),
                0L, 0L,
                course.getCreatedAt()
        );
    }

    @Transactional
    public HrCourseDto updateCourse(Long id, CreateOrUpdateCourseRequest req) {
        Course course = courseRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Course not found: " + id));

        Skill skill = null;
        if (req.skillId() != null) {
            skill = skillRepository.findById(req.skillId()).orElse(null);
        }

        course.setTitle(req.title().trim());
        course.setDescription(req.description());
        course.setSkill(skill);
        course.setLevel(req.level());
        course.setDurationHours(req.durationHours());
        course.setProvider(req.provider().trim());
        course.setCourseUrl(req.courseUrl());
        course.setThumbnailUrl(req.thumbnailUrl());
        course.setActive(req.isActive());
        course = courseRepository.save(course);

        return new HrCourseDto(
                course.getId(),
                course.getTitle(),
                course.getDescription(),
                course.getLevel(),
                course.getDurationHours(),
                course.getProvider(),
                course.getCourseUrl(),
                course.getThumbnailUrl(),
                skill != null ? skill.getId() : null,
                skill != null ? skill.getName() : null,
                course.isActive(),
                0L, 0L,
                course.getCreatedAt()
        );
    }

    @Transactional
    public HrCourseDto toggleCourseActive(Long id) {
        Course course = courseRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Course not found: " + id));
        course.setActive(!course.isActive());
        course = courseRepository.save(course);

        return new HrCourseDto(
                course.getId(),
                course.getTitle(),
                course.getDescription(),
                course.getLevel(),
                course.getDurationHours(),
                course.getProvider(),
                course.getCourseUrl(),
                course.getThumbnailUrl(),
                course.getSkill() != null ? course.getSkill().getId() : null,
                course.getSkill() != null ? course.getSkill().getName() : null,
                course.isActive(),
                0L, 0L,
                course.getCreatedAt()
        );
    }

    // =========================================================================
    // 7. Learning Paths
    // =========================================================================
    @Transactional(readOnly = true)
    public List<HrLearningPathDto> getLearningPaths() {
        List<LearningPath> paths = learningPathRepository.findAllWithDetails();

        return paths.stream().map(p -> {
            List<LearningPathItem> items = learningPathItemRepository.findByLearningPathIdOrderByStepOrderAsc(p.getId());
            int totalHours = items.stream().mapToInt(i -> i.getCourse().getDurationHours()).sum();
            List<HrLearningPathItemDto> itemDtos = items.stream()
                    .map(i -> new HrLearningPathItemDto(
                            i.getId(),
                            i.getCourse().getId(),
                            i.getCourse().getTitle(),
                            i.getCourse().getProvider(),
                            i.getCourse().getLevel(),
                            i.getCourse().getDurationHours(),
                            i.getStepOrder()
                    ))
                    .toList();

            return new HrLearningPathDto(
                    p.getId(),
                    p.getTitle(),
                    p.getDescription(),
                    p.getTargetRole(),
                    p.getSkill() != null ? p.getSkill().getId() : null,
                    p.getSkill() != null ? p.getSkill().getName() : null,
                    p.getDifficultyLevel() != null ? p.getDifficultyLevel() : "Beginner",
                    p.isActive(),
                    totalHours,
                    items.size(),
                    itemDtos,
                    p.getCreatedAt()
            );
        }).toList();
    }

    @Transactional
    public HrLearningPathDto createLearningPath(CreateOrUpdateLearningPathRequest req, Long authorId) {
        User author = userRepository.findById(authorId).orElse(null);
        Skill skill = null;
        if (req.skillId() != null) {
            skill = skillRepository.findById(req.skillId()).orElse(null);
        }

        LearningPath path = new LearningPath(
                req.title().trim(),
                req.description(),
                req.targetRole(),
                author,
                skill,
                req.difficultyLevel() != null ? req.difficultyLevel() : "Beginner"
        );
        path.setActive(req.isActive());
        path = learningPathRepository.save(path);

        List<HrLearningPathItemDto> items = new ArrayList<>();
        int totalHours = 0;
        if (req.courseIds() != null && !req.courseIds().isEmpty()) {
            int order = 1;
            for (Long cId : req.courseIds()) {
                Course c = courseRepository.findById(cId).orElse(null);
                if (c != null) {
                    LearningPathItem item = new LearningPathItem(path, c, order++);
                    item = learningPathItemRepository.save(item);
                    totalHours += c.getDurationHours();
                    items.add(new HrLearningPathItemDto(
                            item.getId(),
                            c.getId(),
                            c.getTitle(),
                            c.getProvider(),
                            c.getLevel(),
                            c.getDurationHours(),
                            item.getStepOrder()
                    ));
                }
            }
        }

        return new HrLearningPathDto(
                path.getId(),
                path.getTitle(),
                path.getDescription(),
                path.getTargetRole(),
                skill != null ? skill.getId() : null,
                skill != null ? skill.getName() : null,
                path.getDifficultyLevel(),
                path.isActive(),
                totalHours,
                items.size(),
                items,
                path.getCreatedAt()
        );
    }

    @Transactional
    public HrLearningPathDto updateLearningPath(Long id, CreateOrUpdateLearningPathRequest req) {
        LearningPath path = learningPathRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Learning path not found: " + id));

        Skill skill = null;
        if (req.skillId() != null) {
            skill = skillRepository.findById(req.skillId()).orElse(null);
        }

        path.setTitle(req.title().trim());
        path.setDescription(req.description());
        path.setTargetRole(req.targetRole());
        path.setSkill(skill);
        if (req.difficultyLevel() != null) {
            path.setDifficultyLevel(req.difficultyLevel());
        }
        path.setActive(req.isActive());
        path = learningPathRepository.save(path);

        // Update items
        learningPathItemRepository.deleteByLearningPathId(id);
        List<HrLearningPathItemDto> items = new ArrayList<>();
        int totalHours = 0;
        if (req.courseIds() != null && !req.courseIds().isEmpty()) {
            int order = 1;
            for (Long cId : req.courseIds()) {
                Course c = courseRepository.findById(cId).orElse(null);
                if (c != null) {
                    LearningPathItem item = new LearningPathItem(path, c, order++);
                    item = learningPathItemRepository.save(item);
                    totalHours += c.getDurationHours();
                    items.add(new HrLearningPathItemDto(
                            item.getId(),
                            c.getId(),
                            c.getTitle(),
                            c.getProvider(),
                            c.getLevel(),
                            c.getDurationHours(),
                            item.getStepOrder()
                    ));
                }
            }
        }

        return new HrLearningPathDto(
                path.getId(),
                path.getTitle(),
                path.getDescription(),
                path.getTargetRole(),
                skill != null ? skill.getId() : null,
                skill != null ? skill.getName() : null,
                path.getDifficultyLevel(),
                path.isActive(),
                totalHours,
                items.size(),
                items,
                path.getCreatedAt()
        );
    }

    @Transactional
    public HrLearningPathDto toggleLearningPathActive(Long id) {
        LearningPath path = learningPathRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Learning path not found: " + id));
        path.setActive(!path.isActive());
        path = learningPathRepository.save(path);

        return new HrLearningPathDto(
                path.getId(),
                path.getTitle(),
                path.getDescription(),
                path.getTargetRole(),
                path.getSkill() != null ? path.getSkill().getId() : null,
                path.getSkill() != null ? path.getSkill().getName() : null,
                path.getDifficultyLevel(),
                path.isActive(),
                0, 0, List.of(),
                path.getCreatedAt()
        );
    }

    // =========================================================================
    // 8. Training Programmes
    // =========================================================================
    @Transactional(readOnly = true)
    public List<HrTrainingProgrammeDto> getTrainingProgrammes() {
        return trainingProgrammeRepository.findAllWithDetails().stream()
                .map(p -> new HrTrainingProgrammeDto(
                        p.getId(),
                        p.getTitle(),
                        p.getDescription(),
                        p.getDepartment() != null ? p.getDepartment().getId() : null,
                        p.getDepartment() != null ? p.getDepartment().getName() : "All Departments",
                        p.getSkill() != null ? p.getSkill().getId() : null,
                        p.getSkill() != null ? p.getSkill().getName() : null,
                        p.getCourse() != null ? p.getCourse().getId() : null,
                        p.getCourse() != null ? p.getCourse().getTitle() : null,
                        p.getStartDate() != null ? p.getStartDate().toString() : null,
                        p.getEndDate() != null ? p.getEndDate().toString() : null,
                        p.getCapacity(),
                        p.getEnrolledCount(),
                        p.getTrainerName(),
                        p.getStatus(),
                        p.getCreatedAt()
                ))
                .toList();
    }

    @Transactional
    public HrTrainingProgrammeDto createTrainingProgramme(CreateOrUpdateTrainingProgrammeRequest req) {
        Department dept = null;
        if (req.departmentId() != null) {
            dept = departmentRepository.findById(req.departmentId()).orElse(null);
        }
        Skill skill = null;
        if (req.skillId() != null) {
            skill = skillRepository.findById(req.skillId()).orElse(null);
        }
        Course course = null;
        if (req.courseId() != null) {
            course = courseRepository.findById(req.courseId()).orElse(null);
        }

        TrainingProgramme p = new TrainingProgramme(
                req.title().trim(),
                req.description(),
                dept,
                req.startDate(),
                req.endDate(),
                req.capacity(),
                req.trainerName(),
                req.status() != null ? req.status() : "UPCOMING"
        );
        p.setSkill(skill);
        p.setCourse(course);
        p = trainingProgrammeRepository.save(p);

        return new HrTrainingProgrammeDto(
                p.getId(),
                p.getTitle(),
                p.getDescription(),
                dept != null ? dept.getId() : null,
                dept != null ? dept.getName() : "All Departments",
                skill != null ? skill.getId() : null,
                skill != null ? skill.getName() : null,
                course != null ? course.getId() : null,
                course != null ? course.getTitle() : null,
                p.getStartDate().toString(),
                p.getEndDate().toString(),
                p.getCapacity(),
                p.getEnrolledCount(),
                p.getTrainerName(),
                p.getStatus(),
                p.getCreatedAt()
        );
    }

    @Transactional
    public HrTrainingProgrammeDto updateTrainingProgramme(Long id, CreateOrUpdateTrainingProgrammeRequest req) {
        TrainingProgramme p = trainingProgrammeRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Programme not found: " + id));

        Department dept = null;
        if (req.departmentId() != null) {
            dept = departmentRepository.findById(req.departmentId()).orElse(null);
        }
        Skill skill = null;
        if (req.skillId() != null) {
            skill = skillRepository.findById(req.skillId()).orElse(null);
        }
        Course course = null;
        if (req.courseId() != null) {
            course = courseRepository.findById(req.courseId()).orElse(null);
        }

        p.setTitle(req.title().trim());
        p.setDescription(req.description());
        p.setDepartment(dept);
        p.setSkill(skill);
        p.setCourse(course);
        p.setStartDate(req.startDate());
        p.setEndDate(req.endDate());
        p.setCapacity(req.capacity());
        p.setTrainerName(req.trainerName());
        if (req.status() != null) {
            p.setStatus(req.status());
        }
        p = trainingProgrammeRepository.save(p);

        return new HrTrainingProgrammeDto(
                p.getId(),
                p.getTitle(),
                p.getDescription(),
                dept != null ? dept.getId() : null,
                dept != null ? dept.getName() : "All Departments",
                skill != null ? skill.getId() : null,
                skill != null ? skill.getName() : null,
                course != null ? course.getId() : null,
                course != null ? course.getTitle() : null,
                p.getStartDate().toString(),
                p.getEndDate().toString(),
                p.getCapacity(),
                p.getEnrolledCount(),
                p.getTrainerName(),
                p.getStatus(),
                p.getCreatedAt()
        );
    }

    @Transactional(readOnly = true)
    public List<ProgrammeEnrollmentDetailDto> getProgrammeEnrollments(Long programmeId) {
        TrainingProgramme programme = trainingProgrammeRepository.findById(programmeId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Programme not found: " + programmeId));

        List<TrainingEnrollment> enrollments = trainingEnrollmentRepository.findByProgrammeId(programmeId);
        Map<Long, EmployeeProfile> profileMap = employeeProfileRepository.findAllWithUser().stream()
                .collect(Collectors.toMap(pr -> pr.getUser().getId(), pr -> pr, (a, b) -> a));

        return enrollments.stream().map(te -> {
            User u = te.getUser();
            EmployeeProfile prof = profileMap.get(u.getId());
            String dept = prof != null && prof.getDepartment() != null ? prof.getDepartment() : "General";
            return new ProgrammeEnrollmentDetailDto(
                    te.getId(),
                    programme.getId(),
                    programme.getTitle(),
                    u.getId(),
                    u.getFullName(),
                    u.getEmail(),
                    dept,
                    te.getStatus(),
                    te.getEnrolledAt()
            );
        }).toList();
    }

    @Transactional
    public ProgrammeEnrollmentDetailDto enrollEmployee(Long programmeId, Long userId) {
        TrainingProgramme programme = trainingProgrammeRepository.findById(programmeId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Programme not found: " + programmeId));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "User not found: " + userId));

        if (trainingEnrollmentRepository.findByProgrammeIdAndUserId(programmeId, userId).isPresent()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Employee is already enrolled in this programme");
        }

        if (programme.getEnrolledCount() >= programme.getCapacity()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Programme capacity reached (" + programme.getCapacity() + ")");
        }

        TrainingEnrollment enrollment = new TrainingEnrollment(programme, user);
        enrollment = trainingEnrollmentRepository.save(enrollment);

        programme.setEnrolledCount(programme.getEnrolledCount() + 1);
        trainingProgrammeRepository.save(programme);

        notificationService.createNotification(
                user,
                "TRAINING_ENROLLMENT",
                "Enrolled in Training Programme",
                "You have been enrolled in " + programme.getTitle() + " starting " + programme.getStartDate(),
                "/dashboard/employee"
        );

        return new ProgrammeEnrollmentDetailDto(
                enrollment.getId(),
                programme.getId(),
                programme.getTitle(),
                user.getId(),
                user.getFullName(),
                user.getEmail(),
                "General",
                enrollment.getStatus(),
                enrollment.getEnrolledAt()
        );
    }

    @Transactional
    public ProgrammeEnrollmentDetailDto updateProgrammeEnrollmentStatus(Long enrollmentId, String status) {
        TrainingEnrollment te = trainingEnrollmentRepository.findById(enrollmentId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Enrollment not found: " + enrollmentId));

        te.setStatus(status.toUpperCase());
        te = trainingEnrollmentRepository.save(te);

        return new ProgrammeEnrollmentDetailDto(
                te.getId(),
                te.getProgramme().getId(),
                te.getProgramme().getTitle(),
                te.getUser().getId(),
                te.getUser().getFullName(),
                te.getUser().getEmail(),
                "General",
                te.getStatus(),
                te.getEnrolledAt()
        );
    }

    // =========================================================================
    // 9. Mentor Directory
    // =========================================================================
    @Transactional(readOnly = true)
    public List<HrMentorDirectoryItemDto> getMentorDirectory() {
        List<MentorProfile> profiles = mentorProfileRepository.findAllWithDetails();
        Map<Long, EmployeeProfile> empProfiles = employeeProfileRepository.findAllWithUser().stream()
                .collect(Collectors.toMap(p -> p.getUser().getId(), p -> p, (a, b) -> a));

        List<MentorshipFeedback> feedbacks = mentorshipFeedbackRepository.findAll();

        return profiles.stream().map(mp -> {
            User u = mp.getUser();
            EmployeeProfile ep = empProfiles.get(u.getId());
            String dept = ep != null && ep.getDepartment() != null ? ep.getDepartment() : "General";
            String jobRole = ep != null && ep.getJobRole() != null ? ep.getJobRole() : "Staff";
            String expLevel = ep != null && ep.getExperienceLevel() != null ? ep.getExperienceLevel() : "Senior";

            long activeMentees = mentorAssignmentRepository.countByMentorIdAndStatus(u.getId(), AssignmentStatus.ACTIVE);
            long completedSessions = mentorshipSessionRepository.countByAssignmentMentorIdAndStatus(u.getId(), SessionStatus.COMPLETED);

            double avgRating = feedbacks.stream()
                    .filter(f -> f.getSession() != null && f.getSession().getAssignment() != null &&
                                 f.getSession().getAssignment().getMentor().getId().equals(u.getId()))
                    .mapToInt(MentorshipFeedback::getRating)
                    .average()
                    .orElse(5.0);
            avgRating = Math.round(avgRating * 10.0) / 10.0;

            List<String> tags = mp.getExpertiseAreas() != null ?
                    Arrays.stream(mp.getExpertiseAreas().split(","))
                            .map(String::trim)
                            .filter(s -> !s.isBlank())
                            .toList() : List.of();

            return new HrMentorDirectoryItemDto(
                    mp.getId(),
                    u.getId(),
                    u.getFullName(),
                    u.getEmail(),
                    mp.getHeadline(),
                    dept,
                    jobRole,
                    expLevel,
                    mp.getExpertiseAreas(),
                    tags,
                    mp.getMaxMentees(),
                    (int) activeMentees,
                    completedSessions,
                    avgRating,
                    mp.isActive(),
                    mp.getCreatedAt()
            );
        }).toList();
    }

    @Transactional(readOnly = true)
    public MentorsForSkillDto getMentorsForSkill(Long skillId) {
        Skill skill = skillRepository.findById(skillId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Skill not found: " + skillId));

        List<HrMentorDirectoryItemDto> allMentors = getMentorDirectory();
        String skillNameLower = skill.getName().toLowerCase();

        List<HrMentorDirectoryItemDto> matched = allMentors.stream()
                .filter(m -> m.expertiseAreas() != null && m.expertiseAreas().toLowerCase().contains(skillNameLower))
                .toList();

        return new MentorsForSkillDto(skill.getId(), skill.getName(), matched);
    }

    // =========================================================================
    // 10. Learning Progress
    // =========================================================================
    @Transactional(readOnly = true)
    public HrLearningProgressOverviewDto getLearningProgressOverview() {
        List<EmployeeLearning> learnings = employeeLearningRepository.findAllWithDetails();
        List<TrainingEnrollment> programmeEnrollments = trainingEnrollmentRepository.findAll();
        Map<Long, EmployeeProfile> profileMap = employeeProfileRepository.findAllWithUser().stream()
                .collect(Collectors.toMap(p -> p.getUser().getId(), p -> p, (a, b) -> a));

        long totalCourseEnrollments = learnings.size();
        long completedCourses = learnings.stream()
                .filter(l -> "COMPLETED".equalsIgnoreCase(l.getStatus()) || "CERTIFIED".equalsIgnoreCase(l.getStatus()))
                .count();
        long inProgressCourses = learnings.stream()
                .filter(l -> "IN_PROGRESS".equalsIgnoreCase(l.getStatus()))
                .count();

        double completionRate = totalCourseEnrollments > 0 ?
                Math.round((completedCourses * 100.0 / totalCourseEnrollments) * 10.0) / 10.0 : 0.0;

        long totalProgrammes = programmeEnrollments.size();
        long attendedProgrammes = programmeEnrollments.stream()
                .filter(te -> "ATTENDED".equalsIgnoreCase(te.getStatus()) || "COMPLETED".equalsIgnoreCase(te.getStatus()))
                .count();

        List<HrEmployeeCourseProgressItemDto> items = learnings.stream()
                .map(l -> {
                    User u = l.getUser();
                    EmployeeProfile prof = profileMap.get(u.getId());
                    String dept = prof != null && prof.getDepartment() != null ? prof.getDepartment() : "General";
                    return new HrEmployeeCourseProgressItemDto(
                            l.getId(),
                            u.getId(),
                            u.getFullName(),
                            u.getEmail(),
                            dept,
                            l.getCourse().getId(),
                            l.getCourse().getTitle(),
                            l.getCourse().getLevel(),
                            l.getProgressPct(),
                            l.getStatus(),
                            l.getEnrolledAt(),
                            l.getCompletedAt()
                    );
                })
                .toList();

        return new HrLearningProgressOverviewDto(
                totalCourseEnrollments,
                completedCourses,
                inProgressCourses,
                completionRate,
                totalProgrammes,
                attendedProgrammes,
                items
        );
    }

    // =========================================================================
    // 11. Feedback & Effectiveness
    // =========================================================================
    @Transactional(readOnly = true)
    public HrFeedbackOverviewDto getFeedbackOverview() {
        List<PlatformFeedback> platformList = platformFeedbackRepository.findAllWithUser();
        double avgPlatform = platformList.stream().mapToInt(PlatformFeedback::getRating).average().orElse(4.5);
        avgPlatform = Math.round(avgPlatform * 10.0) / 10.0;

        List<MentorshipFeedback> mentorshipList = mentorshipFeedbackRepository.findAll();
        double avgMentor = mentorshipList.stream().mapToInt(MentorshipFeedback::getRating).average().orElse(4.8);
        avgMentor = Math.round(avgMentor * 10.0) / 10.0;

        Map<String, Double> ratingByCategory = platformList.stream()
                .collect(Collectors.groupingBy(
                        PlatformFeedback::getCategory,
                        Collectors.collectingAndThen(
                                Collectors.averagingInt(PlatformFeedback::getRating),
                                d -> Math.round(d * 10.0) / 10.0
                        )
                ));

        List<HrPlatformFeedbackItemDto> platformDtos = platformList.stream().map(pf -> new HrPlatformFeedbackItemDto(
                pf.getId(),
                pf.getUser() != null ? pf.getUser().getId() : null,
                pf.getUser() != null ? pf.getUser().getFullName() : "Anonymous",
                pf.getUser() != null ? pf.getUser().getEmail() : null,
                pf.getCategory(),
                pf.getRating(),
                pf.getComments(),
                pf.getCreatedAt()
        )).toList();

        List<HrMentorshipFeedbackItemDto> mentorDtos = mentorshipList.stream().map(mf -> new HrMentorshipFeedbackItemDto(
                mf.getId(),
                mf.getSession() != null ? mf.getSession().getId() : null,
                mf.getAuthor() != null ? mf.getAuthor().getFullName() : "Mentee",
                mf.getSession() != null && mf.getSession().getAssignment() != null ?
                        mf.getSession().getAssignment().getMentor().getFullName() : "Mentor",
                mf.getRating(),
                mf.getComments(),
                mf.getCreatedAt()
        )).toList();

        return new HrFeedbackOverviewDto(
                avgPlatform,
                platformList.size(),
                avgMentor,
                mentorshipList.size(),
                platformDtos,
                mentorDtos,
                ratingByCategory
        );
    }

    // =========================================================================
    // 12. Reports & Analytics & Leadership Sharing
    // =========================================================================
    @Transactional(readOnly = true)
    public HrReportResultDto generateReport(String reportType, Long departmentId) {
        String type = reportType != null ? reportType.toUpperCase() : "WORKFORCE_READINESS";
        Map<String, Object> data = new HashMap<>();
        String title;

        switch (type) {
            case "SKILL_GAP_ANALYSIS" -> {
                title = "Organizational Skill Gap Analysis";
                HrSkillGapSummaryDto summary = getSkillGapSummary();
                data.put("totalGaps", summary.totalGaps());
                data.put("criticalShortages", summary.criticalShortages());
                data.put("topGapSkills", summary.topGapSkills());
                data.put("departmentBreakdown", summary.departmentBreakdown());
            }
            case "LEARNING_PROGRESS" -> {
                title = "Workforce Learning & Training Progress";
                HrLearningProgressOverviewDto prog = getLearningProgressOverview();
                data.put("totalEnrollments", prog.totalCourseEnrollments());
                data.put("completedEnrollments", prog.completedCourseEnrollments());
                data.put("completionRate", prog.completionRatePercentage());
                data.put("programmeAttendance", prog.attendedProgrammeEnrollments());
            }
            case "COMPETENCY_AUDIT" -> {
                title = "Competency Framework Audit";
                List<HrCompetencyDto> comp = getCompetencies();
                data.put("totalCompetencies", comp.size());
                data.put("activeCompetencies", comp.stream().filter(HrCompetencyDto::isActive).count());
                data.put("competencies", comp);
            }
            default -> {
                title = "Workforce Readiness & Skill Intelligence Report";
                HrDashboardOverviewDto overview = getOverview();
                data.put("totalEmployees", overview.totalEmployees());
                data.put("assessedEmployees", overview.assessedEmployeesCount());
                data.put("skillCoveragePercentage", overview.skillCoveragePercentage());
                data.put("criticalShortages", overview.criticalShortagesCount());
            }
        }

        return new HrReportResultDto(type, title, Instant.now(), data);
    }

    @Transactional
    public LeadershipReportShareDto shareReportWithLeadership(ShareReportRequest req, Long sharedById) {
        User sharedBy = userRepository.findById(sharedById)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "User not found: " + sharedById));

        Department dept = null;
        if (req.departmentId() != null) {
            dept = departmentRepository.findById(req.departmentId()).orElse(null);
        }

        LeadershipReportShare share = new LeadershipReportShare(
                req.reportType(),
                req.title().trim(),
                sharedBy,
                req.sharedWithRole().toUpperCase(),
                dept,
                req.summaryNotes(),
                req.reportDataJson()
        );
        share = leadershipReportShareRepository.save(share);

        return new LeadershipReportShareDto(
                share.getId(),
                share.getReportType(),
                share.getTitle(),
                sharedBy.getId(),
                sharedBy.getFullName(),
                share.getSharedWithRole(),
                dept != null ? dept.getId() : null,
                dept != null ? dept.getName() : "All Departments",
                share.getSummaryNotes(),
                share.getReportDataJson(),
                share.getCreatedAt()
        );
    }

    @Transactional(readOnly = true)
    public List<LeadershipReportShareDto> getSharedLeadershipReports(String userRole, Long deptId) {
        List<LeadershipReportShare> shares;
        if ("HR_SPECIALIST".equalsIgnoreCase(userRole) || "ADMIN".equalsIgnoreCase(userRole)) {
            shares = leadershipReportShareRepository.findAllWithDetails();
        } else {
            List<String> roles = List.of(userRole.toUpperCase(), "ALL_LEADERSHIP");
            shares = leadershipReportShareRepository.findForLeadership(roles, deptId);
        }

        return shares.stream().map(s -> new LeadershipReportShareDto(
                s.getId(),
                s.getReportType(),
                s.getTitle(),
                s.getSharedBy().getId(),
                s.getSharedBy().getFullName(),
                s.getSharedWithRole(),
                s.getDepartment() != null ? s.getDepartment().getId() : null,
                s.getDepartment() != null ? s.getDepartment().getName() : "All Departments",
                s.getSummaryNotes(),
                s.getReportDataJson(),
                s.getCreatedAt()
        )).toList();
    }

    // =========================================================================
    // 13. Notifications & Announcements
    // =========================================================================
    @Transactional(readOnly = true)
    public List<HrAnnouncementDto> getAnnouncements(Long currentUserId) {
        List<HrAnnouncement> announcements = hrAnnouncementRepository.findAllWithDetails();
        long totalUsers = userRepository.count();

        return announcements.stream().map(a -> {
            long readCount = hrAnnouncementReadRepository.countByAnnouncementId(a.getId());
            boolean readByMe = currentUserId != null &&
                    hrAnnouncementReadRepository.existsByAnnouncementIdAndUserId(a.getId(), currentUserId);

            return new HrAnnouncementDto(
                    a.getId(),
                    a.getAuthor().getId(),
                    a.getAuthor().getFullName(),
                    a.getTargetType(),
                    a.getDepartment() != null ? a.getDepartment().getId() : null,
                    a.getDepartment() != null ? a.getDepartment().getName() : "All Departments",
                    a.getTargetRole(),
                    a.getTitle(),
                    a.getContent(),
                    a.getPriority(),
                    a.getCreatedAt(),
                    readCount,
                    totalUsers,
                    readByMe
            );
        }).toList();
    }

    @Transactional
    public HrAnnouncementDto createAnnouncement(CreateAnnouncementRequest req, Long authorId) {
        User author = userRepository.findById(authorId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Author not found: " + authorId));

        Department dept = null;
        if (req.departmentId() != null) {
            dept = departmentRepository.findById(req.departmentId()).orElse(null);
        }

        HrAnnouncement a = new HrAnnouncement(
                author,
                req.targetType().toUpperCase(),
                dept,
                req.targetRole(),
                req.title().trim(),
                req.content().trim(),
                req.priority() != null ? req.priority().toUpperCase() : "NORMAL"
        );
        a = hrAnnouncementRepository.save(a);

        // Targeted user notifications
        List<User> recipients;
        final Department finalDept = dept;
        final String finalTargetRole = a.getTargetRole();
        if ("DEPARTMENT".equalsIgnoreCase(a.getTargetType()) && finalDept != null) {
            List<EmployeeProfile> deptProfiles = employeeProfileRepository.findAllWithUser().stream()
                    .filter(p -> finalDept.getName().equalsIgnoreCase(p.getDepartment()))
                    .toList();
            recipients = deptProfiles.stream().map(EmployeeProfile::getUser).toList();
        } else if ("ROLE".equalsIgnoreCase(a.getTargetType()) && finalTargetRole != null) {
            recipients = userRepository.findAll().stream()
                    .filter(u -> u.getRole().name().equalsIgnoreCase(finalTargetRole))
                    .toList();
        } else {
            recipients = userRepository.findAll();
        }

        for (User recipient : recipients) {
            if (recipient.getId() != null && !recipient.getId().equals(authorId)) {
                notificationService.createNotification(
                        recipient,
                        "HR_ANNOUNCEMENT",
                        "[" + a.getPriority() + "] " + a.getTitle(),
                        a.getContent().length() > 100 ? a.getContent().substring(0, 97) + "..." : a.getContent(),
                        "/dashboard/hr"
                );
            }
        }

        return new HrAnnouncementDto(
                a.getId(),
                author.getId(),
                author.getFullName(),
                a.getTargetType(),
                dept != null ? dept.getId() : null,
                dept != null ? dept.getName() : "All Departments",
                a.getTargetRole(),
                a.getTitle(),
                a.getContent(),
                a.getPriority(),
                a.getCreatedAt(),
                0L,
                recipients.size(),
                false
        );
    }

    @Transactional
    public void markAnnouncementRead(Long announcementId, Long userId) {
        HrAnnouncement a = hrAnnouncementRepository.findById(announcementId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Announcement not found: " + announcementId));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "User not found: " + userId));

        if (!hrAnnouncementReadRepository.existsByAnnouncementIdAndUserId(announcementId, userId)) {
            HrAnnouncementRead read = new HrAnnouncementRead(a, user);
            hrAnnouncementReadRepository.save(read);
        }
    }
}
