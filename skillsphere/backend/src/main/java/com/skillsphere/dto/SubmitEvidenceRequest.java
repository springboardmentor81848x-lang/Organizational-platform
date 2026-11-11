package com.skillsphere.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record SubmitEvidenceRequest(
        @NotNull(message = "Employee Skill ID is required.")
        Long employeeSkillId,

        @NotBlank(message = "Title is required.")
        @Size(max = 140)
        String title,

        @Size(max = 2000)
        String description,

        @Size(max = 500)
        String evidenceUrl
) {}
