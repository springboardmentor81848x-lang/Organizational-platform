package com.skillsphere.service;

import com.skillsphere.dto.LoginRequest;
import com.skillsphere.dto.UserResponse;
import com.skillsphere.entity.AccountStatus;
import com.skillsphere.entity.User;
import com.skillsphere.exception.ApiException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
public class AuthService {

    private static final Duration DEFAULT_IDLE_TIMEOUT = Duration.ofMinutes(30);

    private final AuthenticationManager authenticationManager;
    private final SecurityContextRepository securityContextRepository;
    private final UserService userService;
    private final LoginAttemptService loginAttempts;
    private final long rememberMeDays;
    private final boolean cookieSecure;

    public AuthService(AuthenticationManager authenticationManager,
                       SecurityContextRepository securityContextRepository,
                       UserService userService,
                       LoginAttemptService loginAttempts,
                       @Value("${app.session.remember-me-days}") long rememberMeDays,
                       @Value("${server.servlet.session.cookie.secure:false}") boolean cookieSecure) {
        this.authenticationManager = authenticationManager;
        this.securityContextRepository = securityContextRepository;
        this.userService = userService;
        this.loginAttempts = loginAttempts;
        this.rememberMeDays = rememberMeDays;
        this.cookieSecure = cookieSecure;
    }

    public UserResponse login(LoginRequest req, HttpServletRequest request, HttpServletResponse response) {
        String email = req.email().trim().toLowerCase();
        loginAttempts.assertNotLocked(email);

        Authentication authentication;
        try {
            authentication = authenticationManager.authenticate(
                    UsernamePasswordAuthenticationToken.unauthenticated(email, req.password()));
        } catch (AuthenticationException ex) {
            loginAttempts.recordFailure(email);
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Invalid email or password.");
        }

        User user = userService.getByEmail(email);
        if (user.getAccountStatus() != AccountStatus.ACTIVE) {
            throw new ApiException(HttpStatus.FORBIDDEN,
                    "This account is not active. Contact your administrator.");
        }
        loginAttempts.reset(email);

        // New session id on every login (prevents session fixation).
        HttpSession session = request.getSession(true);
        request.changeSessionId();

        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);
        securityContextRepository.saveContext(context, request, response);

        Duration lifetime = req.rememberMe() ? Duration.ofDays(rememberMeDays) : DEFAULT_IDLE_TIMEOUT;
        session.setMaxInactiveInterval((int) lifetime.toSeconds());

        if (req.rememberMe()) {
            // Persistent cookie so the session survives closing the browser.
            ResponseCookie cookie = ResponseCookie.from("JSESSIONID", request.getSession().getId())
                    .httpOnly(true)
                    .secure(cookieSecure)
                    .sameSite("Lax")
                    .path(request.getContextPath().isEmpty() ? "/" : request.getContextPath())
                    .maxAge(lifetime)
                    .build();
            response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
        }
        return UserResponse.from(user);
    }

    public UserResponse currentUser(Authentication authentication) {
        return UserResponse.from(userService.getByEmail(authentication.getName()));
    }
}
