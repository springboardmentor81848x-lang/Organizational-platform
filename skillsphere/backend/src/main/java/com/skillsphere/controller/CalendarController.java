package com.skillsphere.controller;

import com.skillsphere.dto.*;
import com.skillsphere.service.CalendarService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/calendar")
public class CalendarController {

    private final CalendarService calendarService;

    public CalendarController(CalendarService calendarService) {
        this.calendarService = calendarService;
    }

    @GetMapping("/events")
    public ResponseEntity<List<CalendarEventDto>> getEvents(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to) {
        return ResponseEntity.ok(calendarService.getUserEvents(userDetails.getUsername(), from, to));
    }

    @PostMapping("/events")
    public ResponseEntity<CalendarEventDto> createEvent(@AuthenticationPrincipal UserDetails userDetails,
                                                        @Valid @RequestBody CreateEventRequest req) {
        CalendarEventDto event = calendarService.createEvent(userDetails.getUsername(), req);
        return ResponseEntity.status(HttpStatus.CREATED).body(event);
    }

    @PutMapping("/events/{id}/rsvp")
    public ResponseEntity<Void> rsvp(@AuthenticationPrincipal UserDetails userDetails,
                                     @PathVariable Long id,
                                     @RequestBody Map<String, String> body) {
        String status = body.getOrDefault("status", "ACCEPTED");
        calendarService.updateRsvpStatus(userDetails.getUsername(), id, status);
        return ResponseEntity.noContent().build();
    }
}
