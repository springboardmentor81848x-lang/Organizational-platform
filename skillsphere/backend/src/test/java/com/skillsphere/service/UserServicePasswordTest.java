package com.skillsphere.service;

import com.skillsphere.dto.ChangePasswordRequest;
import com.skillsphere.entity.AccountStatus;
import com.skillsphere.entity.Role;
import com.skillsphere.entity.User;
import com.skillsphere.exception.ApiException;
import com.skillsphere.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class UserServicePasswordTest {

    private UserRepository userRepository;
    private PasswordEncoder passwordEncoder;
    private AdminGovernanceService adminGovernanceService;
    private UserService userService;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        passwordEncoder = mock(PasswordEncoder.class);
        adminGovernanceService = mock(AdminGovernanceService.class);
        userService = new UserService(userRepository, passwordEncoder, adminGovernanceService);
    }

    @Test
    void changePassword_success() {
        User user = new User();
        user.setEmail("employee@skillsphere.com");
        user.setPasswordHash("hashed_old");

        when(userRepository.findByEmail("employee@skillsphere.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("OldPass123!", "hashed_old")).thenReturn(true);
        when(passwordEncoder.matches("NewPass123!", "hashed_old")).thenReturn(false);
        when(passwordEncoder.encode("NewPass123!")).thenReturn("hashed_new");

        ChangePasswordRequest req = new ChangePasswordRequest("OldPass123!", "NewPass123!", "NewPass123!");
        assertDoesNotThrow(() -> userService.changePassword("employee@skillsphere.com", req));

        assertEquals("hashed_new", user.getPasswordHash());
        verify(userRepository).save(user);
    }

    @Test
    void changePassword_mismatchedConfirmation_throwsBadRequest() {
        ChangePasswordRequest req = new ChangePasswordRequest("OldPass123!", "NewPass123!", "Different123!");
        ApiException ex = assertThrows(ApiException.class, () -> userService.changePassword("employee@skillsphere.com", req));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
        assertTrue(ex.getMessage().contains("match"));
    }

    @Test
    void changePassword_wrongCurrentPassword_throwsBadRequest() {
        User user = new User();
        user.setEmail("employee@skillsphere.com");
        user.setPasswordHash("hashed_old");

        when(userRepository.findByEmail("employee@skillsphere.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("WrongOld!", "hashed_old")).thenReturn(false);

        ChangePasswordRequest req = new ChangePasswordRequest("WrongOld!", "NewPass123!", "NewPass123!");
        ApiException ex = assertThrows(ApiException.class, () -> userService.changePassword("employee@skillsphere.com", req));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
        assertTrue(ex.getMessage().contains("Current password is incorrect"));
    }

    @Test
    void changePassword_samePasswordAsCurrent_throwsBadRequest() {
        User user = new User();
        user.setEmail("employee@skillsphere.com");
        user.setPasswordHash("hashed_old");

        when(userRepository.findByEmail("employee@skillsphere.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("OldPass123!", "hashed_old")).thenReturn(true);
        when(passwordEncoder.matches("OldPass123!", "hashed_old")).thenReturn(true);

        ChangePasswordRequest req = new ChangePasswordRequest("OldPass123!", "OldPass123!", "OldPass123!");
        ApiException ex = assertThrows(ApiException.class, () -> userService.changePassword("employee@skillsphere.com", req));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
        assertTrue(ex.getMessage().contains("cannot be the same"));
    }
}
