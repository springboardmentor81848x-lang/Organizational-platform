package com.skillsphere.dto;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public record AssignAssessmentRequest(
        @NotNull(message = "Assessment ID is required.")
        Long assessmentId,

        @NotNull(message = "User ID is required.")
        Long userId,

        LocalDate dueDate
) {}
