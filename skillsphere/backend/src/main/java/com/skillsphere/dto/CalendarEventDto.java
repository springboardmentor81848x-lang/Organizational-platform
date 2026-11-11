package com.skillsphere.dto;

import java.time.Instant;
import java.util.List;

public record CalendarEventDto(
        Long id,
        Long creatorId,
        String creatorName,
        String title,
        String description,
        String eventType, // ASSESSMENT, MENTORSHIP, LEARNING, TRAINING, TEAM
        Instant startTime,
        Instant endTime,
        String locationOrLink,
        Long referenceId,
        List<ParticipantStatusDto> participants
) {
    public record ParticipantStatusDto(
            Long userId,
            String fullName,
            String status
    ) {}
}
