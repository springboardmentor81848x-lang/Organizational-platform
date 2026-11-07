package com.skillsphere.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateTeamRequest(
        Long departmentId,

        @NotBlank(message = "Team name is required")
        @Size(max = 100, message = "Team name cannot exceed 100 characters")
        String name,

        Long managerUserId,
        Long managerId
) {
    public Long effectiveManagerUserId() {
        return managerUserId != null ? managerUserId : managerId;
    }
}
