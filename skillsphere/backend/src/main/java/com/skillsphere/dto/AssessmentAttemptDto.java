package com.skillsphere.dto;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public record AssessmentAttemptDto(
        Long attemptId,
        Long skillId,
        String skillName,
        String categoryName,
        String targetLevel,
        String currentLevel,
        String status,
        String generationSource,
        int totalQuestions,
        int timeLimitMinutes,
        int passScore,
        Instant startedAt,
        Map<Long, Integer> savedAnswers,
        List<QuestionDto> questions
) {}
