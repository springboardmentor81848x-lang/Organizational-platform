package com.wki.auth.manager;

import com.wki.auth.employee.CurrentEmployee;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/** Notifications for the logged-in team manager. Only the MANAGER role can reach /api/manager/** (see SecurityConfig). */
@RestController
@RequestMapping("/api/manager")
public class ManagerNotificationController {
    private final JdbcTemplate jdbc;
    private final CurrentEmployee current;

    public ManagerNotificationController(JdbcTemplate jdbc, CurrentEmployee current) {
        this.jdbc = jdbc;
        this.current = current;
    }

    @GetMapping("/notifications")
    public Map<String, Object> list() {
        long me = current.id();
        List<Map<String, Object>> items = jdbc.queryForList(
                "SELECT notification_id AS notificationId, type, message, is_read AS isRead, created_at AS createdAt " +
                "FROM mgr_notifications WHERE manager_id = ? ORDER BY created_at DESC, notification_id DESC LIMIT 100", me);
        Integer unread = jdbc.queryForObject(
                "SELECT COUNT(*) FROM mgr_notifications WHERE manager_id = ? AND is_read = 0", Integer.class, me);
        return Map.of("unread", unread == null ? 0 : unread, "items", items);
    }

    @PutMapping("/notifications/{id}/read")
    public Map<String, Object> markRead(@PathVariable long id) {
        jdbc.update("UPDATE mgr_notifications SET is_read = 1 WHERE notification_id = ? AND manager_id = ?", id, current.id());
        return Map.of("message", "Marked read");
    }

    @PutMapping("/notifications/read-all")
    public Map<String, Object> markAllRead() {
        jdbc.update("UPDATE mgr_notifications SET is_read = 1 WHERE manager_id = ?", current.id());
        return Map.of("message", "All marked read");
    }
}
