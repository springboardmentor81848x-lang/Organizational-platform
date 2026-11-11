package com.skillsphere.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.List;

public record CreateEventRequest(
        @NotBlank(message = "Title is required.")
        String title,

        String description,

        @NotBlank(message = "Event type is required.")
        String eventType,

        @NotNull(message = "Start time is required.")
        Instant startTime,

        @NotNull(message = "End time is required.")
        Instant endTime,

        String locationOrLink,
        Long referenceId,
        List<Long> participantUserIds
) {}
