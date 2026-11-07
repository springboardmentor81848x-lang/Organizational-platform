package com.skillsphere.dto;

import jakarta.validation.constraints.Pattern;

public record StartAssessmentRequest(
        Long skillId,
        @Pattern(regexp = "^(?i)(Unaware|Beginner|Intermediate|Advanced|Expert)$",
                message = "Proficiency level must be one of: Unaware, Beginner, Intermediate, Advanced, Expert.")
        String proficiencyLevel,
        Long assignmentId,
        Long assessmentId
) {}
