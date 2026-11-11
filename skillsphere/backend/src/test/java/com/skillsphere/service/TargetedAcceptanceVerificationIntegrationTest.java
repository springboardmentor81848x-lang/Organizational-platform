package com.skillsphere.service;

import com.skillsphere.dto.*;
import com.skillsphere.entity.*;
import com.skillsphere.exception.ApiException;
import com.skillsphere.repository.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.annotation.Transactional;

import java.net.http.HttpClient;
import java.net.http.HttpHeaders;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@SpringBootTest
@Transactional
public class TargetedAcceptanceVerificationIntegrationTest {

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
    private EmployeeLearningRepository employeeLearningRepository;
    @Autowired
    private CertificateRepository certificateRepository;
    @Autowired
    private AssessmentAttemptRepository attemptRepository;
    @Autowired
    private AssessmentService assessmentService;
    @Autowired
    private LearningService learningService;
    @Autowired
    private ReportService reportService;
    @Autowired
    private ManagerService managerService;
    @Autowired
    private DepartmentHeadService departmentHeadService;
    @Autowired
    private GeminiRecommendationService geminiRecommendationService;

    private User createTestUser(String email, String name, Role role) {
        User u = new User();
        u.setEmail(email);
        u.setFullName(name);
        u.setPasswordHash("$2a$12$DUMMYHASHFORTESTINGPURPOSESONLY");
        u.setRole(role);
        u.setAccountStatus(AccountStatus.ACTIVE);
        return userRepository.save(u);
    }

    private Department createTestDepartment(String name, String code, User head) {
        Department d = new Department(name, code, "Test department", head);
        return departmentRepository.save(d);
    }

    // =========================================================================
    // AREA 1: Skill catalogue and role requirements
    // =========================================================================

    @Test
    @DisplayName("Area 1: Verify all six supported roles and job-role mappings")
    void testArea1_RolesAndJobRoleMappings() {
        // 1. Verify exact 6 supported platform roles
        Role[] roles = Role.values();
        assertEquals(6, roles.length, "Platform must support exactly six roles.");
        Set<String> roleNames = new HashSet<>();
        for (Role r : roles) roleNames.add(r.name());
        assertTrue(roleNames.contains("EMPLOYEE"));
        assertTrue(roleNames.contains("MANAGER"));
        assertTrue(roleNames.contains("MENTOR"));
        assertTrue(roleNames.contains("HR_SPECIALIST"));
        assertTrue(roleNames.contains("DEPARTMENT_HEAD"));
        assertTrue(roleNames.contains("ADMIN"));
        assertFalse(roleNames.contains("L&D_ADMIN"), "L&D_ADMIN role must NOT exist.");

        // 2. Verify skill catalogue has valid categories and skills
        List<Skill> skills = skillRepository.findAll();
        assertFalse(skills.isEmpty(), "Skill catalogue must not be empty.");
        assertTrue(skills.size() >= 8, "Expected at least 8 core skills in catalogue.");

        // 3. Verify role skill requirements coverage
        List<RoleSkillRequirement> devReqs = requirementRepository.findByJobRole("Software Developer");
        assertFalse(devReqs.isEmpty(), "Software Developer must have mapped skill requirements.");
        assertTrue(devReqs.size() >= 7, "Software Developer should have at least 7 skill requirements.");

        List<RoleSkillRequirement> qaReqs = requirementRepository.findByJobRole("QA Engineer");
        assertFalse(qaReqs.isEmpty(), "QA Engineer must have mapped skill requirements.");
        assertTrue(qaReqs.size() >= 4, "QA Engineer should have at least 4 skill requirements.");

        List<RoleSkillRequirement> dataReqs = requirementRepository.findByJobRole("Data Analyst");
        assertFalse(dataReqs.isEmpty(), "Data Analyst must have mapped skill requirements.");
        assertTrue(dataReqs.size() >= 3, "Data Analyst should have at least 3 skill requirements.");

        // 4. Identify missing mapping: Support Engineer currently has no predefined requirements
        List<RoleSkillRequirement> supportReqs = requirementRepository.findByJobRole("Support Engineer");
        assertTrue(supportReqs.isEmpty(), "Documented Gap: Support Engineer has 0 requirements in role_skill_requirements.");

        // 5. Verify target proficiency values match the 5-point scale
        Set<String> validProficiencies = Set.of("Unaware", "Beginner", "Intermediate", "Advanced", "Expert");
        for (RoleSkillRequirement r : devReqs) {
            assertTrue(validProficiencies.contains(r.getRequiredProficiency()),
                    "Proficiency must be one of the standard 5 levels: " + r.getRequiredProficiency());
        }
    }

