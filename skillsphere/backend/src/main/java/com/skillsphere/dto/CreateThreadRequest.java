package com.skillsphere.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateThreadRequest(
        @NotBlank(message = "Title is required.")
        @Size(max = 140)
        String title,

        @NotBlank(message = "Content is required.")
        String content,

        String category
) {}
