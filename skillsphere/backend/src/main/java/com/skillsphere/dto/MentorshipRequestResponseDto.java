package com.skillsphere.dto;

import java.time.Instant;

public record MentorshipRequestResponseDto(
        Long id,
        Long mentorId,
        String mentorName,
        String mentorEmail,
        String mentorHeadline,
        Long employeeId,
        String employeeName,
        String employeeEmail,
        String employeeJobRole,
        String message,
        String status, // PENDING, ACCEPTED, DECLINED, CANCELLED
        Instant createdAt,
        Instant respondedAt,
        String responseNote
) {}
