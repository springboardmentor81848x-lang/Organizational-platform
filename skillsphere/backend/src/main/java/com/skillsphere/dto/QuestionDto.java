package com.skillsphere.dto;

import java.util.List;

public record QuestionDto(
        Long id,
        Long skillId,
        String skillName,
        String questionText,
        List<String> options,
        int points,
        String difficulty,
        String subtopic,
        int questionOrder
) {
    public QuestionDto(Long id, Long skillId, String skillName, String questionText, List<String> options, int points) {
        this(id, skillId, skillName, questionText, options, points, "MEDIUM", "General", 0);
    }
}