    // =========================================================================
    // AREA 2: Assessment and skill-gap consistency
    // =========================================================================

    @Test
    @DisplayName("Area 2: Verify verified proficiency selection, history preservation, and gap calculation")
    void testArea2_AssessmentAndGapConsistency() {
        // Setup employee
        User emp = createTestUser("alice.dev@test.com", "Alice Developer", Role.EMPLOYEE);

        EmployeeProfile profile = new EmployeeProfile(emp);
        profile.setJobRole("Software Developer");
        profile.setDepartment("Engineering");
        profile.setExperienceLevel("Intermediate");
        profileRepository.save(profile);

        Skill javaSkill = skillRepository.findByNameIgnoreCase("Java").orElseThrow();

        // 1. Initial State: No EmployeeSkill record → proficiency is Unaware (1)
        List<SkillGapDto> initialGaps = assessmentService.recalculateSkillGapsForUser(emp);
        SkillGapDto javaGap = initialGaps.stream()
                .filter(g -> g.skillName().equalsIgnoreCase("Java"))
                .findFirst()
                .orElseThrow();
        assertEquals(1, javaGap.currentLevel(), "Initial unassessed skill level must be 1 (Unaware).");
        assertTrue(javaGap.gapSize() > 0, "Initial gap must be greater than 0.");

        // 2. Employee takes assessment and passes with Advanced level (score = 80%)
        AssessmentAttempt attempt = new AssessmentAttempt(emp, javaSkill, null, null, "Advanced", "Unaware", "FALLBACK");
        attempt.setScore(80);
        attempt.setPassed(true);
        attempt.setStatus("COMPLETED");
        attempt.setAnswersJson("{}");
        attempt = attemptRepository.save(attempt);

        // Update employee verified skill
        EmployeeSkill es = new EmployeeSkill(emp, javaSkill, "Advanced");
        es.setVerified(true);
        employeeSkillRepository.save(es);

        // Recalculate gaps
        List<SkillGapDto> updatedGaps = assessmentService.recalculateSkillGapsForUser(emp);
        SkillGapDto updatedJavaGap = updatedGaps.stream()
                .filter(g -> g.skillName().equalsIgnoreCase("Java"))
                .findFirst()
                .orElseThrow();

        assertEquals(4, updatedJavaGap.currentLevel(), "Verified level must be Advanced (4).");
        assertEquals(0, updatedJavaGap.gapSize(), "Required is Advanced (4), current is Advanced (4) → gapSize must be 0.");
        assertEquals("ON_TRACK", updatedJavaGap.priority());

        // 3. Historical attempt preservation: original attempt is preserved in assessment_attempts
        List<AssessmentAttempt> history = attemptRepository.findByUserIdOrderByStartedAtDesc(emp.getId());
        assertEquals(1, history.size());
        assertEquals(80, history.get(0).getScore());
        assertEquals("COMPLETED", history.get(0).getStatus());

        // 4. Reassessment: employee retakes assessment at Expert level (score = 95%)
        AssessmentAttempt attempt2 = new AssessmentAttempt(emp, javaSkill, null, null, "Expert", "Advanced", "FALLBACK");
        attempt2.setScore(95);
        attempt2.setPassed(true);
        attempt2.setStatus("COMPLETED");
        attemptRepository.save(attempt2);

        es.setProficiencyLevel("Expert");
        employeeSkillRepository.save(es);

        List<SkillGapDto> expertGaps = assessmentService.recalculateSkillGapsForUser(emp);
        SkillGapDto expertJavaGap = expertGaps.stream()
                .filter(g -> g.skillName().equalsIgnoreCase("Java"))
                .findFirst()
                .orElseThrow();
        assertEquals(5, expertJavaGap.currentLevel(), "Verified level must be Expert (5).");
        assertEquals(0, expertJavaGap.gapSize());

        // Verify both attempts exist in history
        List<AssessmentAttempt> completeHistory = attemptRepository.findByUserIdOrderByStartedAtDesc(emp.getId());
        assertEquals(2, completeHistory.size(), "Both assessment attempts must be preserved in historical records.");
    }

