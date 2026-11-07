package com.skillsphere.dto;

import java.time.Instant;

public record MessageDto(
        Long id,
        Long conversationId,
        Long senderId,
        String senderName,
        String content,
        String attachmentUrl,
        String attachmentName,
        Instant createdAt
) {}
