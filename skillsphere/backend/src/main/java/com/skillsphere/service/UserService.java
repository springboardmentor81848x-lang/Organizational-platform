package com.skillsphere.service;

import com.skillsphere.dto.ChangePasswordRequest;
import com.skillsphere.dto.RegisterRequest;
import com.skillsphere.dto.RegisterResponse;
import com.skillsphere.dto.UserResponse;
import com.skillsphere.entity.AccountStatus;
import com.skillsphere.entity.Role;
import com.skillsphere.entity.User;
import com.skillsphere.exception.ApiException;
import com.skillsphere.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AdminGovernanceService adminGovernanceService;

    @Autowired
    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder,
                       AdminGovernanceService adminGovernanceService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.adminGovernanceService = java.util.Objects.requireNonNull(adminGovernanceService,
                "AdminGovernanceService is required for secure administrator audit logging");
    }

    /**
     * Public registration. The role is ALWAYS Employee. A different role in the
     * request is stored only as "requested_role" for an administrator to review.
     */
    @Transactional
    public RegisterResponse register(RegisterRequest req) {
        String email = req.email().trim().toLowerCase();
        String employeeId = (req.employeeId() == null || req.employeeId().isBlank())
                ? null : req.employeeId().trim();

        Map<String, String> errors = new LinkedHashMap<>();
        if (!req.password().equals(req.confirmPassword())) {
            errors.put("confirmPassword", "Passwords do not match.");
        }
        if (!errors.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Please correct the highlighted fields.", errors);
        }

        if (userRepository.existsByEmail(email)) {
            throw new ApiException(HttpStatus.CONFLICT, "An account with this email already exists.",
                    Map.of("email", "An account with this email already exists."));
        }
        if (employeeId != null && userRepository.existsByEmployeeId(employeeId)) {
            throw new ApiException(HttpStatus.CONFLICT, "This Employee ID is already registered.",
                    Map.of("employeeId", "This Employee ID is already registered."));
        }

        Role requested = (req.role() == null || req.role().isBlank())
                ? Role.EMPLOYEE : Role.valueOf(req.role().trim().toUpperCase());

        User user = new User();
        user.setFullName(req.fullName().trim());
        user.setEmail(email);
        user.setEmployeeId(employeeId);
        user.setPasswordHash(passwordEncoder.encode(req.password()));
        user.setRole(Role.EMPLOYEE);                       // never trust client-supplied privileges
        user.setRequestedRole(requested == Role.EMPLOYEE ? null : requested);
        user.setAccountStatus(AccountStatus.ACTIVE);
        user = userRepository.saveAndFlush(user);

        String message = requested == Role.EMPLOYEE
                ? "Account created. You can sign in now."
                : "Account created as Employee. Your request for " + requested.getLabel()
                  + " access has been sent to an administrator for approval.";
        return new RegisterResponse(message, UserResponse.from(user));
    }

    @Transactional(readOnly = true)
    public User getByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "User not found."));
    }

    @Transactional
    public void changePassword(String email, ChangePasswordRequest req) {
        if (!req.newPassword().equals(req.confirmPassword())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "New password and confirmation do not match.",
                    Map.of("confirmPassword", "Passwords do not match."));
        }

        User user = userRepository.findByEmail(email.trim().toLowerCase())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "User not found."));

        if (!passwordEncoder.matches(req.currentPassword(), user.getPasswordHash())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Current password is incorrect.",
                    Map.of("currentPassword", "Current password is incorrect."));
        }

        if (passwordEncoder.matches(req.newPassword(), user.getPasswordHash())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "New password cannot be the same as your current password.",
                    Map.of("newPassword", "New password cannot be the same as current password."));
        }

        user.setPasswordHash(passwordEncoder.encode(req.newPassword()));
        userRepository.save(user);
    }

    // ---------------- administration ----------------

    @Transactional(readOnly = true)
    public List<UserResponse> listAll() {
        return userRepository.findAll().stream()
                .sorted((a, b) -> a.getCreatedAt().compareTo(b.getCreatedAt()))
                .map(UserResponse::from)
                .toList();
    }

    @Transactional
    public UserResponse changeRole(Long id, Role newRole, String actingAdminEmail) {
        User target = find(id);
        if (target.getEmail().equalsIgnoreCase(actingAdminEmail)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "You cannot change your own role.");
        }
        if (target.getRole() == Role.ADMIN && newRole != Role.ADMIN) {
            long activeAdminCount = userRepository.findAll().stream()
                    .filter(u -> u.getRole() == Role.ADMIN && u.getAccountStatus() == AccountStatus.ACTIVE)
                    .count();
            if (activeAdminCount <= 1) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "Cannot demote the last remaining active platform administrator.");
            }
        }
        Role oldRole = target.getRole();
        target.setRole(newRole);
        if (target.getRequestedRole() == newRole) {
            target.setRequestedRole(null);                 // request fulfilled
        }
        target = userRepository.save(target);

        User actingAdmin = userRepository.findByEmail(actingAdminEmail.trim().toLowerCase()).orElse(null);
        adminGovernanceService.recordAuditLog(
                actingAdmin,
                "CHANGE_ROLE",
                "User",
                target.getId().toString(),
                "Changed role of " + target.getEmail() + " from " + oldRole + " to " + newRole,
                "127.0.0.1"
        );
        return UserResponse.from(target);
    }

    @Transactional
    public UserResponse changeStatus(Long id, AccountStatus status, String actingAdminEmail) {
        User target = find(id);
        if (target.getEmail().equalsIgnoreCase(actingAdminEmail)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "You cannot change your own account status.");
        }
        if (status != AccountStatus.ACTIVE && target.getRole() == Role.ADMIN && target.getAccountStatus() == AccountStatus.ACTIVE) {
            long activeAdminCount = userRepository.findAll().stream()
                    .filter(u -> u.getRole() == Role.ADMIN && u.getAccountStatus() == AccountStatus.ACTIVE)
                    .count();
            if (activeAdminCount <= 1) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "Cannot deactivate the last remaining active platform administrator.");
            }
        }
        AccountStatus oldStatus = target.getAccountStatus();
        target.setAccountStatus(status);
        target = userRepository.save(target);

        User actingAdmin = userRepository.findByEmail(actingAdminEmail.trim().toLowerCase()).orElse(null);
        adminGovernanceService.recordAuditLog(
                actingAdmin,
                "CHANGE_STATUS",
                "User",
                target.getId().toString(),
                "Changed account status of " + target.getEmail() + " from " + oldStatus + " to " + status,
                "127.0.0.1"
        );
        return UserResponse.from(target);
    }

    private User find(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "User not found."));
    }
}