    // =========================================================================
    // AREA 3: Learning completion and certificates
    // =========================================================================

    @Test
    @DisplayName("Area 3: Verify backend-controlled learning transitions, completion dates, and certificates")
    void testArea3_LearningCompletionAndCertificates() {
        User bob = createTestUser("bob.learner@test.com", "Bob Learner", Role.EMPLOYEE);
        User mallory = createTestUser("mallory.attacker@test.com", "Mallory Attacker", Role.EMPLOYEE);

        List<Course> courses = courseRepository.findAll();
        assertFalse(courses.isEmpty(), "Catalogue courses must exist.");
        Course course = courses.get(0);

        // 1. Enrollment: status is IN_PROGRESS with 0%
        EmployeeLearningDto enrolled = learningService.enrollInCourse(bob.getEmail(), course.getId());
        assertNotNull(enrolled);
        assertEquals(0, enrolled.progressPct());
        assertEquals("IN_PROGRESS", enrolled.status());

        // 2. Direct API manipulation attempt: Mallory tries to update Bob's progress
        assertThrows(ApiException.class, () ->
                learningService.updateProgress(mallory.getEmail(), course.getId(), 100),
                "Mallory must NOT be able to modify Bob's course progress.");

        // 3. Status transition to IN_PROGRESS
        EmployeeLearningDto inProgress = learningService.updateProgress(bob.getEmail(), course.getId(), 50);
        assertEquals(50, inProgress.progressPct());
        assertEquals("IN_PROGRESS", inProgress.status());
        assertNull(inProgress.completedAt(), "completedAt must be null while IN_PROGRESS.");

        // 4. Status transition to COMPLETED at 100%
        EmployeeLearningDto completed = learningService.updateProgress(bob.getEmail(), course.getId(), 100);
        assertEquals(100, completed.progressPct());
        assertEquals("COMPLETED", completed.status());
        assertNotNull(completed.completedAt(), "completedAt must be set upon reaching 100%.");

        // 5. Automatic Certificate Generation
        List<CertificateDto> certs = learningService.getMyCertificates(bob.getEmail());
        assertEquals(1, certs.size(), "Exactly one certificate must be generated upon course completion.");
        CertificateDto cert = certs.get(0);
        assertTrue(cert.certificateCode().startsWith("CERT-"), "Certificate code must start with CERT- prefix.");
        assertNotNull(cert.issueDate());
        assertNotNull(cert.expiryDate());

        // 6. Duplicate completion protection: calling updateProgress again does not create duplicate certificates
        learningService.updateProgress(bob.getEmail(), course.getId(), 100);
        List<CertificateDto> certsAfter = learningService.getMyCertificates(bob.getEmail());
        assertEquals(1, certsAfter.size(), "Duplicate certificates must NOT be issued for the same course.");
    }

    // =========================================================================
    // AREA 4: Report export security
    // =========================================================================

