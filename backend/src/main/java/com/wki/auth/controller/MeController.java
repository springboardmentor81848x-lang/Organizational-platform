package com.wki.auth.controller;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/** Protected endpoints used by the dashboards to verify the JWT. */
@RestController
@RequestMapping("/api")
public class MeController {

    @GetMapping("/me")
    public Map<String, Object> me(Authentication auth) {
        String role = auth.getAuthorities().iterator().next().getAuthority().replace("ROLE_", "");
        return Map.of("email", auth.getName(), "role", role);
    }

    /** Only reachable with an ADMIN token (see SecurityConfig). */
    @GetMapping("/admin/ping")
    public Map<String, String> adminPing() {
        return Map.of("message", "Hello, admin");
    }
}
