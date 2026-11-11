package com.skillsphere.dto;

import java.time.Instant;

public record PlatformFeedbackDto(
        Long id,
        Long userId,
        String userName,
        String category,
        int rating,
        String comments,
        Instant createdAt
) {
    public record CreateRequest(
            String category,
            int rating,
            String comments
    ) {}
}
