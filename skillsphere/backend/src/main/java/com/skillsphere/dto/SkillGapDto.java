package com.skillsphere.dto;

import java.time.Instant;

public record SkillGapDto(
        Long skillId,
        String skillName,
        String categoryName,
        int currentLevel,
        String currentProficiency,
        int requiredLevel,
        String requiredProficiency,
        int gapSize,
        String priority, // HIGH, MEDIUM, LOW, ON_TRACK
        Instant calculatedAt
) {}
