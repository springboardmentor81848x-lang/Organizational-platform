package com.skillsphere.dto;

import java.time.Instant;

public record ReplyDto(
        Long id,
        Long threadId,
        Long authorId,
        String authorName,
        String content,
        boolean isAcceptedAnswer,
        Instant createdAt
) {}
