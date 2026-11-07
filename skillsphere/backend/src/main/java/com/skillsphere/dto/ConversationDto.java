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
        long unreadCount,
        boolean isMuted,
        boolean isArchived
) {
    public ConversationDto(Long id, String type, String title, Instant updatedAt,
                           List<ParticipantDto> participants, MessageDto latestMessage, long unreadCount) {
        this(id, type, title, updatedAt, participants, latestMessage, unreadCount, false, false);
    }

    public record ParticipantDto(
            Long userId,
            String fullName,
            String email,
            String role
    ) {}
}
