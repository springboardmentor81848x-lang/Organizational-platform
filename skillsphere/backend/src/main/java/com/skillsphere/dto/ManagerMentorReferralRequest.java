package com.skillsphere.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ManagerMentorReferralRequest(
        @NotNull(message = "Employee ID is required")
        Long employeeId,

        @NotNull(message = "Mentor ID is required")
        Long mentorId,

        Long skillId,

        @NotBlank(message = "Referral note is required")
        @Size(max = 1000, message = "Note cannot exceed 1000 characters")
        String note
) {}
