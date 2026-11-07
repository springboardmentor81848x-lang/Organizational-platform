package com.skillsphere.dto;

import java.time.Instant;

public record ReassessmentRequestDto(
        Long id,
        Long managerId,
        String managerName,
        Long employeeId,
        String employeeName,
        String employeeEmail,
        Long skillId,
        String skillName,
        String categoryName,
        String reason,
        String status, // PENDING, COMPLETED, CANCELLED
        Instant createdAt,
        Instant updatedAt
) {}
