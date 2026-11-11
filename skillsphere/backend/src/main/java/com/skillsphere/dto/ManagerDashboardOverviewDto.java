package com.skillsphere.dto;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public record ManagerDashboardOverviewDto(
        int totalTeams,
        int totalEmployees,
        int totalAssessedSkills,
        double skillCoveragePercentage,
        int totalGaps,
        int criticalGaps,
        int highGaps,
        int mediumGaps,
        int lowGaps,
        int pendingReassessments,
        int activeCourseEnrollments,
        int completedCourses,
        double courseCompletionRate,
        int activeMentorships,
        Map<String, Integer> proficiencyDistribution,
        List<SkillCoverageSummary> skillCoverageSummaries,
        List<TeamActivityItem> recentActivities,
        List<NotificationDto> recentAlerts
) {
    public record SkillCoverageSummary(
            Long skillId,
            String skillName,
            String categoryName,
            int employeesWithSkill,
            int totalEmployees,
            double coveragePercentage,
            int averageProficiency
    ) {}

    public record TeamActivityItem(
            String id,
            String activityType, // ASSESSMENT, ENROLLMENT, COMPLETION, REASSESSMENT, MENTORSHIP
            String title,
            String description,
            String employeeName,
            Instant timestamp
    ) {}
}
