package com.skillsphere.dto;

import jakarta.validation.constraints.*;
import java.time.Instant;

public record CreateSessionRequest(
        @NotNull(message = "Assignment ID is required.")
        Long assignmentId,

        @NotBlank(message = "Title is required.")
        @Size(max = 140, message = "Title is too long.")
        String title,

        @Size(max = 2000, message = "Description is too long.")
        String description,

        @NotNull(message = "Scheduled date and time is required.")
        Instant scheduledAt,

        @Min(value = 15, message = "Duration must be at least 15 minutes.")
        @Max(value = 240, message = "Duration cannot exceed 240 minutes.")
        int durationMinutes,

        @Pattern(regexp = "^$|^https?://.*$", message = "Meeting link must be a valid HTTP or HTTPS URL.")
        @Size(max = 500)
        String meetingLink
) {}
