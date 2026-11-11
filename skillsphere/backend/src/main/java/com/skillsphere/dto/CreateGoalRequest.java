package com.skillsphere.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record CreateGoalRequest(
        @NotNull(message = "Assignment ID is required.")
        Long assignmentId,

        @NotBlank(message = "Title is required.")
        @Size(max = 140, message = "Title is too long.")
        String title,

        @Size(max = 2000, message = "Description is too long.")
        String description,

        LocalDate targetDate
) {}
