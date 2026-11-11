package com.skillsphere.dto;

import java.time.Instant;

public record FeedbackResponse(
        Long id,
        Long sessionId,
        Long authorId,
        String authorName,
        int rating,
        String comments,
        Instant createdAt
) {}
