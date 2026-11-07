package com.skillsphere.dto;

public record ManagerTeamMemberDto(
        Long userId,
        String fullName,
        String email,
        String employeeId,
        String jobRole,
        String departmentName,
        Long teamId,
        String teamName,
        String roleInTeam,
        int skillsAssessedCount,
        int totalGapsCount,
        int criticalGapsCount,
        int learningProgressPercent,
        int completedCoursesCount,
        String mentorName,
        boolean active
) {}
