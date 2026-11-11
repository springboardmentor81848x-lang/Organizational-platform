package com.skillsphere.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateAssignmentRequest(
        @NotNull(message = "Mentor ID is required.")
        Long mentorUserId,

        @NotNull(message = "Employee ID is required.")
        Long employeeUserId,

        @Size(max = 1000)
        String notes
) {}
