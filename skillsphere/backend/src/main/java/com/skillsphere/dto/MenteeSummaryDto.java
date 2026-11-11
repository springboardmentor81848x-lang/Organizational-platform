package com.skillsphere.dto;

import java.time.Instant;

public record MenteeSummaryDto(
        Long assignmentId,
        Long employeeUserId,
        String fullName,
        String email,
        String employeeId,
        String jobRole,
        String department,
        String experienceLevel,
        String bio,
        String notes,
        Instant assignedAt,
        long goalsCount,
        long sessionsCount
) {}
