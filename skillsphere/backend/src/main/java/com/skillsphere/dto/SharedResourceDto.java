package com.skillsphere.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;

public record SharedResourceDto(
        Long id,
        Long assignmentId,
        String title,
        String urlOrContent,
        Long sharedById,
        String sharedByName,
        Instant createdAt
) {
    public record CreateRequest(
            @NotNull Long assignmentId,
            @NotBlank String title,
            @NotBlank String urlOrContent
    ) {}
}
