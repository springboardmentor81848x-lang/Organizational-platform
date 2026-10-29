package com.wki.auth.config;

import com.wki.auth.entity.Role;
import com.wki.auth.repository.RoleRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * On startup, makes sure every role offered on the register form exists in the roles table.
 * This removes the need to run auth_setup.sql by hand (the SQL script still works too).
 */
@Component
public class RoleSeeder implements CommandLineRunner {
    private final RoleRepository roles;

    public RoleSeeder(RoleRepository roles) {
        this.roles = roles;
    }

    @Override
    public void run(String... args) {
        Map<String, String> required = Map.of(
                "EMPLOYEE", "Regular employee",
                "MANAGER", "Manages a team",
                "ADMIN", "Platform administrator",
                "TRAINING_COORDINATOR", "Coordinates training",
                "HR_MANAGER", "Manages HR operations",
                "TRAINER_MENTOR", "Trains and mentors employees");

        required.forEach((name, description) -> {
            if (roles.findByRoleName(name).isEmpty()) {
                roles.save(new Role(name, description));
                System.out.println("RoleSeeder: added missing role " + name);
            }
        });
    }
}
