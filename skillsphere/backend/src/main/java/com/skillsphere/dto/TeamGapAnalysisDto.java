package com.skillsphere.dto;

import java.time.Instant;

public record TeamGapAnalysisDto(
        Long employeeId,
        String employeeName,
        Long teamId,
        String teamName,
        Long skillId,
        String skillName,
        String categoryName,
        int currentLevel,
        String currentLevelName,
        int requiredLevel,
        String requiredLevelName,
        int gapSize,
        String priority, // HIGH, MEDIUM, LOW
        Instant lastAssessedAt
) {}
