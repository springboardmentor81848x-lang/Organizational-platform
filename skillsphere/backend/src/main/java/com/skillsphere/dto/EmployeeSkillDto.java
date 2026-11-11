package com.skillsphere.dto;

import java.time.Instant;

public record EmployeeSkillDto(
        Long id,
        Long userId,
        String userName,
        Long skillId,
        String skillName,
        String categoryName,
        String proficiencyLevel,
        int numericLevel,
        boolean verified,
        String verifiedByName,
        Instant updatedAt
) {}
