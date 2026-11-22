package com.wki.auth.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;

/** Creates the mgr_notifications table on startup (only if it is missing). */
@Component
@Order(2)
public class ManagerNotificationSchema implements CommandLineRunner {
    private final DataSource dataSource;

    public ManagerNotificationSchema(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public void run(String... args) {
        try {
            ResourceDatabasePopulator p = new ResourceDatabasePopulator(new ClassPathResource("db/manager_notifications.sql"));
            p.setSqlScriptEncoding("UTF-8");
            p.execute(dataSource);
            System.out.println("ManagerNotificationSchema: manager notification table is ready");
        } catch (Exception e) {
            System.err.println("ManagerNotificationSchema FAILED: " + e.getMessage());
            System.err.println("Run database/manager_notifications.sql by hand in MySQL and restart.");
        }
    }
}
