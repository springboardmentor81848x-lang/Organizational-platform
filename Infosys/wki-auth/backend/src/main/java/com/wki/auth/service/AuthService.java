package com.wki.auth.service;

import com.wki.auth.dto.LoginRequest;
import com.wki.auth.dto.RegisterRequest;
import com.wki.auth.entity.Employee;
import com.wki.auth.entity.Role;
import com.wki.auth.repository.EmployeeRepository;
import com.wki.auth.repository.RoleRepository;
import com.wki.auth.security.JwtUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Set;

@Service
public class AuthService {
    private static final Set<String> PRIVILEGED = Set.of("ADMIN", "HR_MANAGER");
    private static final String DUMMY_HASH =
            "$2a$10$7EqJtq98hPqEX7fNZaFWoOhi5BtHy1Q2k3n5Y7q6Z0rXo5wq0nYhe";

    private final EmployeeRepository employees;
    private final RoleRepository roles;
    private final PasswordEncoder encoder;
    private final JwtUtil jwt;
    private final boolean allowPrivilegedSignup;

    public AuthService(EmployeeRepository employees, RoleRepository roles, PasswordEncoder encoder,
                       JwtUtil jwt,
                       @Value("${app.allow-privileged-self-signup}") boolean allowPrivilegedSignup) {
        this.employees = employees;
        this.roles = roles;
        this.encoder = encoder;
        this.jwt = jwt;
        this.allowPrivilegedSignup = allowPrivilegedSignup;
    }

    public Map<String, Object> register(RegisterRequest r) {
        if (!r.getPassword().equals(r.getConfirmPassword()))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Passwords do not match");

        String roleName = r.getRole().trim().toUpperCase();
        if (!allowPrivilegedSignup && PRIVILEGED.contains(roleName))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "This role cannot be self-selected. Ask an administrator.");

        String email = r.getEmail().trim().toLowerCase();
        if (employees.existsByEmail(email))
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already registered");

        Role role = roles.findByRoleName(roleName)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid role"));

        String[] parts = r.getFullName().trim().split("\\s+", 2);
        Employee e = new Employee();
        e.setFirstName(parts[0]);
        e.setLastName(parts.length > 1 ? parts[1] : "-");
        e.setEmail(email);
        e.setPasswordHash(encoder.encode(r.getPassword()));
        e.setRoleId(role.getRoleId());
        e.setDepartmentId(1L);   // default: Engineering
        e.setDesignationId(1L);  // default: Software Engineer
        employees.save(e);

        return Map.of("message", "Registration successful");
    }

    public Map<String, Object> login(LoginRequest r) {
        String email = r.getEmail().trim().toLowerCase();
        Employee e = employees.findByEmail(email).orElse(null);

        // Always run a BCrypt comparison so response time doesn't reveal whether the email exists.
        String hash = (e != null && e.getPasswordHash() != null) ? e.getPasswordHash() : DUMMY_HASH;
        boolean ok;
        try {
            ok = encoder.matches(r.getPassword(), hash);
        } catch (IllegalArgumentException ex) {
            ok = false; // e.g. seed rows holding DEMO_PLACEHOLDER
        }
        if (e == null || !ok)
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid email or password");
        if (!"ACTIVE".equals(e.getStatus()))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Account is not active");

        // Record this successful login in the database (employees.last_login_at)
        employees.updateLastLogin(e.getEmployeeId(), LocalDateTime.now());

        String roleName = roles.findById(e.getRoleId()).map(Role::getRoleName).orElse("EMPLOYEE");
        String token = jwt.generate(e.getEmployeeId(), e.getEmail(), roleName);

        return Map.of(
                "message", "Login successful",
                "token", token,
                "employeeId", e.getEmployeeId(),
                "fullName", e.getFirstName() + " " + e.getLastName(),
                "email", e.getEmail(),
                "role", roleName);
    }
}
