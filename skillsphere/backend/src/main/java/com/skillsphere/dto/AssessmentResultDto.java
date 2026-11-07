package com.skillsphere.dto;

import java.time.Instant;
import java.util.List;

public record AssessmentResultDto(
        Long submissionId,
        Long assessmentId,
        String assessmentTitle,
        int score,
        boolean passed,
        int totalQuestions,
        int correctAnswers,
        Instant completedAt,
        List<SkillGapDto> updatedGaps,
        Long attemptId,
        String generationSource,
        String targetLevel,
        String skillName,
        List<QuestionResultDto> questionResults
) {
    public AssessmentResultDto(Long submissionId, Long assessmentId, String assessmentTitle,
                               int score, boolean passed, int totalQuestions, int correctAnswers,
                               Instant completedAt, List<SkillGapDto> updatedGaps) {
        this(submissionId, assessmentId, assessmentTitle, score, passed, totalQuestions,
             correctAnswers, completedAt, updatedGaps, null, "FALLBACK", "Intermediate", "General", List.of());
    }
}
