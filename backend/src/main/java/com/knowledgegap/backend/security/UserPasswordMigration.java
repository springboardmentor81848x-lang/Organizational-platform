package com.knowledgegap.backend.security;

import com.knowledgegap.backend.model.User;
import com.knowledgegap.backend.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class UserPasswordMigration implements CommandLineRunner {

  private final UserRepository userRepository;
  private final PasswordEncoder passwordEncoder;

  public UserPasswordMigration(UserRepository userRepository, PasswordEncoder passwordEncoder) {
    this.userRepository = userRepository;
    this.passwordEncoder = passwordEncoder;
  }

  @Override
  public void run(String... args) {
    for (User user : userRepository.findAll()) {
      boolean changed = false;
      if (user.getPassword() != null && !isBcryptHash(user.getPassword())) {
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        changed = true;
      }
      if (user.getRole() == null || user.getRole().isBlank()) {
        user.setRole("ROLE_EMPLOYEE");
        changed = true;
      }
      if (changed) {
        userRepository.save(user);
      }
    }
  }

  private boolean isBcryptHash(String password) {
    return password.startsWith("$2a$")
        || password.startsWith("$2b$")
        || password.startsWith("$2y$");
  }
}