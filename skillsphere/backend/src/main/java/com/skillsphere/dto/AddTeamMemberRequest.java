package com.skillsphere.dto;

import jakarta.validation.constraints.NotNull;

public record AddTeamMemberRequest(
        @NotNull(message = "User ID is required")
        Long userId,

        String roleInTeam
) {}
