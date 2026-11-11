package com.skillsphere.service;

import com.skillsphere.dto.NotificationDto;
import com.skillsphere.entity.Notification;
import com.skillsphere.entity.Role;
import com.skillsphere.entity.User;
import com.skillsphere.repository.NotificationRepository;
import com.skillsphere.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private NotificationRepository notificationRepository;
    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private NotificationService notificationService;

    private User user;
    private Notification notification1;
    private Notification notification2;

    @BeforeEach
    void setUp() throws Exception {
        user = new User();
        var field = User.class.getDeclaredField("id");
        field.setAccessible(true);
        field.set(user, 1L);
        user.setEmail("user@example.com");
        user.setFullName("Test User");
        user.setRole(Role.EMPLOYEE);

        notification1 = new Notification(user, "TEST_TYPE", "Title 1", "Message 1", "/link1");
        notification2 = new Notification(user, "TEST_TYPE", "Title 2", "Message 2", "/link2");
    }

    @Test
    @DisplayName("markAsRead sets isRead to true for the specified notification")
    void testMarkAsRead_Success() {
        when(notificationRepository.findById(10L)).thenReturn(Optional.of(notification1));

        assertFalse(notification1.isRead());
        notificationService.markAsRead("user@example.com", 10L);

        assertTrue(notification1.isRead());
        verify(notificationRepository, times(1)).save(notification1);
    }

    @Test
    @DisplayName("markAsRead for notification belonging to another user does not mark it as read")
    void testMarkAsRead_AnotherUser_DoesNotMarkRead() {
        when(notificationRepository.findById(10L)).thenReturn(Optional.of(notification1));

        notificationService.markAsRead("intruder@example.com", 10L);

        assertFalse(notification1.isRead());
        verify(notificationRepository, never()).save(any());
    }

    @Test
    @DisplayName("markAllAsRead marks all user notifications as read")
    void testMarkAllAsRead_Success() {
        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(user));
        when(notificationRepository.findByRecipientIdOrderByCreatedAtDesc(1L)).thenReturn(List.of(notification1, notification2));

        assertFalse(notification1.isRead());
        assertFalse(notification2.isRead());

        notificationService.markAllAsRead("user@example.com");

        assertTrue(notification1.isRead());
        assertTrue(notification2.isRead());
        verify(notificationRepository, times(1)).saveAll(any());
    }

    @Test
    @DisplayName("getUserNotifications returns mapped DTOs for the user")
    void testGetUserNotifications() {
        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(user));
        when(notificationRepository.findByRecipientIdOrderByCreatedAtDesc(1L)).thenReturn(List.of(notification1));

        List<NotificationDto> result = notificationService.getUserNotifications("user@example.com");

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("Title 1", result.get(0).title());
    }

    @Test
    @DisplayName("getUnreadCount returns unread count for user")
    void testGetUnreadCount() {
        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(user));
        when(notificationRepository.countByRecipientIdAndIsReadFalse(1L)).thenReturn(5L);

        long count = notificationService.getUnreadCount("user@example.com");

        assertEquals(5L, count);
    }
}
