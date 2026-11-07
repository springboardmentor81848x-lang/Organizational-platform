package com.skillsphere.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateArticleRequest(
        Long categoryId,

        @NotBlank(message = "Title is required.")
        @Size(max = 140)
        String title,

        @NotBlank(message = "Content is required.")
        String content,

        @Size(max = 200)
        String tags,

        String status // DRAFT, PUBLISHED
) {}
