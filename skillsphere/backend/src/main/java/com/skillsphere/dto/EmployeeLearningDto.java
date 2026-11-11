package com.skillsphere.dto;

import java.time.Instant;

public record EmployeeLearningDto(
        Long id,
        Long courseId,
        String courseTitle,
        String skillName,
        String level,
        int durationHours,
        String provider,
        String courseUrl,
        String status, // NOT_STARTED, IN_PROGRESS, COMPLETED, CERTIFIED, EXPIRED
        int progressPct,
        Instant enrolledAt,
        Instant completedAt,
        Instant expiresAt
) {}
