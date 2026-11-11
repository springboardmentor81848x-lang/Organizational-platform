package com.skillsphere.dto;

import jakarta.validation.constraints.NotNull;

public record UpdateMentorStatusRequest(
        @NotNull(message = "Active status is required.")
        Boolean isActive
) {}
