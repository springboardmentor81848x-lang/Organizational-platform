package com.skillsphere.dto;

import java.time.Instant;

public record TeamMemberDto(
        Long userId,
        String fullName,
        String email,
        String employeeId,
        String role,
        String jobRole,
        String department,
        Instant joinedAt
) {}