    @Test
    @DisplayName("Area 4: Verify CSV export security across all six roles and parameter manipulation")
    void testArea4_ReportExportSecurity() {
        User deptHead = createTestUser("head.sec@test.com", "Head Helen", Role.DEPARTMENT_HEAD);
        Department deptEng = createTestDepartment("Engineering Dept", "ENG-SEC", deptHead);
        Department deptSales = createTestDepartment("Sales Dept", "SALES-SEC", null);

        User manager = createTestUser("mgr.sec@test.com", "Manager Mark", Role.MANAGER);
        User otherManager = createTestUser("other.mgr@test.com", "Manager Dave", Role.MANAGER);
        User hr = createTestUser("hr.sec@test.com", "HR Hannah", Role.HR_SPECIALIST);
        User admin = createTestUser("admin.sec@test.com", "Admin Andy", Role.ADMIN);
        User employee = createTestUser("emp.sec@test.com", "Emp Eve", Role.EMPLOYEE);
        User mentor = createTestUser("mentor.sec@test.com", "Mentor Mike", Role.MENTOR);

        Team team1 = teamRepository.save(new Team(deptEng, "Alpha Team", manager));
        Team team2 = teamRepository.save(new Team(deptSales, "Beta Team", otherManager));

        membershipRepository.save(new TeamMembership(team1, employee, "Software Developer"));

        Skill skill = skillRepository.findAll().get(0);
        skillGapRepository.save(new SkillGap(employee, skill, 1, 4, 3, "HIGH"));

        // 1. Role Check: EMPLOYEE calling generateSkillGapsCsv throws 403 FORBIDDEN
        ApiException empEx = assertThrows(ApiException.class, () ->
                reportService.generateSkillGapsCsv(employee.getEmail()));
        assertEquals(HttpStatus.FORBIDDEN, empEx.getStatus(), "EMPLOYEE must not be permitted to export skill gaps CSV.");

        // 2. Role Check: MENTOR calling generateSkillGapsCsv throws 403 FORBIDDEN
        ApiException mtrEx = assertThrows(ApiException.class, () ->
                reportService.generateSkillGapsCsv(mentor.getEmail()));
        assertEquals(HttpStatus.FORBIDDEN, mtrEx.getStatus(), "MENTOR must not be permitted to export skill gaps CSV.");

        // 3. Role Check & Scope: MANAGER calling generateSkillGapsCsv receives only their team members
        String mgrCsv = reportService.generateSkillGapsCsv(manager.getEmail());
        assertNotNull(mgrCsv);
        assertTrue(mgrCsv.contains("Emp Eve"), "Manager must see employees in their managed team.");

        // 4. Manipulation: Manager attempts to export team report for another manager's team
        ApiException teamManipEx = assertThrows(ApiException.class, () ->
                managerService.getReportCsv(manager.getEmail(), "TEAM_SKILL_GAP_SUMMARY", team2.getId()));
        assertEquals(HttpStatus.FORBIDDEN, teamManipEx.getStatus(),
                "Manager attempting to export another manager's team must receive 403 FORBIDDEN.");

        // 5. Role Check & Scope: DEPARTMENT_HEAD calling generateSkillGapsCsv receives department employees
        String headCsv = reportService.generateSkillGapsCsv(deptHead.getEmail());
        assertNotNull(headCsv);
        assertTrue(headCsv.contains("Emp Eve"), "Department Head must see employees in their department.");

        // 6. Manipulation: Department Head attempts to export CSV for another department
        ApiException deptManipEx = assertThrows(ApiException.class, () ->
                departmentHeadService.generateDepartmentReport(deptHead.getEmail(), "EXECUTIVE_SUMMARY", deptSales.getId()));
        assertEquals(HttpStatus.FORBIDDEN, deptManipEx.getStatus(),
                "Department Head attempting to export another department must receive 403 FORBIDDEN.");

        // 7. Role Check: HR_SPECIALIST calling generateSkillGapsCsv receives organization-wide data
        String hrCsv = reportService.generateSkillGapsCsv(hr.getEmail());
        assertNotNull(hrCsv);
        assertTrue(hrCsv.contains("Emp Eve"));

        // 8. Role Check: ADMIN calling generateSkillGapsCsv receives organization-wide data
        String adminCsv = reportService.generateSkillGapsCsv(admin.getEmail());
        assertNotNull(adminCsv);
        assertTrue(adminCsv.contains("Emp Eve"));
    }

