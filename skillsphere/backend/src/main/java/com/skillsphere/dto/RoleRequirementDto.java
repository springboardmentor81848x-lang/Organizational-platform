package com.skillsphere.dto;

public record RoleRequirementDto(
        Long id,
        String jobRole,
        Long skillId,
        String skillName,
        String categoryName,
        String requiredProficiency,
        int requiredLevel,
        String priority
) {}
