package com.skillsphere.dto;

import java.time.Instant;

public record AdminAssignmentDto(
        Long id,
        Long mentorUserId,
        String mentorName,
        String mentorEmail,
        Long employeeUserId,
        String employeeName,
        String employeeEmail,
        String assignedByName,
        String status,
        String notes,
        Instant assignedAt,
        Instant endedAt
) {}
