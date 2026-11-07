package com.skillsphere.dto;

import java.time.Instant;

public record AuditLogDto(
        Long id,
        Long userId,
        String userName,
        String action,
        String entityName,
        String entityId,
        String details,
        String ipAddress,
        Instant createdAt
) {}
