package com.skillsphere.dto;

import jakarta.validation.constraints.NotNull;
import java.util.Map;

public record SubmitAssessmentRequest(
        Long assignmentId,
        @NotNull(message = "Answers map is required.")
        Map<Long, Integer> answers // questionId -> selectedOptionIndex
) {}
