package com.wki.auth.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Adds employees.last_login_at if it is missing, so you don't have to alter the table by hand.
 * Safe to run on every startup.
 */
@Component
@Order(0)
public class SchemaUpdater implements CommandLineRunner {
    private final JdbcTemplate jdbc;

    public SchemaUpdater(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public void run(String... args) {
        Integer count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM information_schema.columns "
                        + "WHERE table_schema = DATABASE() AND table_name = 'employees' "
                        + "AND column_name = 'last_login_at'",
                Integer.class);
        if (count == null || count == 0) {
            jdbc.execute("ALTER TABLE employees ADD COLUMN last_login_at DATETIME NULL");
            System.out.println("SchemaUpdater: added column employees.last_login_at");
        }
    }
}
