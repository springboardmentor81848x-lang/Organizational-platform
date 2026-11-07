package com.skillsphere.dto;

import java.time.Instant;

public record SkillEvidenceDto(
        Long id,
        Long employeeSkillId,
        String skillName,
        String title,
        String description,
        String evidenceUrl,
        String status,
        String reviewedByName,
        Instant createdAt
) {}
