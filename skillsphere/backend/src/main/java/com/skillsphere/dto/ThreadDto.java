package com.skillsphere.dto;

import java.time.Instant;
import java.util.List;

public record ThreadDto(
        Long id,
        Long authorId,
        String authorName,
        String title,
        String content,
        String category,
        Instant createdAt,
        Instant updatedAt,
        int replyCount,
        List<ReplyDto> replies
) {}
