package com.skillsphere.service;

import com.skillsphere.dto.EmployeeDashboardOverviewDto;
import com.skillsphere.dto.EmployeeDashboardOverviewDto.RecentActivityItem;
import com.skillsphere.entity.*;
import com.skillsphere.exception.ApiException;
import com.skillsphere.repository.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
public class EmployeeDashboardService {

    private final UserRepository userRepository;
    private final EmployeeProfileRepository profileRepository;
    private final EmployeeSkillRepository employeeSkillRepository;
    private final SkillGapRepository skillGapRepository;
    private final AssessmentAttemptRepository attemptRepository;
    private final EmployeeLearningRepository learningRepository;
    private final CertificateRepository certificateRepository;
    private final MentorAssignmentRepository mentorAssignmentRepository;
    private final MentorshipGoalRepository goalRepository;
    private final NotificationRepository notificationRepository;

    public EmployeeDashboardService(UserRepository userRepository,
                                  EmployeeProfileRepository profileRepository,
                                  EmployeeSkillRepository employeeSkillRepository,
                                  SkillGapRepository skillGapRepository,
                                  AssessmentAttemptRepository attemptRepository,
                                  EmployeeLearningRepository learningRepository,
                                  CertificateRepository certificateRepository,
                                  MentorAssignmentRepository mentorAssignmentRepository,
                                  MentorshipGoalRepository goalRepository,
                                  NotificationRepository notificationRepository) {
        this.userRepository = userRepository;
        this.profileRepository = profileRepository;
        this.employeeSkillRepository = employeeSkillRepository;
        this.skillGapRepository = skillGapRepository;
        this.attemptRepository = attemptRepository;
        this.learningRepository = learningRepository;
        this.certificateRepository = certificateRepository;
        this.mentorAssignmentRepository = mentorAssignmentRepository;
        this.goalRepository = goalRepository;
        this.notificationRepository = notificationRepository;
    }

    @Transactional(readOnly = true)
    public EmployeeDashboardOverviewDto getOverview(String email) {
        User user = userRepository.findByEmail(email.trim().toLowerCase())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "User not found."));

        EmployeeProfile profile = profileRepository.findByUserId(user.getId()).orElse(null);
        String jobRole = profile != null && profile.getJobRole() != null ? profile.getJobRole() : "Software Engineer";
        String department = profile != null && profile.getDepartment() != null ? profile.getDepartment() : "Engineering";
        String experienceLevel = profile != null && profile.getExperienceLevel() != null ? profile.getExperienceLevel() : "Mid";

        List<EmployeeSkill> employeeSkills = employeeSkillRepository.findByUserId(user.getId());
        int skillsCount = employeeSkills.size();
        int verifiedSkillsCount = (int) employeeSkills.stream().filter(EmployeeSkill::isVerified).count();

        double avgProf = employeeSkills.stream()
                .mapToInt(es -> SkillService.PROFICIENCY_TO_NUMERIC.getOrDefault(es.getProficiencyLevel(), 1))
                .average()
                .orElse(0.0);
        double averageProficiency = BigDecimal.valueOf(avgProf).setScale(1, RoundingMode.HALF_UP).doubleValue();

        List<SkillGap> gaps = skillGapRepository.findByUserId(user.getId());
        int activeGapsCount = (int) gaps.stream().filter(g -> g.getGapSize() > 0).count();

        List<AssessmentAttempt> attempts = attemptRepository.findByUserIdOrderByStartedAtDesc(user.getId());
        int attemptsCount = attempts.size();
        Integer latestScore = null;
        String latestSkill = null;
        if (!attempts.isEmpty()) {
            AssessmentAttempt latest = attempts.get(0);
            latestScore = latest.getScore();
            latestSkill = latest.getSkill() != null ? latest.getSkill().getName() : "General";
        }

        List<EmployeeLearning> learnings = learningRepository.findByUserId(user.getId());
        int activeLearnings = (int) learnings.stream().filter(l -> l.getProgressPct() < 100).count();
        int completedCourses = (int) learnings.stream().filter(l -> l.getProgressPct() >= 100).count();

        List<Certificate> certs = certificateRepository.findByUserId(user.getId());
        int certsCount = certs.size();

        Optional<MentorAssignment> activeAssignment = mentorAssignmentRepository.findByEmployeeIdAndStatus(user.getId(), AssignmentStatus.ACTIVE);
        boolean hasMentor = activeAssignment.isPresent();
        String mentorName = hasMentor ? activeAssignment.get().getMentor().getFullName() : null;
        String mentorDept = null;
        int activeGoals = 0;
        if (hasMentor) {
            EmployeeProfile mentorProf = profileRepository.findByUserId(activeAssignment.get().getMentor().getId()).orElse(null);
            if (mentorProf != null) mentorDept = mentorProf.getDepartment();
            activeGoals = (int) goalRepository.findByAssignmentIdOrderByCreatedAtDesc(activeAssignment.get().getId()).stream()
                    .filter(g -> g.getStatus() == GoalStatus.IN_PROGRESS)
                    .count();
        }

        long unreadNotifications = notificationRepository.countByRecipientIdAndIsReadFalse(user.getId());

        // Gather real recent activity items
        List<RecentActivityItem> recentActivities = new ArrayList<>();
        DateTimeFormatter dtf = DateTimeFormatter.ISO_INSTANT;

        for (AssessmentAttempt a : attempts) {
            if (recentActivities.size() >= 5) break;
            recentActivities.add(new RecentActivityItem(
                    "ASSESSMENT",
                    "Completed " + (a.getSkill() != null ? a.getSkill().getName() : "Skill") + " Assessment",
                    "Score: " + (a.getScore() != null ? a.getScore() : 0) + "% (" + (Boolean.TRUE.equals(a.getPassed()) ? "Passed" : "Retake recommended") + ")",
                    a.getCompletedAt() != null ? dtf.format(a.getCompletedAt()) : dtf.format(a.getStartedAt())
            ));
        }

        for (Certificate c : certs) {
            if (recentActivities.size() >= 8) break;
            recentActivities.add(new RecentActivityItem(
                    "CERTIFICATE",
                    "Earned Certificate: " + c.getTitle(),
                    "Code: " + c.getCertificateCode(),
                    dtf.format(c.getIssueDate())
            ));
        }

        for (EmployeeLearning l : learnings) {
            if (recentActivities.size() >= 10) break;
            recentActivities.add(new RecentActivityItem(
                    "COURSE",
                    (l.getProgressPct() >= 100 ? "Completed: " : "Enrolled in: ") + (l.getCourse() != null ? l.getCourse().getTitle() : "Course"),
                    "Progress: " + l.getProgressPct() + "%",
                    dtf.format(l.getEnrolledAt())
            ));
        }

        return new EmployeeDashboardOverviewDto(
                user.getId(),
                user.getFullName(),
                user.getEmail(),
                user.getEmployeeId(),
                jobRole,
                department,
                experienceLevel,
                skillsCount,
                verifiedSkillsCount,
                averageProficiency,
                activeGapsCount,
                attemptsCount,
                latestScore,
                latestSkill,
                activeLearnings,
                completedCourses,
                certsCount,
                hasMentor,
                mentorName,
                mentorDept,
                activeGoals,
                unreadNotifications,
                recentActivities
        );
    }
}
