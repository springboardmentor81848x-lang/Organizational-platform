package com.wki.auth.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Ensures tables and columns required for the HR Specialist Dashboard exist.
 * Uses CREATE TABLE IF NOT EXISTS and non-destructive ALTER statements.
 */
@Component
@Order(50)
public class HrSchema implements CommandLineRunner {

    private final JdbcTemplate jdbc;

    public HrSchema(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public void run(String... args) {
        try {
            // 1. Table for learning path assignments by HR to employees or departments
            jdbc.execute(
                "CREATE TABLE IF NOT EXISTS ed_learning_path_assignments (" +
                "  assignment_id BIGINT AUTO_INCREMENT PRIMARY KEY," +
                "  path_id BIGINT NOT NULL," +
                "  employee_id BIGINT NOT NULL," +
                "  assigned_by BIGINT," +
                "  status VARCHAR(20) NOT NULL DEFAULT 'ASSIGNED'," +
                "  assigned_at DATETIME DEFAULT CURRENT_TIMESTAMP," +
                "  target_completion_date DATE," +
                "  KEY idx_hr_lpa_emp (employee_id)," +
                "  KEY idx_hr_lpa_path (path_id)" +
                ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci"
            );

            // 2. Table for HR announcements & notifications history
            jdbc.execute(
                "CREATE TABLE IF NOT EXISTS hr_announcements (" +
                "  announcement_id BIGINT AUTO_INCREMENT PRIMARY KEY," +
                "  title VARCHAR(200) NOT NULL," +
                "  message TEXT NOT NULL," +
                "  target_type VARCHAR(50) NOT NULL DEFAULT 'ALL'," + // ALL, DEPARTMENT, ROLE, EMPLOYEE
                "  target_id BIGINT DEFAULT NULL," +
                "  sent_by BIGINT NOT NULL," +
                "  created_at DATETIME DEFAULT CURRENT_TIMESTAMP" +
                ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci"
            );

            // 3. Make sure ed_courses has description and url columns if not present
            try {
                jdbc.execute("ALTER TABLE ed_courses ADD COLUMN description VARCHAR(500) NULL AFTER duration_hours");
            } catch (Exception ignored) {
                // Column already exists
            }
            try {
                jdbc.execute("ALTER TABLE ed_courses ADD COLUMN url VARCHAR(500) NULL AFTER description");
            } catch (Exception ignored) {
                // Column already exists
            }
            try {
                jdbc.execute("ALTER TABLE ed_courses ADD COLUMN is_active TINYINT(1) NOT NULL DEFAULT 1 AFTER url");
            } catch (Exception ignored) {
                // Column already exists
            }

            System.out.println("HrSchema: HR Specialist tables and columns are ready");
        } catch (Exception e) {
            System.err.println("HrSchema initialization notice: " + e.getMessage());
        }
    }
}

