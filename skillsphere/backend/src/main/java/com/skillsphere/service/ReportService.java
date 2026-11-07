package com.skillsphere.service;

import com.skillsphere.dto.*;
import com.skillsphere.entity.*;
import com.skillsphere.exception.ApiException;
import com.skillsphere.repository.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class ReportService {

    private final UserRepository userRepository;
    private final EmployeeProfileRepository profileRepository;
    private final DepartmentRepository departmentRepository;
    private final TeamRepository teamRepository;
    private final TeamMembershipRepository membershipRepository;
    private final SkillRepository skillRepository;
    private final EmployeeSkillRepository employeeSkillRepository;
    private final SkillGapRepository skillGapRepository;
    private final EmployeeLearningRepository learningRepository;
    private final CertificateRepository certificateRepository;
    private final MentorAssignmentRepository mentorAssignmentRepository;
    private final AssessmentSubmissionRepository submissionRepository;
    private final SupportTicketRepository supportTicketRepository;
    private final AuditLogRepository auditLogRepository;
    private final ConversationRepository conversationRepository;
    private final MessageRepository messageRepository;
    private final TrainingEnrollmentRepository trainingEnrollmentRepository;

    public ReportService(UserRepository userRepository,
                         EmployeeProfileRepository profileRepository,
                         DepartmentRepository departmentRepository,
                         TeamRepository teamRepository,
                         TeamMembershipRepository membershipRepository,
                         SkillRepository skillRepository,
                         EmployeeSkillRepository employeeSkillRepository,
                         SkillGapRepository skillGapRepository,
                         EmployeeLearningRepository learningRepository,
                         CertificateRepository certificateRepository,
                         MentorAssignmentRepository mentorAssignmentRepository,
                         AssessmentSubmissionRepository submissionRepository,
                         SupportTicketRepository supportTicketRepository,
                         AuditLogRepository auditLogRepository,
                         ConversationRepository conversationRepository,
                         MessageRepository messageRepository,
                         TrainingEnrollmentRepository trainingEnrollmentRepository) {
        this.userRepository = userRepository;
        this.profileRepository = profileRepository;
        this.departmentRepository = departmentRepository;
        this.teamRepository = teamRepository;
        this.membershipRepository = membershipRepository;
        this.skillRepository = skillRepository;
        this.employeeSkillRepository = employeeSkillRepository;
        this.skillGapRepository = skillGapRepository;
        this.learningRepository = learningRepository;
        this.certificateRepository = certificateRepository;
        this.mentorAssignmentRepository = mentorAssignmentRepository;
        this.submissionRepository = submissionRepository;
        this.supportTicketRepository = supportTicketRepository;
        this.auditLogRepository = auditLogRepository;
        this.conversationRepository = conversationRepository;
        this.messageRepository = messageRepository;
        this.trainingEnrollmentRepository = trainingEnrollmentRepository;
    }

    @Transactional(readOnly = true)
    public ReportDtos.EmployeeReportDto getEmployeeReport(String email) {
        User user = getUserByEmail(email);
        EmployeeProfile profile = profileRepository.findByUserId(user.getId()).orElse(null);

        List<EmployeeSkill> skills = employeeSkillRepository.findByUserId(user.getId());
        int totalSkills = skills.size();
        int avgSkill = totalSkills > 0 ? (int) Math.round(skills.stream()
                .mapToInt(s -> SkillService.PROFICIENCY_TO_NUMERIC.getOrDefault(s.getProficiencyLevel(), 1))
                .average().orElse(1)) : 1;

        List<SkillGap> gaps = skillGapRepository.findByUserId(user.getId());
        List<SkillGapDto> gapDtos = gaps.stream().map(this::toSkillGapDto).toList();

        List<EmployeeLearning> learnings = learningRepository.findByUserId(user.getId());
        int completedCourses = (int) learnings.stream().filter(l -> "COMPLETED".equalsIgnoreCase(l.getStatus())).count();
        List<EmployeeLearningDto> learningDtos = learnings.stream().map(this::toEmployeeLearningDto).toList();

        List<Certificate> certs = certificateRepository.findByUserId(user.getId());
        List<CertificateDto> certDtos = certs.stream().map(c -> new CertificateDto(
                c.getId(),
                c.getCourse() != null ? c.getCourse().getId() : null,
                c.getCourse() != null ? c.getCourse().getTitle() : "Skill Certificate",
                c.getCertificateCode(),
                c.getTitle(),
                c.getIssueDate(),
                c.getExpiryDate(),
                c.getCredentialUrl()
        )).toList();

        int activeMentorships = (int) mentorAssignmentRepository.countByMentorIdAndStatus(user.getId(), AssignmentStatus.ACTIVE);
        if (mentorAssignmentRepository.findByEmployeeIdAndStatus(user.getId(), AssignmentStatus.ACTIVE).isPresent()) {
            activeMentorships++;
        }

        return new ReportDtos.EmployeeReportDto(
                user.getFullName(),
                user.getEmployeeId(),
                user.getEmail(),
                profile != null ? profile.getJobRole() : (user.getRequestedRole() != null ? user.getRequestedRole().name() : "Employee"),
                profile != null ? profile.getDepartment() : "General",
                totalSkills,
                avgSkill,
                (int) gaps.stream().filter(g -> g.getGapSize() > 0).count(),
                completedCourses,
                activeMentorships,
                gapDtos,
                learningDtos,
                certDtos
        );
    }

    @Transactional(readOnly = true)
    public ReportDtos.TeamReportDto getTeamReport(Long teamId, String callerEmail) {
        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Team not found."));

        User caller = getUserByEmail(callerEmail);
        boolean isAuthorized = caller.getRole() == Role.ADMIN
                || (caller.getRole() == Role.MANAGER && team.getManager() != null && team.getManager().getId().equals(caller.getId()))
                || (caller.getRole() == Role.DEPARTMENT_HEAD && team.getDepartment() != null && team.getDepartment().getHead() != null && team.getDepartment().getHead().getId().equals(caller.getId()));

        if (!isAuthorized) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Access denied. You can only access reports for teams assigned to you.");
        }

        return buildTeamReport(team);
    }

    @Transactional(readOnly = true)
    public ReportDtos.TeamReportDto getTeamReport(Long teamId) {
        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Team not found."));
        return buildTeamReport(team);
    }

    private ReportDtos.TeamReportDto buildTeamReport(Team team) {
        Long teamId = team.getId();
        List<TeamMembership> memberships = membershipRepository.findByTeamId(teamId);
        List<Long> memberIds = memberships.stream().map(m -> m.getUser().getId()).toList();

        List<ReportDtos.TeamReportDto.TeamMemberProgressDto> summaries = new ArrayList<>();
        int totalEnrolled = 0;
        int totalCompleted = 0;

        for (TeamMembership m : memberships) {
            User u = m.getUser();
            int sCount = employeeSkillRepository.findByUserId(u.getId()).size();
            int gCount = (int) skillGapRepository.findByUserId(u.getId()).stream().filter(g -> g.getGapSize() > 0).count();
            List<EmployeeLearning> userLearnings = learningRepository.findByUserId(u.getId());
            int cCount = (int) userLearnings.stream().filter(l -> "COMPLETED".equalsIgnoreCase(l.getStatus())).count();
            totalEnrolled += userLearnings.size();
            totalCompleted += cCount;

            summaries.add(new ReportDtos.TeamReportDto.TeamMemberProgressDto(
                    u.getId(),
                    u.getFullName(),
                    m.getRoleInTeam(),
                    sCount,
                    gCount,
                    cCount
            ));
        }

        List<SkillGap> teamGaps = memberIds.isEmpty() ? Collections.emptyList() : skillGapRepository.findByUserIds(memberIds);
        List<SkillGapDto> topGaps = teamGaps.stream()
                .filter(g -> "HIGH".equalsIgnoreCase(g.getPriority()))
                .map(this::toSkillGapDto)
                .limit(10)
                .toList();

        Map<String, Integer> coverage = new HashMap<>();
        for (SkillGap g : teamGaps) {
            coverage.put(g.getSkill().getName(), coverage.getOrDefault(g.getSkill().getName(), 0) + 1);
        }

        return new ReportDtos.TeamReportDto(
                team.getId(),
                team.getName(),
                team.getManager() != null ? team.getManager().getFullName() : "Unassigned",
                memberships.size(),
                coverage,
                topGaps,
                totalEnrolled,
                totalCompleted,
                summaries
        );
    }

    @Transactional(readOnly = true)
    public ReportDtos.DepartmentReportDto getDepartmentReport(Long deptId, String callerEmail) {
        Department dept = departmentRepository.findById(deptId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Department not found."));

        User caller = getUserByEmail(callerEmail);
        boolean isAuthorized = caller.getRole() == Role.ADMIN
                || caller.getRole() == Role.HR_SPECIALIST
                || (caller.getRole() == Role.DEPARTMENT_HEAD && dept.getHead() != null && dept.getHead().getId().equals(caller.getId()));

        if (!isAuthorized) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Access denied. You can only access reports for departments you head.");
        }

        return buildDepartmentReport(dept);
    }

    @Transactional(readOnly = true)
    public ReportDtos.DepartmentReportDto getDepartmentReport(Long deptId) {
        Department dept = departmentRepository.findById(deptId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Department not found."));
        return buildDepartmentReport(dept);
    }

    private ReportDtos.DepartmentReportDto buildDepartmentReport(Department dept) {
        Long deptId = dept.getId();
        List<Team> teams = teamRepository.findByDepartmentId(deptId);
        List<TeamMembership> memberships = membershipRepository.findByDepartmentId(deptId);
        List<Long> userIds = memberships.stream().map(m -> m.getUser().getId()).distinct().toList();

        List<SkillGap> deptGaps = userIds.isEmpty() ? Collections.emptyList() : skillGapRepository.findByUserIds(userIds);

        Map<String, Integer> highGapCounts = new HashMap<>();
        for (SkillGap g : deptGaps) {
            if ("HIGH".equalsIgnoreCase(g.getPriority())) {
                highGapCounts.put(g.getSkill().getName(), highGapCounts.getOrDefault(g.getSkill().getName(), 0) + 1);
            }
        }

        List<ReportDtos.DepartmentReportDto.DepartmentRiskDto> risks = highGapCounts.entrySet().stream()
                .map(e -> new ReportDtos.DepartmentReportDto.DepartmentRiskDto(
                        e.getKey(),
                        e.getValue(),
                        e.getValue() > 3 ? "HIGH" : "MEDIUM"
                )).toList();

        Map<String, Double> skillAverages = new HashMap<>();
        Map<String, List<Integer>> skillValues = new HashMap<>();
        for (SkillGap g : deptGaps) {
            skillValues.computeIfAbsent(g.getSkill().getName(), k -> new ArrayList<>()).add(g.getCurrentLevel());
        }
        for (Map.Entry<String, List<Integer>> entry : skillValues.entrySet()) {
            double avg = entry.getValue().stream().mapToInt(Integer::intValue).average().orElse(0.0);
            skillAverages.put(entry.getKey(), Math.round(avg * 10.0) / 10.0);
        }

        return new ReportDtos.DepartmentReportDto(
                dept.getId(),
                dept.getName(),
                dept.getHead() != null ? dept.getHead().getFullName() : "Unassigned",
                userIds.size(),
                teams.size(),
                skillAverages,
                risks,
                12
        );
    }

    @Transactional(readOnly = true)
    public ReportDtos.OrganizationReportDto getOrganizationReport() {
        List<User> users = userRepository.findAll();
        Map<String, Long> byRole = users.stream().collect(Collectors.groupingBy(u -> u.getRole().name(), Collectors.counting()));

        int totalDepts = (int) departmentRepository.count();
        int totalSkills = (int) skillRepository.count();
        int totalAssessments = (int) submissionRepository.count();
        int totalGaps = (int) skillGapRepository.findAll().stream().filter(g -> g.getGapSize() > 0).count();
        int completedCourses = (int) learningRepository.findAll().stream().filter(l -> "COMPLETED".equalsIgnoreCase(l.getStatus())).count();
        int activeMentorships = (int) mentorAssignmentRepository.findAll().stream().filter(a -> a.getStatus() == AssignmentStatus.ACTIVE).count();

        List<Department> departments = departmentRepository.findAll();
        List<ReportDtos.OrganizationReportDto.DepartmentSummaryDto> deptSummaries = new ArrayList<>();
        for (Department d : departments) {
            List<TeamMembership> m = membershipRepository.findByDepartmentId(d.getId());
            List<Long> uids = m.stream().map(mem -> mem.getUser().getId()).distinct().toList();
            int openGaps = uids.isEmpty() ? 0 : (int) skillGapRepository.findByUserIds(uids).stream().filter(g -> g.getGapSize() > 0).count();

            deptSummaries.add(new ReportDtos.OrganizationReportDto.DepartmentSummaryDto(
                    d.getName(),
                    uids.size(),
                    openGaps,
                    8
            ));
        }

        return new ReportDtos.OrganizationReportDto(
                users.size(),
                byRole,
                totalDepts,
                totalSkills,
                totalAssessments,
                totalGaps,
                completedCourses,
                activeMentorships,
                deptSummaries
        );
    }

    @Transactional(readOnly = true)
    public ReportDtos.AdminReportDto getAdminReport() {
        List<User> users = userRepository.findAll();
        long active = users.stream().filter(u -> u.getAccountStatus() == AccountStatus.ACTIVE).count();
        long suspended = users.stream().filter(u -> u.getAccountStatus() == AccountStatus.SUSPENDED).count();
        long pending = users.stream().filter(u -> u.getAccountStatus() == AccountStatus.PENDING).count();

        Map<String, Long> roleDist = users.stream().collect(Collectors.groupingBy(u -> u.getRole().name(), Collectors.counting()));

        long totalAudit = auditLogRepository.count();
        long openTickets = supportTicketRepository.findAll().stream().filter(t -> !"CLOSED".equalsIgnoreCase(t.getStatus())).count();
        long totalConvs = conversationRepository.count();
        long totalMsgs = messageRepository.count();

        return new ReportDtos.AdminReportDto(
                users.size(),
                active,
                suspended,
                pending,
                roleDist,
                totalAudit,
                openTickets,
                totalConvs,
                totalMsgs
        );
    }

    @Transactional(readOnly = true)
    public String generateSkillGapsCsv() {
        List<SkillGap> gaps = skillGapRepository.findAll();
        StringBuilder csv = new StringBuilder();
        csv.append("User ID,User Name,Email,Skill Name,Category,Current Level,Required Level,Gap Size,Priority,Calculated At\n");
        for (SkillGap g : gaps) {
            csv.append(g.getUser().getId()).append(",")
                    .append("\"").append(g.getUser().getFullName()).append("\",")
                    .append(g.getUser().getEmail()).append(",")
                    .append("\"").append(g.getSkill().getName()).append("\",")
                    .append("\"").append(g.getSkill().getCategory().getName()).append("\",")
                    .append(g.getCurrentLevel()).append(",")
                    .append(g.getRequiredLevel()).append(",")
                    .append(g.getGapSize()).append(",")
                    .append(g.getPriority()).append(",")
                    .append(g.getCalculatedAt()).append("\n");
        }
        return csv.toString();
    }

    private SkillGapDto toSkillGapDto(SkillGap g) {
        return new SkillGapDto(
                g.getSkill().getId(),
                g.getSkill().getName(),
                g.getSkill().getCategory().getName(),
                g.getCurrentLevel(),
                SkillService.NUMERIC_TO_PROFICIENCY.getOrDefault(g.getCurrentLevel(), "Unaware"),
                g.getRequiredLevel(),
                SkillService.NUMERIC_TO_PROFICIENCY.getOrDefault(g.getRequiredLevel(), "Intermediate"),
                g.getGapSize(),
                g.getPriority(),
                g.getCalculatedAt()
        );
    }

    private EmployeeLearningDto toEmployeeLearningDto(EmployeeLearning el) {
        Course c = el.getCourse();
        return new EmployeeLearningDto(
                el.getId(),
                c.getId(),
                c.getTitle(),
                c.getSkill() != null ? c.getSkill().getName() : null,
                c.getLevel(),
                c.getDurationHours(),
                c.getProvider(),
                c.getCourseUrl(),
                el.getStatus(),
                el.getProgressPct(),
                el.getEnrolledAt(),
                el.getCompletedAt(),
                el.getExpiresAt()
        );
    }

    private User getUserByEmail(String email) {
        return userRepository.findByEmail(email.trim().toLowerCase())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "User not found."));
    }
}
