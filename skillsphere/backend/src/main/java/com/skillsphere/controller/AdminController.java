package com.skillsphere.controller;

import com.skillsphere.dto.UpdateRoleRequest;
import com.skillsphere.dto.UpdateStatusRequest;
import com.skillsphere.dto.UserResponse;
import com.skillsphere.service.UserService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** Privileged roles are granted only here, by an authenticated administrator. */
@RestController
@RequestMapping("/api/admin/users")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final UserService userService;

    public AdminController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    public List<UserResponse> list() {
        return userService.listAll();
    }

    @PatchMapping("/{id}/role")
    public UserResponse changeRole(@PathVariable Long id, @Valid @RequestBody UpdateRoleRequest body,
                                   Authentication auth) {
        return userService.changeRole(id, body.role(), auth.getName());
    }

    @PatchMapping("/{id}/status")
    public UserResponse changeStatus(@PathVariable Long id, @Valid @RequestBody UpdateStatusRequest body,
                                     Authentication auth) {
        return userService.changeStatus(id, body.status(), auth.getName());
    }
}
