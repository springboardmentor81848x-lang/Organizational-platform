package com.skillsphere.dto;

import java.time.Instant;
import java.time.LocalDate;

public record AssessmentAssignmentDto(
        Long id,
        Long assessmentId,
        String assessmentTitle,
        int passScore,
        int timeLimitMinutes,
        Long userId,
        String userName,
        String assignedByName,
        LocalDate dueDate,
        String status,
        Instant createdAt
) {}
