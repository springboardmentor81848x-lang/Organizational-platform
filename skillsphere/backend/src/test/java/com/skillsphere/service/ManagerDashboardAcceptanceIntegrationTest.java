package com.skillsphere.service;

import com.skillsphere.dto.*;
import com.skillsphere.entity.*;
import com.skillsphere.exception.ApiException;
import com.skillsphere.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
public class ManagerDashboardAcceptanceIntegrationTest {

    @Autowired
    private UserRepository userRepository;
    @Autowired
    private EmployeeProfileRepository profileRepository;
    @Autowired
    private DepartmentRepository departmentRepository;
    @Autowired
    private TeamRepository teamRepository;
    @Autowired
    private TeamMembershipRepository membershipRepository;
    @Autowired
    private SkillCategoryRepository categoryRepository;
    @Autowired
    private SkillRepository skillRepository;
    @Autowired
    private RoleSkillRequirementRepository requirementRepository;
    @Autowired
    private EmployeeSkillRepository employeeSkillRepository;
    @Autowired
    private SkillGapRepository skillGapRepository;
    @Autowired
    private CourseRepository courseRepository;
    @Autowired
    private SkillEvidenceRepository skillEvidenceRepository;
    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private ManagerService managerService;
    @Autowired
    private SkillService skillService;

    private User managerA;
    private User managerB;
    private User employeeA;
    private User employeeB;
    private User mentorUser;
    private Department department;
    private Team teamA;
    private Team teamB;
    private SkillCategory category;
    private Skill skillJava;
    private Course courseJava;

    private User createTestUser(String email, String name, Role role) {
        User u = new User();
        u.setEmail(email.toLowerCase());
        u.setPasswordHash("$2a$10$abcdefghijklmnopqrstuvwxyzABCDEF");
        u.setFullName(name);
        u.setRole(role);
        u.setEmployeeId("EMP-" + UUID.randomUUID().toString().substring(0, 7).toUpperCase());
        return userRepository.save(u);
    }

    @BeforeEach
    void setUp() {
        department = departmentRepository.save(new Department("Engineering Division", "ENG-DIV", "Main Eng Dept", null));

        managerA = createTestUser("mgr_a_" + UUID.randomUUID() + "@skillsphere.com", "Manager Alpha", Role.MANAGER);
        managerB = createTestUser("mgr_b_" + UUID.randomUUID() + "@skillsphere.com", "Manager Beta", Role.MANAGER);
        employeeA = createTestUser("emp_a_" + UUID.randomUUID() + "@skillsphere.com", "Employee Alpha", Role.EMPLOYEE);
        employeeB = createTestUser("emp_b_" + UUID.randomUUID() + "@skillsphere.com", "Employee Beta", Role.EMPLOYEE);
        mentorUser = createTestUser("mentor_" + UUID.randomUUID() + "@skillsphere.com", "Mentor Senior", Role.MENTOR);

        teamA = teamRepository.save(new Team(department, "Squad Alpha", managerA));
        teamB = teamRepository.save(new Team(department, "Squad Beta", managerB));

        membershipRepository.save(new TeamMembership(teamA, employeeA));
        membershipRepository.save(new TeamMembership(teamB, employeeB));

        EmployeeProfile profA = profileRepository.findByUserId(employeeA.getId()).orElseGet(() -> new EmployeeProfile(employeeA));
        profA.setJobRole("Software Developer");
        profA.setDepartment("Engineering Division");
        profA.setExperienceLevel("Intermediate");
        profileRepository.save(profA);

        EmployeeProfile profB = profileRepository.findByUserId(employeeB.getId()).orElseGet(() -> new EmployeeProfile(employeeB));
        profB.setJobRole("QA Engineer");
        profB.setDepartment("Engineering Division");
        profB.setExperienceLevel("Entry level");
        profileRepository.save(profB);

        category = categoryRepository.save(new SkillCategory("Backend Development", "Backend core skills"));
        skillJava = skillRepository.save(new Skill(category, "Java 21 Enterprise", "Core Java and Spring Boot"));

        courseJava = courseRepository.save(new Course(
                "Advanced Java 21",
                "In-depth modern Java architecture",
                skillJava,
                "Advanced",
                20,
                "Internal L&D",
                "https://learning.skillsphere.com/java21"
        ));
    }

