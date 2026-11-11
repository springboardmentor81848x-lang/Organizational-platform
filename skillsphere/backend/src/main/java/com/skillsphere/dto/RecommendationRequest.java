package com.skillsphere.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.util.List;

/** Assessment result sent by the Employee dashboard so Gemini can recommend courses. */
public record RecommendationRequest(
        @Size(max = 80, message = "Job role is too long.") String jobRole,
        @Size(max = 40, message = "Experience level is too long.") String experience,
        @Size(max = 80, message = "Department is too long.") String department,
        @Min(0) @Max(100) int overallScore,
        @NotEmpty(message = "Assessment results are required.")
        @Size(max = 12, message = "Too many skills.")
        List<@Valid SkillResult> skillResults
) {
    public record SkillResult(
            @NotBlank @Size(max = 60) String skill,
            @Min(0) @Max(50) int correct,
            @Min(1) @Max(50) int total,
            @Min(0) @Max(5) int level,
            @Min(1) @Max(5) int required
    ) {}
}
