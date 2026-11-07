package com.skillsphere.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateReassessmentRequest(
        @NotNull(message = "Employee ID is required")
        Long employeeId,

        @NotNull(message = "Skill ID is required")
        Long skillId,

        @NotBlank(message = "Reason is required")
        @Size(max = 1000, message = "Reason cannot exceed 1000 characters")
        String reason
) {}
