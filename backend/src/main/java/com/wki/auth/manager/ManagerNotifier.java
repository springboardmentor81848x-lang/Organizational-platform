package com.wki.auth.manager;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Creates notifications for team managers when something happens to one of their employees.
 * Goes to the employee's own manager (employees.manager_id) when one is set, otherwise to every
 * active employee with the MANAGER role. A failure here never breaks the employee's own action.
 */
@Component
public class ManagerNotifier {
    private final JdbcTemplate jdbc;

    public ManagerNotifier(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public void notifyManagers(long employeeId, String type, String what) {
        try {
            List<String> names = jdbc.queryForList(
                    "SELECT CONCAT(first_name, CASE WHEN last_name = '-' THEN '' ELSE CONCAT(' ', last_name) END) " +
                    "FROM employees WHERE employee_id = ?", String.class, employeeId);
            String who = names.isEmpty() ? "An employee" : names.get(0);
            String message = who + " " + what;
            if (message.length() > 300) message = message.substring(0, 297) + "...";

            List<Long> managers = jdbc.queryForList(
                    "SELECT manager_id FROM employees WHERE employee_id = ? AND manager_id IS NOT NULL", Long.class, employeeId);
            if (managers.isEmpty()) {
                managers = jdbc.queryForList(
                        "SELECT e.employee_id FROM employees e JOIN roles r ON r.role_id = e.role_id " +
                        "WHERE r.role_name = 'MANAGER' AND e.status = 'ACTIVE' AND e.employee_id <> ?", Long.class, employeeId);
            }
            for (Long m : managers) {
                jdbc.update("INSERT INTO mgr_notifications (manager_id, employee_id, type, message) VALUES (?,?,?,?)",
                        m, employeeId, type, message);
            }
        } catch (Exception e) {
            System.err.println("ManagerNotifier: could not create notification: " + e.getMessage());
        }
    }
}
