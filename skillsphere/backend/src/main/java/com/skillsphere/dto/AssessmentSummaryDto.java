package com.skillsphere.dto;

public record AssessmentSummaryDto(
        Long id,
        String title,
        String description,
        Long categoryId,
        String categoryName,
        String targetRole,
        int passScore,
        int timeLimitMinutes,
        int questionCount
) {}
