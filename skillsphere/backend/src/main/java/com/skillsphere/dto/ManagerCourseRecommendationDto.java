package com.skillsphere.dto;

import java.time.Instant;

public record ManagerCourseRecommendationDto(
        Long id,
        Long managerId,
        String managerName,
        Long employeeId,
        String employeeName,
        Long courseId,
        String courseTitle,
        String courseLevel,
        String courseProvider,
        String courseUrl,
        Long skillId,
        String skillName,
        String reason,
        String status, // RECOMMENDED, ENROLLED, DISMISSED
        Instant createdAt,
        Instant updatedAt
) {}
