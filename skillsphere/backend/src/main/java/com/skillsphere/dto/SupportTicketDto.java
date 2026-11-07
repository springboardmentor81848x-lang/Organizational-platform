package com.skillsphere.dto;

import java.time.Instant;

public record SupportTicketDto(
        Long id,
        Long userId,
        String userName,
        String userEmail,
        String subject,
        String description,
        String priority,
        String status,
        String adminNotes,
        Instant createdAt,
        Instant updatedAt
) {
    public record CreateRequest(
            String subject,
            String description,
            String priority
    ) {}
    public record UpdateRequest(
            String status,
            String adminNotes
    ) {}
}
