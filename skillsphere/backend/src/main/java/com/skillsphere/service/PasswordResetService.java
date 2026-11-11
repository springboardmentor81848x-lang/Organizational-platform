package com.skillsphere.service;

import com.skillsphere.dto.ResetPasswordRequest;
import com.skillsphere.entity.AccountStatus;
import com.skillsphere.entity.PasswordResetToken;
import com.skillsphere.entity.User;
import com.skillsphere.exception.ApiException;
import com.skillsphere.repository.PasswordResetTokenRepository;
import com.skillsphere.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Map;

@Service
public class PasswordResetService {

    private final UserRepository userRepository;
    private final PasswordResetTokenRepository tokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final String frontendUrl;
    private final Duration tokenLifetime;
    private final SecureRandom random = new SecureRandom();

    public PasswordResetService(UserRepository userRepository,
                                PasswordResetTokenRepository tokenRepository,
                                PasswordEncoder passwordEncoder,
                                EmailService emailService,
                                @Value("${app.frontend-url}") String frontendUrl,
                                @Value("${app.password-reset.token-minutes}") long tokenMinutes) {
        this.userRepository = userRepository;
        this.tokenRepository = tokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.emailService = emailService;
        this.frontendUrl = frontendUrl;
        this.tokenLifetime = Duration.ofMinutes(tokenMinutes);
    }

    /** Always completes silently so the endpoint cannot be used to discover which e-mails are registered. */
    @Transactional
    public void requestReset(String rawEmail) {
        String email = rawEmail.trim().toLowerCase();
        userRepository.findByEmail(email)
                .filter(u -> u.getAccountStatus() == AccountStatus.ACTIVE)
                .ifPresent(user -> {
                    tokenRepository.deleteByUser(user);
                    byte[] bytes = new byte[32];
                    random.nextBytes(bytes);
                    String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
                    tokenRepository.save(new PasswordResetToken(user, sha256(token), Instant.now().plus(tokenLifetime)));
                    emailService.sendPasswordReset(user.getEmail(), user.getFullName(),
                            frontendUrl + "/reset-password?token=" + token);
                });
    }

    @Transactional
    public void resetPassword(ResetPasswordRequest req) {
        if (!req.password().equals(req.confirmPassword())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Please correct the highlighted fields.",
                    Map.of("confirmPassword", "Passwords do not match."));
        }
        PasswordResetToken token = tokenRepository.findByTokenHash(sha256(req.token()))
                .filter(PasswordResetToken::isUsable)
                .orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST,
                        "This reset link is invalid or has expired. Request a new one."));

        User user = token.getUser();
        user.setPasswordHash(passwordEncoder.encode(req.password()));
        userRepository.save(user);
        token.markUsed();
        tokenRepository.save(token);
    }

    private static String sha256(String value) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(md.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
