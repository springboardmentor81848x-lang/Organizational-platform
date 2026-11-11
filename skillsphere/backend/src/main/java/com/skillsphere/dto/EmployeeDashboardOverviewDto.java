package com.skillsphere.dto;

import java.util.List;

public record EmployeeDashboardOverviewDto(
        Long userId,
        String fullName,
        String email,
        String employeeId,
        String jobRole,
        String department,
        String experienceLevel,
        String teamName,
        String reportingManagerName,
        int skillsCount,
        int verifiedSkillsCount,
        double averageProficiency,
        int activeGapsCount,
        int assessmentAttemptsCount,
        Integer latestAssessmentScore,
        String latestAssessmentSkill,
        int activeLearningsCount,
        int completedCoursesCount,
        int certificatesCount,
        boolean hasMentor,
        String mentorName,
        String mentorDepartment,
        int activeMentorshipGoals,
        long unreadNotificationsCount,
        List<RecentActivityItem> recentActivities
) {
    public record RecentActivityItem(
            String type,
            String title,
            String description,
            String timestamp
    ) {}
}
