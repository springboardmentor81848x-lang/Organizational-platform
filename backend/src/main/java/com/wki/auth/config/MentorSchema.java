package com.wki.auth.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Extra columns for the mentor dashboard: what a session covers, how long it runs, whether it is online,
 * and whether each registered employee attended. Runs after the employee tables exist. Safe on every startup.
 */
@Component
@Order(4)
public class MentorSchema implements CommandLineRunner {
    private final JdbcTemplate jdbc;

    public MentorSchema(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public void run(String... args) {
        try {
            add("ed_knowledge_sessions", "skill_id", "BIGINT NULL");
            add("ed_knowledge_sessions", "duration_min", "INT NOT NULL DEFAULT 60");
            add("ed_knowledge_sessions", "session_mode", "VARCHAR(20) NOT NULL DEFAULT 'ONLINE'");
            add("ed_knowledge_sessions", "location", "VARCHAR(200) NULL");
            add("ed_session_registrations", "attended", "TINYINT NULL");
            jdbc.execute("CREATE TABLE IF NOT EXISTS ed_mentor_profiles (employee_id BIGINT PRIMARY KEY, headline VARCHAR(200) NULL, " +
                    "accepting TINYINT NOT NULL DEFAULT 1, updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP)");
            jdbc.execute("CREATE TABLE IF NOT EXISTS ed_mentor_skills (employee_id BIGINT NOT NULL, skill_id BIGINT NOT NULL, " +
                    "PRIMARY KEY (employee_id, skill_id))");
            System.out.println("MentorSchema: mentor dashboard columns are ready");
        } catch (Exception e) {
            System.err.println("MentorSchema FAILED: " + e.getMessage());
            System.err.println("Add these columns by hand: ed_knowledge_sessions(skill_id BIGINT NULL, duration_min INT NOT NULL DEFAULT 60, " +
                    "session_mode VARCHAR(20) NOT NULL DEFAULT 'ONLINE', location VARCHAR(200) NULL), ed_session_registrations(attended TINYINT NULL)");
        }
    }

    private void add(String table, String column, String definition) {
        Integer c = jdbc.queryForObject(
                "SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = ? AND column_name = ?",
                Integer.class, table, column);
        if (c == null || c == 0) {
            jdbc.execute("ALTER TABLE " + table + " ADD COLUMN " + column + " " + definition);
            System.out.println("MentorSchema: added " + table + "." + column);
        }
    }
}
