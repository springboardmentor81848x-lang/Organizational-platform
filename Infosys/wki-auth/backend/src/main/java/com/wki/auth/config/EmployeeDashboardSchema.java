package com.wki.auth.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;

/**
 * Creates the employee-dashboard tables (all named ed_...) and loads the sample skills, courses
 * and learning paths on startup, so there is no SQL to run by hand.
 * The script (resources/db/employee_dashboard.sql) is safe to run on every startup: it only
 * creates what is missing. It never touches your existing tables.
 */
@Component
@Order(1)
public class EmployeeDashboardSchema implements CommandLineRunner {
    private final DataSource dataSource;

    public EmployeeDashboardSchema(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public void run(String... args) {
        try {
            ResourceDatabasePopulator populator =
                    new ResourceDatabasePopulator(new ClassPathResource("db/employee_dashboard.sql"));
            populator.setSqlScriptEncoding("UTF-8");
            populator.execute(dataSource);

            // Add profile columns that Spring's SQL runner can't handle (needs DELIMITER for procedures)
            JdbcTemplate jdbc = new JdbcTemplate(dataSource);
            addColumnIfMissing(jdbc, "ed_employee_profiles", "education", "VARCHAR(500) AFTER linkedin");
            addColumnIfMissing(jdbc, "ed_employee_profiles", "work_experience", "VARCHAR(1000) AFTER education");
            addColumnIfMissing(jdbc, "ed_employee_profiles", "department", "VARCHAR(100) AFTER work_experience");
            addColumnIfMissing(jdbc, "ed_employee_profiles", "job_role", "VARCHAR(100) AFTER department");

            System.out.println("EmployeeDashboardSchema: employee dashboard tables are ready");
        } catch (Exception e) {
            // Do not stop the whole app (login still works); the dashboard will show errors until this is fixed.
            System.err.println("EmployeeDashboardSchema FAILED: " + e.getMessage());
            System.err.println("Run database/employee_dashboard.sql by hand in MySQL Workbench and restart.");
        }
    }

    private void addColumnIfMissing(JdbcTemplate jdbc, String table, String column, String definition) {
        Integer count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = ? AND column_name = ?",
                Integer.class, table, column);
        if (count == null || count == 0) {
            jdbc.execute("ALTER TABLE " + table + " ADD COLUMN " + column + " " + definition);
            System.out.println("EmployeeDashboardSchema: added column " + table + "." + column);
        }
    }
}
