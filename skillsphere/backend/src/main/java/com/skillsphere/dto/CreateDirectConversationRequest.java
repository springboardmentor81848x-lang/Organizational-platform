package com.skillsphere.dto;

import jakarta.validation.constraints.NotNull;

public record CreateDirectConversationRequest(
        @NotNull(message = "Recipient user ID is required.")
        Long targetUserId
) {}
