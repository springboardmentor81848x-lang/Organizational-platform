package com.skillsphere.config;

import com.skillsphere.entity.AccountStatus;
import com.skillsphere.entity.Role;
import com.skillsphere.entity.User;
import com.skillsphere.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Creates the first administrator from ADMIN_EMAIL / ADMIN_PASSWORD environment variables,
 * only when no Admin exists yet. This is the secure backend path for obtaining the very
 * first privileged account; later promotions go through /api/admin/users/{id}/role.
 */
@Component
public class AdminBootstrap implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminBootstrap.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final String email;
    private final String password;
    private final String fullName;

    public AdminBootstrap(UserRepository userRepository, PasswordEncoder passwordEncoder,
                          @Value("${app.bootstrap-admin.email:}") String email,
                          @Value("${app.bootstrap-admin.password:}") String password,
                          @Value("${app.bootstrap-admin.full-name}") String fullName) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.email = email;
        this.password = password;
        this.fullName = fullName;
    }

    @Override
    public void run(String... args) {
        if (email.isBlank() || password.isBlank()) {
            log.info("Admin bootstrap skipped (ADMIN_EMAIL / ADMIN_PASSWORD not set).");
            return;
        }
        if (userRepository.existsByRole(Role.ADMIN)) {
            return;
        }
        String normalized = email.trim().toLowerCase();
        if (userRepository.existsByEmail(normalized)) {
            log.warn("Admin bootstrap skipped: {} already exists as a non-admin user.", normalized);
            return;
        }
        User admin = new User();
        admin.setFullName(fullName);
        admin.setEmail(normalized);
        admin.setPasswordHash(passwordEncoder.encode(password));
        admin.setRole(Role.ADMIN);
        admin.setAccountStatus(AccountStatus.ACTIVE);
        userRepository.save(admin);
        log.info("Bootstrap administrator created: {}", normalized);
    }
}
