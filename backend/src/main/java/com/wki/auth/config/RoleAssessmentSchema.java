package com.wki.auth.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;

/**
 * Creates the role-assessment tables, adds the extra skills / questions / courses, and adds the
 * "target role" column to the employee profile. Runs after EmployeeDashboardSchema. Safe on every startup.
 */
@Component
@Order(3)
public class RoleAssessmentSchema implements CommandLineRunner {
    private final DataSource dataSource;

    public RoleAssessmentSchema(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public void run(String... args) {
        try {
            ResourceDatabasePopulator p = new ResourceDatabasePopulator(new ClassPathResource("db/role_assessment.sql"));
            p.setSqlScriptEncoding("UTF-8");
            p.execute(dataSource);

            JdbcTemplate jdbc = new JdbcTemplate(dataSource);
            Integer c = jdbc.queryForObject(
                    "SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() " +
                    "AND table_name = 'ed_employee_profiles' AND column_name = 'target_role_id'", Integer.class);
            if (c == null || c == 0) {
                jdbc.execute("ALTER TABLE ed_employee_profiles ADD COLUMN target_role_id BIGINT NULL");
                System.out.println("RoleAssessmentSchema: added ed_employee_profiles.target_role_id");
            }
            System.out.println("RoleAssessmentSchema: role assessment tables are ready");
        } catch (Exception e) {
            System.err.println("RoleAssessmentSchema FAILED: " + e.getMessage());
            System.err.println("Run database/role_assessment.sql by hand in MySQL, add column ed_employee_profiles.target_role_id BIGINT NULL, and restart.");
        }
    }
}
