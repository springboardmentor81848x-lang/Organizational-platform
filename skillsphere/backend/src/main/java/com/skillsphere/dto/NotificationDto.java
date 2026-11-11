package com.skillsphere.dto;

import java.time.Instant;

public record NotificationDto(
        Long id,
        String type,
        String title,
        String message,
        String link,
        boolean isRead,
        Instant createdAt
) {}
