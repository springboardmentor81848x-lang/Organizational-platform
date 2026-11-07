package com.skillsphere.controller;

import com.skillsphere.dto.NotificationDto;
import com.skillsphere.service.NotificationService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping
    public List<NotificationDto> getMyNotifications(Authentication auth) {
        return notificationService.getUserNotifications(auth.getName());
    }

    @GetMapping("/unread-count")
    public Map<String, Long> getUnreadCount(Authentication auth) {
        return Map.of("unreadCount", notificationService.getUnreadCount(auth.getName()));
    }

    @RequestMapping(value = "/{id}/read", method = {RequestMethod.PATCH, RequestMethod.PUT})
    public Map<String, Boolean> markRead(@PathVariable Long id, Authentication auth) {
        notificationService.markAsRead(auth.getName(), id);
        return Map.of("success", true);
    }

    @RequestMapping(value = "/read-all", method = {RequestMethod.POST, RequestMethod.PUT})
    public Map<String, Boolean> markAllRead(Authentication auth) {
        notificationService.markAllAsRead(auth.getName());
        return Map.of("success", true);
    }
}
