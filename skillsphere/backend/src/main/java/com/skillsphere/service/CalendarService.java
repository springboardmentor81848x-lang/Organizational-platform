package com.skillsphere.service;

import com.skillsphere.dto.*;
import com.skillsphere.entity.*;
import com.skillsphere.exception.ApiException;
import com.skillsphere.repository.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Service
public class CalendarService {

    private final CalendarEventRepository eventRepository;
    private final EventParticipantRepository participantRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;

    public CalendarService(CalendarEventRepository eventRepository,
                           EventParticipantRepository participantRepository,
                           UserRepository userRepository,
                           NotificationService notificationService) {
        this.eventRepository = eventRepository;
        this.participantRepository = participantRepository;
        this.userRepository = userRepository;
        this.notificationService = notificationService;
    }

    @Transactional(readOnly = true)
    public List<CalendarEventDto> getUserEvents(String email, Instant from, Instant to) {
        User user = getUserByEmail(email);
        List<CalendarEvent> events;
        if (from != null && to != null) {
            events = eventRepository.findUserEventsInRange(user.getId(), from, to);
        } else {
            events = eventRepository.findUserEvents(user.getId());
        }

        return events.stream().map(this::toEventDto).toList();
    }

    @Transactional
    public CalendarEventDto createEvent(String email, CreateEventRequest req) {
        User creator = getUserByEmail(email);

        CalendarEvent event = new CalendarEvent(
                creator,
                req.title().trim(),
                req.description(),
                req.eventType(),
                req.startTime(),
                req.endTime(),
                req.locationOrLink(),
                req.referenceId()
        );
        CalendarEvent saved = eventRepository.save(event);

        List<EventParticipant> participants = new ArrayList<>();
        if (req.participantUserIds() != null) {
            for (Long uid : req.participantUserIds()) {
                userRepository.findById(uid).ifPresent(u -> {
                    EventParticipant ep = new EventParticipant(saved, u);
                    participants.add(participantRepository.save(ep));

                    notificationService.createNotification(
                            u,
                            "CALENDAR_INVITATION",
                            "Calendar Invitation: " + saved.getTitle(),
                            creator.getFullName() + " invited you to " + saved.getTitle() + ".",
                            "/dashboard/employee"
                    );
                });
            }
        }

        return toEventDto(saved);
    }

    @Transactional
    public void updateRsvpStatus(String email, Long eventId, String status) {
        User user = getUserByEmail(email);
        List<EventParticipant> participants = participantRepository.findByEventId(eventId);
        for (EventParticipant ep : participants) {
            if (ep.getUser().getId().equals(user.getId())) {
                ep.setStatus(status.toUpperCase());
                participantRepository.save(ep);
                return;
            }
        }
        throw new ApiException(HttpStatus.NOT_FOUND, "Participant record not found for event.");
    }

    private CalendarEventDto toEventDto(CalendarEvent e) {
        List<EventParticipant> participants = participantRepository.findByEventId(e.getId());
        List<CalendarEventDto.ParticipantStatusDto> partDtos = participants.stream()
                .map(p -> new CalendarEventDto.ParticipantStatusDto(
                        p.getUser().getId(),
                        p.getUser().getFullName(),
                        p.getStatus()
                )).toList();

        return new CalendarEventDto(
                e.getId(),
                e.getCreator().getId(),
                e.getCreator().getFullName(),
                e.getTitle(),
                e.getDescription(),
                e.getEventType(),
                e.getStartTime(),
                e.getEndTime(),
                e.getLocationOrLink(),
                e.getReferenceId(),
                partDtos
        );
    }

    private User getUserByEmail(String email) {
        return userRepository.findByEmail(email.trim().toLowerCase())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "User not found."));
    }
}
