package com.skillsphere.dto;

import java.util.List;

public record ManagerEmployeeDetailDto(
        Long userId,
        String fullName,
        String email,
        String employeeId,
        String jobRole,
        String departmentName,
        Long teamId,
        String teamName,
        String roleInTeam,
        String experience,
        String bio,
        List<EmployeeSkillDto> skills,
        List<SkillGapDto> gaps,
        List<AssessmentAttemptDto> assessments,
        List<EmployeeLearningDto> learnings,
        String mentorName,
        Long mentorId,
        List<ReassessmentRequestDto> reassessmentRequests,
        List<ManagerCourseRecommendationDto> courseRecommendations
) {}