    @Test
    @DisplayName("Gemini Recommendations: Upstream 429 Fallback, Real PostgreSQL Catalogue, and Employee Data Isolation Integration")
    void testGeminiRecommendation_Upstream429Fallback_AndDataIsolation_Integration() throws Exception {
        User empA = createTestUser("emp_a_rec_" + UUID.randomUUID() + "@skillsphere.com", "Employee Alice", Role.EMPLOYEE);
        User empB = createTestUser("emp_b_rec_" + UUID.randomUUID() + "@skillsphere.com", "Employee Bob", Role.EMPLOYEE);

        Skill skill = skillRepository.findAll().stream().findFirst().orElseThrow();
        Course course = courseRepository.findAll().stream().findFirst()
                .orElseGet(() -> courseRepository.save(new Course("Integration Cloud Architecture", "Cloud", skill, "Intermediate", 25, "NPTEL", "https://nptel.ac.in/java")));

        RecommendationRequest req = new RecommendationRequest(
                "Software Engineer",
                "Intermediate",
                "Engineering",
                70,
                List.of(new RecommendationRequest.SkillResult(skill.getName(), 3, 5, 2, 4))
        );

        Object originalHttp = ReflectionTestUtils.getField(geminiRecommendationService, "http");
        Object originalApiKey = ReflectionTestUtils.getField(geminiRecommendationService, "apiKey");

        HttpClient mockHttp = mock(HttpClient.class);
        @SuppressWarnings("unchecked")
        HttpResponse<String> mockResponse = mock(HttpResponse.class);
        HttpHeaders headers = HttpHeaders.of(Map.of("Retry-After", List.of("45")), (k, v) -> true);

        when(mockResponse.statusCode()).thenReturn(429);
        when(mockResponse.headers()).thenReturn(headers);
        when(mockHttp.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(mockResponse);

        try {
            ReflectionTestUtils.setField(geminiRecommendationService, "http", mockHttp);
            ReflectionTestUtils.setField(geminiRecommendationService, "apiKey", "test-api-key");

            // 1. Employee A calls recommend() -> Upstream returns 429 -> Falls back to PostgreSQL catalogue
            RecommendationResponse resA = geminiRecommendationService.recommend(empA.getEmail(), req);
            assertNotNull(resA, "Recommendation response must not be null");
            assertEquals("rules", resA.source(), "Source must fall back to rules");
            assertTrue(resA.notice().contains("Gemini rate limit reached"), "Notice must inform about rate limit fallback");
            assertNotNull(resA.recommendations(), "Course recommendations list must be present");
            assertFalse(resA.recommendations().isEmpty(), "Recommendations from real PostgreSQL course catalogue must be returned");

            // 2. Employee B calls recommend() with the same request -> Must receive distinct execution (no cross-employee cache leak)
            RecommendationResponse resB = geminiRecommendationService.recommend(empB.getEmail(), req);
            assertNotNull(resB);
            assertEquals("rules", resB.source());
            assertNotSame(resA, resB, "User B must not receive User A's cached response instance");

            // 3. Employee A calls again immediately with identical request -> Serves cached response
            RecommendationResponse resACached = geminiRecommendationService.recommend(empA.getEmail(), req);
            assertSame(resA, resACached, "Immediate identical repeat call for User A must serve cached response");
        } finally {
            ReflectionTestUtils.setField(geminiRecommendationService, "http", originalHttp);
            ReflectionTestUtils.setField(geminiRecommendationService, "apiKey", originalApiKey);
        }
    }
}
