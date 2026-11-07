package com.skillsphere.service;

import com.skillsphere.dto.NotificationDto;
import com.skillsphere.entity.Notification;
import com.skillsphere.entity.User;
import com.skillsphere.repository.NotificationRepository;
import com.skillsphere.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;

    public NotificationService(NotificationRepository notificationRepository, UserRepository userRepository) {
        this.notificationRepository = notificationRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public void createNotification(User recipient, String type, String title, String message, String link) {
        if (recipient == null) return;
        Notification n = new Notification(recipient, type, title, message, link);
        notificationRepository.save(n);
    }

    @Transactional
    public void createNotification(Long recipientId, String type, String title, String message, String link) {
        userRepository.findById(recipientId).ifPresent(u -> createNotification(u, type, title, message, link));
    }

    @Transactional(readOnly = true)
    public List<NotificationDto> getUserNotifications(String email) {
        User user = userRepository.findByEmail(email.trim().toLowerCase()).orElse(null);
        if (user == null) return List.of();
        return notificationRepository.findByRecipientIdOrderByCreatedAtDesc(user.getId())
                .stream().map(this::toDto).toList();
    }

    @Transactional(readOnly = true)
    public long getUnreadCount(String email) {
        User user = userRepository.findByEmail(email.trim().toLowerCase()).orElse(null);
        if (user == null) return 0;
        return notificationRepository.countByRecipientIdAndIsReadFalse(user.getId());
    }

    @Transactional
    public void markAsRead(String email, Long notificationId) {
        notificationRepository.findById(notificationId).ifPresent(n -> {
            if (n.getRecipient().getEmail().equalsIgnoreCase(email)) {
                n.setRead(true);
                notificationRepository.save(n);
            }
        });
    }

    @Transactional
    public void markAllAsRead(String email) {
        User user = userRepository.findByEmail(email.trim().toLowerCase()).orElse(null);
        if (user == null) return;
        List<Notification> list = notificationRepository.findByRecipientIdOrderByCreatedAtDesc(user.getId());
        for (Notification n : list) {
            if (!n.isRead()) {
                n.setRead(true);
            }
        }
        notificationRepository.saveAll(list);
    }

    private NotificationDto toDto(Notification n) {
        return new NotificationDto(
                n.getId(),
                n.getType(),
                n.getTitle(),
                n.getMessage(),
                n.getLink(),
                n.isRead(),
                n.getCreatedAt()
        );
    }
}
