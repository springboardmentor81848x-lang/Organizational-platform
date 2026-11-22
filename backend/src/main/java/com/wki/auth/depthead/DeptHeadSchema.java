package com.wki.auth.depthead;

import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;

/**
 * Creates the Department Head dashboard tables (dh_risk_alerts, dh_announcements) and
 * inserts sample data on startup. The SQL script is safe to re-run: it only creates what
 * is missing and uses INSERT IGNORE for sample rows.
 */
@Component
@Order(2)
public class DeptHeadSchema implements CommandLineRunner {

    private final DataSource dataSource;

    public DeptHeadSchema(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public void run(String... args) {
        try {
            ResourceDatabasePopulator populator =
                    new ResourceDatabasePopulator(new ClassPathResource("db/dept_head_dashboard.sql"));
            populator.setSqlScriptEncoding("UTF-8");
            populator.execute(dataSource);
            System.out.println("DeptHeadSchema: department head dashboard tables are ready");
        } catch (Exception e) {
            System.err.println("DeptHeadSchema FAILED: " + e.getMessage());
            System.err.println("Run database/dept_head_dashboard.sql by hand in MySQL Workbench and restart.");
        }
    }
}
