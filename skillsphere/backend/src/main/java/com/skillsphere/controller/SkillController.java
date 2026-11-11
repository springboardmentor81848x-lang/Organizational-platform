package com.skillsphere.controller;

import com.skillsphere.dto.*;
import com.skillsphere.service.SkillService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/skills")
public class SkillController {

    private final SkillService skillService;

    public SkillController(SkillService skillService) {
        this.skillService = skillService;
    }

    @GetMapping("/categories")
    public ResponseEntity<List<SkillCategoryDto>> getCategories() {
        return ResponseEntity.ok(skillService.getAllCategories());
    }

    @GetMapping
    public ResponseEntity<List<SkillDto>> getSkills() {
        return ResponseEntity.ok(skillService.getAllSkills());
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('HR_SPECIALIST', 'ADMIN')")
    public ResponseEntity<SkillDto> createSkill(@RequestBody SkillDto dto) {
        SkillDto created = skillService.createSkill(dto.categoryId(), dto.name(), dto.description());
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping("/requirements")
    public ResponseEntity<List<RoleRequirementDto>> getRoleRequirements(@RequestParam(required = false) String jobRole) {
        if (jobRole != null && !jobRole.isBlank()) {
            return ResponseEntity.ok(skillService.getRequirementsForRole(jobRole));
        }
        return ResponseEntity.ok(skillService.getAllRoleRequirements());
    }

    @PostMapping("/requirements")
    @PreAuthorize("hasAnyRole('HR_SPECIALIST', 'ADMIN')")
    public ResponseEntity<RoleRequirementDto> setRoleRequirement(@RequestBody RoleRequirementDto req) {
        RoleRequirementDto result = skillService.setRoleRequirement(
                req.jobRole(), req.skillId(), req.requiredProficiency(), req.priority());
        return ResponseEntity.ok(result);
    }

    @GetMapping("/employee")
    public ResponseEntity<List<EmployeeSkillDto>> getMySkills(@AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(skillService.getUserSkills(userDetails.getUsername()));
    }

    @GetMapping("/employee/{userId}")
    public ResponseEntity<List<EmployeeSkillDto>> getEmployeeSkills(@PathVariable Long userId) {
        return ResponseEntity.ok(skillService.getUserSkillsByUserId(userId));
    }

    @PostMapping("/employee")
    public ResponseEntity<EmployeeSkillDto> addOrUpdateSkill(@AuthenticationPrincipal UserDetails userDetails,
                                                            @Valid @RequestBody AddEmployeeSkillRequest req) {
        EmployeeSkillDto dto = skillService.addOrUpdateUserSkill(userDetails.getUsername(), req.skillId(), req.proficiencyLevel());
        return ResponseEntity.ok(dto);
    }

    @GetMapping("/evidence/my")
    public ResponseEntity<List<SkillEvidenceDto>> getMyEvidence(@AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(skillService.getUserEvidence(userDetails.getUsername()));
    }

    @PostMapping("/evidence")
    public ResponseEntity<SkillEvidenceDto> submitEvidence(@AuthenticationPrincipal UserDetails userDetails,
                                                            @Valid @RequestBody SubmitEvidenceRequest req) {
        SkillEvidenceDto dto = skillService.submitEvidence(userDetails.getUsername(), req);
        return ResponseEntity.status(HttpStatus.CREATED).body(dto);
    }

    @GetMapping("/evidence/pending")
    @PreAuthorize("hasAnyRole('MANAGER', 'HR_SPECIALIST', 'ADMIN')")
    public ResponseEntity<List<SkillEvidenceDto>> getPendingEvidence(@AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(skillService.getPendingEvidence(userDetails.getUsername()));
    }

    @PutMapping("/evidence/{evidenceId}/review")
    @PreAuthorize("hasAnyRole('MANAGER', 'HR_SPECIALIST', 'ADMIN')")
    public ResponseEntity<SkillEvidenceDto> reviewEvidence(@AuthenticationPrincipal UserDetails userDetails,
                                                           @PathVariable Long evidenceId,
                                                           @Valid @RequestBody ReviewEvidenceRequest req) {
        SkillEvidenceDto dto = skillService.reviewEvidence(userDetails.getUsername(), evidenceId, req.status());
        return ResponseEntity.ok(dto);
    }
}
