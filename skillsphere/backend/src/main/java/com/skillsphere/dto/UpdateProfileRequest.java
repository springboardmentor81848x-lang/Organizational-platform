package com.skillsphere.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateProfileRequest(
        @NotBlank(message = "Job role is required.")
        @Size(max = 80)
        String jobRole,

        @Size(max = 80)
        String department,

        @NotBlank(message = "Experience level is required.")
        @Size(max = 40)
        String experienceLevel,

        @Size(max = 1000)
        String bio,

        @Size(max = 500)
        String skills
) {}
