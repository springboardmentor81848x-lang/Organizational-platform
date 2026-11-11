package com.skillsphere.dto;

import java.util.List;

public record AssessmentDetailDto(
        Long id,
        String title,
        String description,
        String targetRole,
        int passScore,
        int timeLimitMinutes,
        List<QuestionDto> questions
) {}
