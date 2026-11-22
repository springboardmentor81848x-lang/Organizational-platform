package com.wki.auth.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Creates the two tables behind the Messages feature (employee-employee chat and employee-mentor chat):
 *   ed_connections  follow requests between employees (PENDING until the other person accepts)
 *   ed_messages     the chat messages themselves
 * Safe to run on every startup: it only creates what is missing and never touches your existing tables.
 */
@Component
@Order(6)
public class MessagingSchema implements CommandLineRunner {
    private final JdbcTemplate jdbc;

    public MessagingSchema(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public void run(String... args) {
        try {
            jdbc.execute("CREATE TABLE IF NOT EXISTS ed_connections (" +
                    "connection_id BIGINT AUTO_INCREMENT PRIMARY KEY, " +
                    "requester_id BIGINT NOT NULL, " +
                    "addressee_id BIGINT NOT NULL, " +
                    "status VARCHAR(12) NOT NULL DEFAULT 'PENDING', " +
                    "created_at DATETIME DEFAULT CURRENT_TIMESTAMP, " +
                    "responded_at DATETIME NULL, " +
                    "UNIQUE KEY uq_ed_connection (requester_id, addressee_id), " +
                    "INDEX idx_ed_conn_inbox (addressee_id, status)" +
                    ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");
            jdbc.execute("CREATE TABLE IF NOT EXISTS ed_messages (" +
                    "message_id BIGINT AUTO_INCREMENT PRIMARY KEY, " +
                    "sender_id BIGINT NOT NULL, " +
                    "recipient_id BIGINT NOT NULL, " +
                    "body VARCHAR(2000) NOT NULL, " +
                    "created_at DATETIME DEFAULT CURRENT_TIMESTAMP, " +
                    "read_at DATETIME NULL, " +
                    "INDEX idx_ed_msg_pair (sender_id, recipient_id, message_id), " +
                    "INDEX idx_ed_msg_inbox (recipient_id, read_at)" +
                    ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");
            System.out.println("MessagingSchema: messaging tables are ready");
        } catch (Exception e) {
            System.err.println("MessagingSchema FAILED: " + e.getMessage());
            System.err.println("Run database/messaging.sql by hand in MySQL Workbench and restart.");
        }
    }
}