    @Test
    @DisplayName("Module 1 & 2: Team Scoping, Access Control, and Employee Isolation")
    void testTeamScopingAndAccessControl() {
        // 1. Manager A can view Team A overview
        ManagerDashboardOverviewDto ovA = managerService.getDashboardOverview(managerA.getEmail(), teamA.getId());
        assertNotNull(ovA);
        assertEquals(1, ovA.totalTeams());
        assertEquals(1, ovA.totalEmployees());

        // 2. Manager A cannot view Team B overview (throws 403 FORBIDDEN)
        ApiException exTeam = assertThrows(ApiException.class, () ->
                managerService.getDashboardOverview(managerA.getEmail(), teamB.getId())
        );
        assertEquals(HttpStatus.FORBIDDEN, exTeam.getStatus());

        // 3. Manager A can view Employee A detail
        ManagerEmployeeDetailDto detailA = managerService.getEmployeeDetail(managerA.getEmail(), employeeA.getId());
        assertNotNull(detailA);
        assertEquals(employeeA.getFullName(), detailA.fullName());

        // 4. Manager A cannot view Employee B detail (throws 403 FORBIDDEN)
        ApiException exEmp = assertThrows(ApiException.class, () ->
                managerService.getEmployeeDetail(managerA.getEmail(), employeeB.getId())
        );
        assertEquals(HttpStatus.FORBIDDEN, exEmp.getStatus());

        // 5. Team members list only contains members from managed teams
        List<ManagerTeamMemberDto> members = managerService.getTeamMembers(managerA.getEmail(), null, null);
        assertEquals(1, members.size());
        assertEquals(employeeA.getEmail(), members.get(0).email());
    }

    @Test
    @DisplayName("Module 3, 4 & 5: Team Skill Inventory, Gap Analysis, and Heatmap Generation")
    void testSkillInventoryGapAnalysisAndHeatmap() {
        // Setup Role requirement for Software Developer: Java target level Advanced, HIGH priority
        requirementRepository.save(new RoleSkillRequirement("Software Developer", skillJava, "Advanced", "HIGH"));

        // Employee A has verified proficiency Intermediate (level 3) in Java
        EmployeeSkill es = new EmployeeSkill(employeeA, skillJava, "Intermediate");
        es.setVerified(true);
        employeeSkillRepository.save(es);

        // Gap is target (4: Advanced) - current (3: Intermediate) = 1 (HIGH priority)
        SkillGap gap = new SkillGap(employeeA, skillJava, 3, 4, 1, "HIGH");
        skillGapRepository.save(gap);

        // 1. Skill Inventory
        List<TeamSkillInventoryDto> inventory = managerService.getSkillInventory(managerA.getEmail(), teamA.getId(), null, null);
        assertFalse(inventory.isEmpty());
        TeamSkillInventoryDto invItem = inventory.stream()
                .filter(i -> i.skillId().equals(skillJava.getId()))
                .findFirst()
                .orElse(null);
        assertNotNull(invItem);
        assertEquals(1, invItem.employeeCount());
        assertEquals(3.0, invItem.averageProficiencyLevel());

        // 2. Gap Analysis
        List<TeamGapAnalysisDto> gapList = managerService.getGapAnalysis(managerA.getEmail(), teamA.getId(), "HIGH", null, null);
        assertFalse(gapList.isEmpty());
        TeamGapAnalysisDto gapItem = gapList.get(0);
        assertEquals(employeeA.getFullName(), gapItem.employeeName());
        assertEquals(skillJava.getName(), gapItem.skillName());
        assertEquals(3, gapItem.currentLevel());
        assertEquals(4, gapItem.requiredLevel());
        assertEquals(1, gapItem.gapSize());
        assertEquals("HIGH", gapItem.priority());

        // 3. Heatmap
        ManagerHeatmapDto heatmap = managerService.getHeatmap(managerA.getEmail(), teamA.getId());
        assertNotNull(heatmap);
        assertNotNull(heatmap.employeeSkillMatrix());
        assertFalse(heatmap.employeeSkillMatrix().employees().isEmpty());
        assertEquals(employeeA.getFullName(), heatmap.employeeSkillMatrix().employees().get(0).name());
        assertFalse(heatmap.employeeSkillMatrix().cells().isEmpty());
        ManagerHeatmapDto.EmployeeSkillCell cell = heatmap.employeeSkillMatrix().cells().stream()
                .filter(c -> c.skillId().equals(skillJava.getId()))
                .findFirst()
                .orElse(null);
        assertNotNull(cell);
        assertEquals(employeeA.getId(), cell.employeeId());
        assertEquals(skillJava.getId(), cell.skillId());
        assertEquals(3, cell.currentLevel());
        assertEquals(4, cell.requiredLevel());
    }

