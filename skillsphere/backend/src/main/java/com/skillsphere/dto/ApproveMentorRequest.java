package com.skillsphere.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ApproveMentorRequest(
        @NotBlank(message = "Headline is required.")
        @Size(max = 140, message = "Headline is too long.")
        String headline,

        @NotBlank(message = "Expertise areas are required.")
        @Size(max = 500, message = "Expertise areas text is too long.")
        String expertiseAreas,

        @Min(value = 1, message = "Capacity must be at least 1.")
        @Max(value = 20, message = "Capacity cannot exceed 20.")
        int maxMentees
) {}
