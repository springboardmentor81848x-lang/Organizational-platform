package com.skillsphere.service;

import com.skillsphere.dto.*;
import com.skillsphere.entity.*;
import com.skillsphere.exception.ApiException;
import com.skillsphere.repository.*;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class SkillService {

    public static final Map<String, Integer> PROFICIENCY_TO_NUMERIC = Map.of(
            "Unaware", 1,
            "Beginner", 2,
            "Intermediate", 3,
            "Advanced", 4,
            "Expert", 5
    );

    public static final Map<Integer, String> NUMERIC_TO_PROFICIENCY = Map.of(
            1, "Unaware",
            2, "Beginner",
            3, "Intermediate",
            4, "Advanced",
            5, "Expert"
    );

    private final SkillCategoryRepository categoryRepository;
    private final SkillRepository skillRepository;
    private final RoleSkillRequirementRepository requirementRepository;
    private final EmployeeSkillRepository employeeSkillRepository;
    private final SkillEvidenceRepository evidenceRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;
    private final AssessmentService assessmentService;
    private final TeamMembershipRepository teamMembershipRepository;

    public SkillService(SkillCategoryRepository categoryRepository,
                        SkillRepository skillRepository,
                        RoleSkillRequirementRepository requirementRepository,
                        EmployeeSkillRepository employeeSkillRepository,
                        SkillEvidenceRepository evidenceRepository,
                        UserRepository userRepository,
                        NotificationService notificationService,
                        @Lazy AssessmentService assessmentService,
                        TeamMembershipRepository teamMembershipRepository) {
        this.categoryRepository = categoryRepository;
        this.skillRepository = skillRepository;
        this.requirementRepository = requirementRepository;
        this.employeeSkillRepository = employeeSkillRepository;
        this.evidenceRepository = evidenceRepository;
        this.userRepository = userRepository;
        this.notificationService = notificationService;
        this.assessmentService = assessmentService;
        this.teamMembershipRepository = teamMembershipRepository;
    }

    @Transactional(readOnly = true)
    public List<SkillCategoryDto> getAllCategories() {
        return categoryRepository.findAll().stream()
                .map(c -> new SkillCategoryDto(c.getId(), c.getName(), c.getDescription()))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<SkillDto> getAllSkills() {
        return skillRepository.findAll().stream()
                .map(s -> new SkillDto(s.getId(), s.getCategory().getId(), s.getCategory().getName(), s.getName(), s.getDescription()))
                .toList();
    }

    @Transactional
    public SkillDto createSkill(Long categoryId, String name, String description) {
        SkillCategory category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Category not found."));

        if (skillRepository.existsByNameIgnoreCase(name)) {
            throw new ApiException(HttpStatus.CONFLICT, "A skill with this name already exists.");
        }

        Skill skill = new Skill(category, name.trim(), description);
        Skill saved = skillRepository.save(skill);
        return new SkillDto(saved.getId(), category.getId(), category.getName(), saved.getName(), saved.getDescription());
    }

    @Transactional(readOnly = true)
    public List<RoleRequirementDto> getRequirementsForRole(String jobRole) {
        return requirementRepository.findByJobRole(jobRole).stream().map(r -> new RoleRequirementDto(
                r.getId(),
                r.getJobRole(),
                r.getSkill().getId(),
                r.getSkill().getName(),
                r.getSkill().getCategory().getName(),
                r.getRequiredProficiency(),
                PROFICIENCY_TO_NUMERIC.getOrDefault(r.getRequiredProficiency(), 3),
                r.getPriority()
        )).toList();
    }

    @Transactional(readOnly = true)
    public List<RoleRequirementDto> getAllRoleRequirements() {
        return requirementRepository.findAll().stream().map(r -> new RoleRequirementDto(
                r.getId(),
                r.getJobRole(),
                r.getSkill().getId(),
                r.getSkill().getName(),
                r.getSkill().getCategory().getName(),
                r.getRequiredProficiency(),
                PROFICIENCY_TO_NUMERIC.getOrDefault(r.getRequiredProficiency(), 3),
                r.getPriority()
        )).toList();
    }

    @Transactional
    public RoleRequirementDto setRoleRequirement(String jobRole, Long skillId, String proficiency, String priority) {
        Skill skill = skillRepository.findById(skillId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Skill not found."));

        RoleSkillRequirement req = requirementRepository.findByJobRoleAndSkillId(jobRole, skillId)
                .orElseGet(() -> new RoleSkillRequirement(jobRole, skill, proficiency, priority));

        req.setRequiredProficiency(proficiency);
        req.setPriority(priority);
        RoleSkillRequirement saved = requirementRepository.save(req);

        return new RoleRequirementDto(
                saved.getId(),
                saved.getJobRole(),
                skill.getId(),
                skill.getName(),
                skill.getCategory().getName(),
                saved.getRequiredProficiency(),
                PROFICIENCY_TO_NUMERIC.getOrDefault(saved.getRequiredProficiency(), 3),
                saved.getPriority()
        );
    }

    @Transactional(readOnly = true)
    public List<EmployeeSkillDto> getUserSkills(String email) {
        User user = getUserByEmail(email);
        return employeeSkillRepository.findByUserId(user.getId()).stream()
                .map(this::toEmployeeSkillDto).toList();
    }

    @Transactional(readOnly = true)
    public List<EmployeeSkillDto> getUserSkillsByUserId(Long userId) {
        return employeeSkillRepository.findByUserId(userId).stream()
                .map(this::toEmployeeSkillDto).toList();
    }

    @Transactional
    public EmployeeSkillDto addOrUpdateUserSkill(String email, Long skillId, String proficiencyLevel) {
        User user = getUserByEmail(email);
        Skill skill = skillRepository.findById(skillId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Skill not found."));

        EmployeeSkill es = employeeSkillRepository.findByUserIdAndSkillId(user.getId(), skillId)
                .orElseGet(() -> new EmployeeSkill(user, skill, proficiencyLevel));

        es.setProficiencyLevel(proficiencyLevel);
        es.setUpdatedAt(Instant.now());
        EmployeeSkill saved = employeeSkillRepository.save(es);

        // Automatically trigger gap recalculation so skill gaps update dynamically in PostgreSQL
        try {
            assessmentService.recalculateSkillGapsForUser(user);
        } catch (Exception ignored) {}

        return toEmployeeSkillDto(saved);
    }

    @Transactional
    public SkillEvidenceDto submitEvidence(String email, SubmitEvidenceRequest req) {
        User user = getUserByEmail(email);
        EmployeeSkill es = employeeSkillRepository.findById(req.employeeSkillId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Employee skill record not found."));

        if (!es.getUser().getId().equals(user.getId())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "You can only submit evidence for your own skills.");
        }

        SkillEvidence ev = new SkillEvidence(es, req.title().trim(), req.description(), req.evidenceUrl());
        SkillEvidence saved = evidenceRepository.save(ev);

        return new SkillEvidenceDto(
                saved.getId(),
                es.getId(),
                es.getSkill().getName(),
                saved.getTitle(),
                saved.getDescription(),
                saved.getEvidenceUrl(),
                saved.getStatus(),
                null,
                saved.getCreatedAt()
        );
    }

    @Transactional(readOnly = true)
    public List<SkillEvidenceDto> getUserEvidence(String email) {
        User user = getUserByEmail(email);
        return evidenceRepository.findByUserId(user.getId()).stream()
                .map(ev -> new SkillEvidenceDto(
                        ev.getId(),
                        ev.getEmployeeSkill().getId(),
                        ev.getEmployeeSkill().getSkill().getName(),
                        ev.getTitle(),
                        ev.getDescription(),
                        ev.getEvidenceUrl(),
                        ev.getStatus(),
                        ev.getReviewedBy() != null ? ev.getReviewedBy().getFullName() : null,
                        ev.getCreatedAt()
                )).toList();
    }

    @Transactional(readOnly = true)
    public List<SkillEvidenceDto> getPendingEvidence(String callerEmail) {
        User caller = getUserByEmail(callerEmail);
        List<SkillEvidence> list = evidenceRepository.findByStatus("PENDING");
        if (caller.getRole() == Role.MANAGER) {
            Set<Long> managedUserIds = teamMembershipRepository.findByManagerUserId(caller.getId()).stream()
                    .map(m -> m.getUser().getId())
                    .collect(Collectors.toSet());
            list = list.stream()
                    .filter(ev -> managedUserIds.contains(ev.getEmployeeSkill().getUser().getId()))
                    .toList();
        }
        return list.stream().map(this::toSkillEvidenceDto).toList();
    }

    @Transactional(readOnly = true)
    public List<SkillEvidenceDto> getPendingEvidence() {
        return evidenceRepository.findByStatus("PENDING").stream()
                .map(this::toSkillEvidenceDto)
                .toList();
    }

    @Transactional
    public SkillEvidenceDto reviewEvidence(String reviewerEmail, Long evidenceId, String status) {
        User reviewer = getUserByEmail(reviewerEmail);
        SkillEvidence ev = evidenceRepository.findById(evidenceId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Evidence not found."));

        if (!"PENDING".equalsIgnoreCase(ev.getStatus())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Evidence has already been reviewed (current status: " + ev.getStatus() + ").");
        }

        if (!"APPROVED".equalsIgnoreCase(status) && !"REJECTED".equalsIgnoreCase(status)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Invalid review status. Must be APPROVED or REJECTED.");
        }

        if (reviewer.getRole() == Role.MANAGER) {
            Long employeeUserId = ev.getEmployeeSkill().getUser().getId();
            boolean belongs = teamMembershipRepository.findByManagerUserId(reviewer.getId()).stream()
                    .anyMatch(m -> m.getUser().getId().equals(employeeUserId));
            if (!belongs) {
                throw new ApiException(HttpStatus.FORBIDDEN, "Access denied. You can only review evidence for employees in your managed teams.");
            }
        }

        String canonicalStatus = status.trim().toUpperCase();
        ev.setStatus(canonicalStatus);
        ev.setReviewedBy(reviewer);

        if ("APPROVED".equals(canonicalStatus)) {
            EmployeeSkill es = ev.getEmployeeSkill();
            es.setVerified(true);
            es.setVerifiedBy(reviewer);
            employeeSkillRepository.save(es);

            try {
                assessmentService.recalculateSkillGapsForUser(es.getUser());
            } catch (Exception ignored) {}
        } else if ("REJECTED".equals(canonicalStatus)) {
            EmployeeSkill es = ev.getEmployeeSkill();
            es.setVerified(false);
            employeeSkillRepository.save(es);
        }

        SkillEvidence saved = evidenceRepository.save(ev);
        notificationService.createNotification(
                ev.getEmployeeSkill().getUser(),
                "SKILL_VERIFICATION",
                "Skill Evidence " + canonicalStatus,
                "Your evidence for " + ev.getEmployeeSkill().getSkill().getName() + " was reviewed: " + canonicalStatus,
                "/dashboard/employee"
        );

        return toSkillEvidenceDto(saved);
    }

    private SkillEvidenceDto toSkillEvidenceDto(SkillEvidence ev) {
        String employeeName = (ev.getEmployeeSkill() != null && ev.getEmployeeSkill().getUser() != null)
                ? ev.getEmployeeSkill().getUser().getFullName() : null;
        String reviewerName = ev.getReviewedBy() != null ? ev.getReviewedBy().getFullName() : null;
        return new SkillEvidenceDto(
                ev.getId(),
                ev.getEmployeeSkill().getId(),
                ev.getEmployeeSkill().getSkill().getName(),
                ev.getTitle(),
                ev.getDescription(),
                ev.getEvidenceUrl(),
                ev.getStatus(),
                reviewerName,
                ev.getCreatedAt(),
                employeeName
        );
    }

    private User getUserByEmail(String email) {
        return userRepository.findByEmail(email.trim().toLowerCase())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "User not found."));
    }

    private EmployeeSkillDto toEmployeeSkillDto(EmployeeSkill es) {
        return new EmployeeSkillDto(
                es.getId(),
                es.getUser().getId(),
                es.getUser().getFullName(),
                es.getSkill().getId(),
                es.getSkill().getName(),
                es.getSkill().getCategory().getName(),
                es.getProficiencyLevel(),
                PROFICIENCY_TO_NUMERIC.getOrDefault(es.getProficiencyLevel(), 1),
                es.isVerified(),
                es.getVerifiedBy() != null ? es.getVerifiedBy().getFullName() : null,
                es.getUpdatedAt()
        );
    }
}