    @Test
    @DisplayName("Module 6: Evidence Review Authorization, Approval, and Rejection Lifecycle")
    void testEvidenceReviewWorkflowAndAuthorization() {
        // Employee A has an unverified skill
        EmployeeSkill es = new EmployeeSkill(employeeA, skillJava, "Advanced");
        es.setVerified(false);
        es = employeeSkillRepository.save(es);

        // Employee A submits skill evidence
        SkillEvidence ev = new SkillEvidence(
                es,
                "Oracle Java 21 Professional",
                "Certification exam credential",
                "https://credentials.example.com/java21-cert.pdf"
        );
        final SkillEvidence savedEv = skillEvidenceRepository.save(ev);

        // 1. Manager A sees Employee A's evidence in pending list
        List<SkillEvidenceDto> pendingA = skillService.getPendingEvidence(managerA.getEmail());
        assertTrue(pendingA.stream().anyMatch(e -> e.id().equals(savedEv.getId())));

        // 2. Manager B does NOT see Employee A's evidence (strict team scoping)
        List<SkillEvidenceDto> pendingB = skillService.getPendingEvidence(managerB.getEmail());
        assertFalse(pendingB.stream().anyMatch(e -> e.id().equals(savedEv.getId())));

        // 3. Manager B attempts to review Employee A's evidence -> throws 403 FORBIDDEN
        Long evidenceId = savedEv.getId();
        ApiException exCrossReview = assertThrows(ApiException.class, () ->
                skillService.reviewEvidence(managerB.getEmail(), evidenceId, "APPROVED")
        );
        assertEquals(HttpStatus.FORBIDDEN, exCrossReview.getStatus());

        // 4. Manager A reviews and APPROVES Employee A's evidence
        SkillEvidenceDto reviewed = skillService.reviewEvidence(managerA.getEmail(), evidenceId, "APPROVED");
        assertNotNull(reviewed);
        assertEquals("APPROVED", reviewed.status());

        // Verify entity updated in DB
        EmployeeSkill refreshedEs = employeeSkillRepository.findById(es.getId()).orElseThrow();
        assertTrue(refreshedEs.isVerified());

        // 5. Attempting to review an already-approved evidence throws 400 BAD REQUEST
        ApiException exDoubleReview = assertThrows(ApiException.class, () ->
                skillService.reviewEvidence(managerA.getEmail(), evidenceId, "REJECTED")
        );
        assertEquals(HttpStatus.BAD_REQUEST, exDoubleReview.getStatus());
    }

    @Test
    @DisplayName("Module 6 (Cont.): Reassessment Request Lifecycle and Ownership Security")
    void testReassessmentRequestLifecycle() {
        CreateReassessmentRequest req = new CreateReassessmentRequest(
                employeeA.getId(),
                skillJava.getId(),
                "Targeted validation for upcoming enterprise project"
        );

        // 1. Manager A creates reassessment request
        ReassessmentRequestDto created = managerService.createReassessmentRequest(managerA.getEmail(), req);
        assertNotNull(created);
        assertEquals("PENDING", created.status());
        assertEquals(employeeA.getFullName(), created.employeeName());
        assertEquals(skillJava.getName(), created.skillName());

        // 2. Manager B attempts to cancel Manager A's request -> throws 403 FORBIDDEN
        ApiException exCancelCross = assertThrows(ApiException.class, () ->
                managerService.cancelReassessmentRequest(managerB.getEmail(), created.id())
        );
        assertEquals(HttpStatus.FORBIDDEN, exCancelCross.getStatus());

        // 3. Manager A cancels own request -> succeeds
        assertDoesNotThrow(() ->
                managerService.cancelReassessmentRequest(managerA.getEmail(), created.id())
        );

        // 4. Attempting to cancel again throws 400 BAD REQUEST (not PENDING)
        ApiException exCancelAgain = assertThrows(ApiException.class, () ->
                managerService.cancelReassessmentRequest(managerA.getEmail(), created.id())
        );
        assertEquals(HttpStatus.BAD_REQUEST, exCancelAgain.getStatus());
    }

