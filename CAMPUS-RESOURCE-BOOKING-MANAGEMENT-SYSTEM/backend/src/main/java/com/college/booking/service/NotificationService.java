package com.college.booking.service;

import com.college.booking.entity.Notification;
import com.college.booking.entity.User;
import com.college.booking.enums.NotificationType;
import com.college.booking.exception.ApiException;
import com.college.booking.repository.NotificationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class NotificationService {

    private final NotificationRepository notificationRepository;

    public NotificationService(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    @Transactional
    public void notify(User user, NotificationType type, String title, String message, String link) {
        Notification n = new Notification();
        n.setUser(user);
        n.setType(type);
        n.setTitle(title);
        n.setMessage(message);
        n.setLink(link);
        n.setReadFlag(false);
        notificationRepository.save(n);
    }

    public List<Notification> forUser(Long userId) {
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    public long unread(Long userId) {
        return notificationRepository.countByUserIdAndReadFlagFalse(userId);
    }

    @Transactional
    public void markRead(Long id, Long userId) {
        Notification n = notificationRepository.findById(id).orElseThrow(() -> ApiException.notFound("Notification not found."));
        if (!n.getUser().getId().equals(userId)) {
            throw ApiException.forbidden("You cannot modify this notification.");
        }
        n.setReadFlag(true);
        notificationRepository.save(n);
    }

    @Transactional
    public void markAllRead(Long userId) {
        notificationRepository.findByUserIdOrderByCreatedAtDesc(userId).forEach(n -> n.setReadFlag(true));
    }
}
