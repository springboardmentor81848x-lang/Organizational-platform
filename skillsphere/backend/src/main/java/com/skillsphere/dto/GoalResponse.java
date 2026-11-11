package com.skillsphere.dto;

import java.time.Instant;
import java.time.LocalDate;

public record GoalResponse(
        Long id,
        Long assignmentId,
        String title,
        String description,
        LocalDate targetDate,
        String status,
        Instant createdAt,
        Instant updatedAt
) {}
