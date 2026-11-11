package com.skillsphere.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RespondMentorshipRequestDto(
        @NotBlank(message = "Status is required.")
        @Pattern(regexp = "^(ACCEPTED|DECLINED)$", message = "Status must be ACCEPTED or DECLINED.")
        String status,

        @Size(max = 500)
        String responseNote
) {}