    @Test
    @DisplayName("Module 7 & 9: Course Recommendation and Mentor Referral Validation")
    void testCourseRecommendationAndMentorReferral() {
        // 1. Recommend Course to Employee A
        CreateManagerCourseRecommendationRequest courseReq = new CreateManagerCourseRecommendationRequest(
                employeeA.getId(),
                courseJava.getId(),
                skillJava.getId(),
                "Mandatory training for architecture refactoring"
        );
        ManagerCourseRecommendationDto rec = managerService.recommendCourse(managerA.getEmail(), courseReq);
        assertNotNull(rec);
        assertEquals(courseJava.getTitle(), rec.courseTitle());
        assertEquals("RECOMMENDED", rec.status());

        // Verify notification dispatched to Employee A
        List<Notification> notifs = notificationRepository.findByRecipientIdOrderByCreatedAtDesc(employeeA.getId());
        assertTrue(notifs.stream().anyMatch(n -> n.getTitle().contains("Course Recommended")));

        // 2. Refer Non-Mentor (Employee B) -> throws 400 BAD REQUEST
        ManagerMentorReferralRequest invalidMentorReq = new ManagerMentorReferralRequest(
                employeeA.getId(),
                employeeB.getId(),
                skillJava.getId(),
                "Peer assistance"
        );
        ApiException exNotMentor = assertThrows(ApiException.class, () ->
                managerService.referMentor(managerA.getEmail(), invalidMentorReq)
        );
        assertEquals(HttpStatus.BAD_REQUEST, exNotMentor.getStatus());

        // 3. Refer Valid Mentor (mentorUser) -> succeeds
        ManagerMentorReferralRequest validMentorReq = new ManagerMentorReferralRequest(
                employeeA.getId(),
                mentorUser.getId(),
                skillJava.getId(),
                "Guidance on high-throughput microservices"
        );
        assertDoesNotThrow(() ->
                managerService.referMentor(managerA.getEmail(), validMentorReq)
        );
    }

    @Test
    @DisplayName("Module 12: Team Reports Generation Across All 8 Types and CSV Export")
    void testReportGenerationAllTypesAndExport() {
        String[] reportTypes = {
                "TEAM_SKILL_GAP_SUMMARY",
                "TEAM_PROFICIENCY_MATRIX",
                "ASSESSMENT_COMPLETION_PERFORMANCE",
                "COURSE_PROGRESS_COMPLIANCE",
                "MENTORSHIP_ENGAGEMENT",
                "SKILL_INVENTORY_CATEGORY",
                "CRITICAL_SKILL_GAPS",
                "TEAM_DEVELOPMENT_LOG"
        };

        for (String rType : reportTypes) {
            // 1. Generate Report JSON
            ManagerReportDto report = managerService.getReport(managerA.getEmail(), rType, teamA.getId());
            assertNotNull(report, "Report should not be null for " + rType);
            assertEquals(rType, report.reportType());
            assertNotNull(report.columns());
            assertNotNull(report.rows());

            // 2. Generate Report CSV
            String csv = managerService.getReportCsv(managerA.getEmail(), rType, teamA.getId());
            assertNotNull(csv, "CSV should not be null for " + rType);
            assertFalse(csv.trim().isEmpty(), "CSV content should not be empty for " + rType);
        }

        // 3. Attempting to generate report for unmanaged team throws 403 FORBIDDEN
        ApiException exUnmanaged = assertThrows(ApiException.class, () ->
                managerService.getReport(managerA.getEmail(), "TEAM_SKILL_GAP_SUMMARY", teamB.getId())
        );
        assertEquals(HttpStatus.FORBIDDEN, exUnmanaged.getStatus());
    }
}
