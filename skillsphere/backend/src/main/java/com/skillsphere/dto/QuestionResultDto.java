package com.skillsphere.dto;

import java.util.List;

public record QuestionResultDto(
        Long questionId,
        String questionText,
        String subtopic,
        String difficulty,
        List<String> options,
        int selectedOptionIndex,
        int correctAnswerIndex,
        boolean isCorrect,
        String explanation
) {}
