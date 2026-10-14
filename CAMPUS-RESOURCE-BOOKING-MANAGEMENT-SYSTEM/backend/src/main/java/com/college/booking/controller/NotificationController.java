package com.college.booking.controller;

import com.college.booking.entity.Notification;
import com.college.booking.security.SecurityUtils;
import com.college.booking.service.CampusService;
import com.college.booking.service.NotificationService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class NotificationController {

    private final NotificationService notificationService;
    private final CampusService campusService;

    public NotificationController(NotificationService notificationService, CampusService campusService) {
        this.notificationService = notificationService;
        this.campusService = campusService;
    }

    @GetMapping("/notifications")
    public List<Notification> list() {
        return notificationService.forUser(SecurityUtils.currentUserId());
    }

    @GetMapping("/notifications/unread")
    public Map<String, Long> unread() {
        return Map.of("count", notificationService.unread(SecurityUtils.currentUserId()));
    }

    @PostMapping("/notifications/{id}/read")
    public Map<String, String> read(@PathVariable Long id) {
        notificationService.markRead(id, SecurityUtils.currentUserId());
        return Map.of("status", "read");
    }

    @PostMapping("/notifications/read-all")
    public Map<String, String> readAll() {
        notificationService.markAllRead(SecurityUtils.currentUserId());
        return Map.of("status", "read");
    }

    @PostMapping("/favorites/{resourceId}")
    public Map<String, String> favorite(@PathVariable Long resourceId) {
        campusService.toggleFavorite(SecurityUtils.currentUser(), resourceId);
        return Map.of("status", "toggled");
    }

    @GetMapping("/favorites")
    public Object favorites() {
        return campusService.favorites(SecurityUtils.currentUser());
    }

    @GetMapping("/recent")
    public Object recent() {
        return campusService.recent(SecurityUtils.currentUser());
    }
}
