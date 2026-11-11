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
        Instant createdAt,
        String employeeName
) {
    public SkillEvidenceDto(Long id, Long employeeSkillId, String skillName, String title,
                            String description, String evidenceUrl, String status,
                            String reviewedByName, Instant createdAt) {
        this(id, employeeSkillId, skillName, title, description, evidenceUrl, status, reviewedByName, createdAt, null);
    }
}
