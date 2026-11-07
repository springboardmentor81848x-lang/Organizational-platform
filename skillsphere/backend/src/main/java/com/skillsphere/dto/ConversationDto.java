package com.skillsphere.dto;

import java.time.Instant;
import java.util.List;

public record ConversationDto(
        Long id,
        String type, // DIRECT, TEAM
        String title,
        Instant updatedAt,
        List<ParticipantDto> participants,
        MessageDto latestMessage,
        long unreadCount
) {
    public record ParticipantDto(
            Long userId,
            String fullName,
            String email,
            String role
    ) {}
}
