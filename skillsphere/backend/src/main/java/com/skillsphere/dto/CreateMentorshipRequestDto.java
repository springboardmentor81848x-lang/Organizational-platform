package com.skillsphere.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateMentorshipRequestDto(
        @NotNull(message = "Mentor ID is required.")
        Long mentorId,

        @Size(max = 1000, message = "Message is too long.")
        String message
) {}
