package com.skillsphere.controller;

import com.skillsphere.dto.EmployeeProfileDto;
import com.skillsphere.dto.UpdateProfileRequest;
import com.skillsphere.service.EmployeeProfileService;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/employee/profile")
public class EmployeeProfileController {

    private final EmployeeProfileService profileService;

    public EmployeeProfileController(EmployeeProfileService profileService) {
        this.profileService = profileService;
    }

    @GetMapping
    public EmployeeProfileDto getProfile(Authentication auth) {
        return profileService.getProfileByEmail(auth.getName());
    }

    @PutMapping
    public EmployeeProfileDto updateProfile(@Valid @RequestBody UpdateProfileRequest request, Authentication auth) {
        return profileService.updateProfile(auth.getName(), request);
    }
}
