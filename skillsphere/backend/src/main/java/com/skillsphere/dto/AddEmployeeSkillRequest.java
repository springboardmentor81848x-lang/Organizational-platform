package com.skillsphere.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record AddEmployeeSkillRequest(
        @NotNull(message = "Skill ID is required.")
        Long skillId,

        @NotBlank(message = "Proficiency level is required.")
        @Pattern(regexp = "^(Unaware|Beginner|Intermediate|Advanced|Expert)$",
                message = "Proficiency must be one of: Unaware, Beginner, Intermediate, Advanced, Expert.")
        String proficiencyLevel
) {}
