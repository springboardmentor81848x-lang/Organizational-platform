package com.skillsphere.dto;

import java.time.Instant;

public record ArticleDto(
        Long id,
        Long categoryId,
        String categoryName,
        Long authorId,
        String authorName,
        String title,
        String content,
        String tags,
        String status,
        Instant createdAt,
        Instant updatedAt
) {}
