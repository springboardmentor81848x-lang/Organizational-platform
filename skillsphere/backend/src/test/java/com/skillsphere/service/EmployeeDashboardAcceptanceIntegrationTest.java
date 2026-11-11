package com.skillsphere.service;

import com.skillsphere.dto.*;
import com.skillsphere.entity.*;
import com.skillsphere.repository.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
public class EmployeeDashboardAcceptanceIntegrationTest {

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
    private EmployeeLearningRepository employeeLearningRepository;
    @Autowired
    private CertificateRepository certificateRepository;
    @Autowired
    private AssessmentAttemptRepository attemptRepository;
    @Autowired
    private MentorshipRequestRepository mentorshipRequestRepository;
    @Autowired
    private SkillEvidenceRepository skillEvidenceRepository;

    @Autowired
    private EmployeeDashboardService employeeDashboardService;
    @Autowired
    private EmployeeProfileService employeeProfileService;
    @Autowired
    private AssessmentService assessmentService;
    @Autowired
    private LearningService learningService;
    @Autowired
    private SkillService skillService;
    @Autowired
    private MentorshipRequestService mentorshipRequestService;
    @Autowired
    private ManagerService managerService;

    private User createTestUser(String email, String name, Role role) {
        User u = new User();
        u.setEmail(email.toLowerCase());
        u.setPasswordHash("$2a$10$abcdefghijklmnopqrstuvwxyzABCDEF");
        u.setFullName(name);
        u.setRole(role);
        u.setEmployeeId("EMP-" + UUID.randomUUID().toString().substring(0, 7).toUpperCase());
        return userRepository.save(u);
    }

    @Test
    @DisplayName("Employee Overview and Profile: Organizational Identity, Team, and Profile Update Persistence")
    void testEmployeeOverviewAndProfileEnrichment() {
        Department dept = departmentRepository.save(new Department("Engineering Acceptance", "ENG-ACC", "Dept for acceptance tests", null));
        User manager = createTestUser("mgr_acc_" + UUID.randomUUID() + "@skillsphere.com", "Acceptance Manager", Role.MANAGER);
        Team team = teamRepository.save(new Team(dept, "Alpha Squad", manager));

        User emp = createTestUser("emp_acc_" + UUID.randomUUID() + "@skillsphere.com", "Acceptance Employee", Role.EMPLOYEE);
        membershipRepository.save(new TeamMembership(team, emp));

        EmployeeProfile prof = profileRepository.findByUserId(emp.getId()).orElseGet(() -> new EmployeeProfile(emp));
        prof.setJobRole("Software Developer");
        prof.setDepartment(dept.getName());
        prof.setExperienceLevel("Intermediate");
        prof.setSkills("Java, Spring, SQL");
        prof.setBio("Passionate backend engineer");
        profileRepository.save(prof);

        // 1. Verify getProfileByEmail returns enriched organizational data
        EmployeeProfileDto profileDto = employeeProfileService.getProfileByEmail(emp.getEmail());
        assertNotNull(profileDto);
        assertEquals(emp.getFullName(), profileDto.fullName());
        assertEquals(emp.getEmployeeId(), profileDto.employeeId());
        assertEquals(emp.getEmail(), profileDto.email());
        assertEquals("Alpha Squad", profileDto.teamName());
        assertEquals("Acceptance Manager", profileDto.reportingManagerName());
        assertEquals("Software Developer", profileDto.jobRole());
        assertEquals("Engineering Acceptance", profileDto.department());

        // 2. Verify updateProfile persists changes to PostgreSQL
        UpdateProfileRequest updateReq = new UpdateProfileRequest(
                "QA Engineer",
                "Quality Assurance",
                "Experienced",
                "Continuous integration & testing specialist",
                "JUnit, Cypress, Postman"
        );
        EmployeeProfileDto updatedDto = employeeProfileService.updateProfile(emp.getEmail(), updateReq);
        assertNotNull(updatedDto);
        assertEquals("QA Engineer", updatedDto.jobRole());
        assertEquals("Quality Assurance", updatedDto.department());
        assertEquals("Experienced", updatedDto.experienceLevel());
        assertEquals("Alpha Squad", updatedDto.teamName());
        assertEquals("Acceptance Manager", updatedDto.reportingManagerName());

        // 3. Verify getOverview returns the updated profile and organizational data
        EmployeeDashboardOverviewDto overview = employeeDashboardService.getOverview(emp.getEmail());
        assertNotNull(overview);
        assertEquals(emp.getFullName(), overview.fullName());
        assertEquals(emp.getEmployeeId(), overview.employeeId());
        assertEquals("QA Engineer", overview.jobRole());
        assertEquals("Alpha Squad", overview.teamName());
        assertEquals("Acceptance Manager", overview.reportingManagerName());
    }

    @Test
    @DisplayName("Assessment Workflow: Start Attempt, Submit Answers, Recalculate Skill Gaps, and Activity Stream")
    void testAssessmentWorkflowAndGapRecalculation() {
        Department dept = departmentRepository.save(new Department("DevOps Dept", "DO-01", "Cloud & Infrastructure", null));
        User emp = createTestUser("emp_asm_" + UUID.randomUUID() + "@skillsphere.com", "DevOps Engineer", Role.EMPLOYEE);

        SkillCategory cat = categoryRepository.findAll().stream().findFirst()
                .orElseGet(() -> categoryRepository.save(new SkillCategory("Cloud Category", "Cloud skills")));
        Skill skill = skillRepository.save(new Skill(cat, "Kubernetes Orchestration " + UUID.randomUUID().toString().substring(0, 6), "Cloud"));
        requirementRepository.save(new RoleSkillRequirement("Software Developer", skill, "Advanced", "HIGH"));

        // Start assessment attempt
        StartAssessmentRequest startReq = new StartAssessmentRequest(skill.getId(), "Advanced", null, null);
        AssessmentAttemptDto attempt = assessmentService.startAssessment(emp.getEmail(), startReq);
        assertNotNull(attempt);
        assertNotNull(attempt.attemptId());
        assertEquals("IN_PROGRESS", attempt.status());
        assertFalse(attempt.questions().isEmpty(), "Questions should be generated from bank or AI");

        // Submit attempt answering options
        Map<Long, Integer> answers = new HashMap<>();
        for (QuestionDto q : attempt.questions()) {
            answers.put(q.id(), 0);
        }

        SubmitAssessmentRequest submitReq = new SubmitAssessmentRequest(null, answers);
        AssessmentResultDto result = assessmentService.submitAttempt(emp.getEmail(), attempt.attemptId(), submitReq);
        assertNotNull(result);

        // Verify EmployeeSkill is saved
        Optional<EmployeeSkill> es = employeeSkillRepository.findByUserIdAndSkillId(emp.getId(), skill.getId());
        assertTrue(es.isPresent());

        // Verify SkillGap recalculation
        List<SkillGap> gaps = skillGapRepository.findByUserId(emp.getId());
        assertFalse(gaps.isEmpty());
        SkillGap gap = gaps.stream().filter(g -> g.getSkill().getId().equals(skill.getId())).findFirst().orElse(null);
        assertNotNull(gap);

        // Verify Overview shows assessment attempt in latest score/skill and recent activities
        EmployeeDashboardOverviewDto overview = employeeDashboardService.getOverview(emp.getEmail());
        assertNotNull(overview.latestAssessmentScore());
        assertEquals(skill.getName(), overview.latestAssessmentSkill());
        assertTrue(overview.recentActivities().stream().anyMatch(a -> "ASSESSMENT".equals(a.type())));
    }

    @Test
    @DisplayName("Course Enrollment and Completion: Progress Tracking, Certificate Issuance & Verification")
    void testCourseEnrollmentAndCertificateIssuance() {
        User emp = createTestUser("emp_learn_" + UUID.randomUUID() + "@skillsphere.com", "Learning Champion", Role.EMPLOYEE);
        SkillCategory cat = categoryRepository.findAll().stream().findFirst()
                .orElseGet(() -> categoryRepository.save(new SkillCategory("General", "General skills")));
        Skill skill = skillRepository.save(new Skill(cat, "Go Programming " + UUID.randomUUID().toString().substring(0, 6), "Languages"));
        Course course = courseRepository.save(new Course(
                "Mastering Go Concurrency",
                "Deep dive into goroutines and channels",
                skill,
                "Advanced",
                15,
                "Coursera",
                "https://coursera.org/go-concurrency"
        ));

        // 1. Enroll in course
        EmployeeLearningDto enrolled = learningService.enrollInCourse(emp.getEmail(), course.getId());
        assertNotNull(enrolled);
        assertEquals(0, enrolled.progressPct());
        assertEquals("IN_PROGRESS", enrolled.status());

        // 2. Incremental progress
        EmployeeLearningDto midProgress = learningService.updateProgress(emp.getEmail(), course.getId(), 60);
        assertEquals(60, midProgress.progressPct());
        assertEquals("IN_PROGRESS", midProgress.status());

        // 3. Complete course (100%)
        EmployeeLearningDto completed = learningService.updateProgress(emp.getEmail(), course.getId(), 100);
        assertEquals(100, completed.progressPct());
        assertEquals("COMPLETED", completed.status());

        // 4. Verify certificate was automatically issued
        List<Certificate> certs = certificateRepository.findByUserId(emp.getId());
        assertFalse(certs.isEmpty());
        Certificate cert = certs.get(0);
        assertEquals(course.getTitle() + " Certificate of Completion", cert.getTitle());
        assertNotNull(cert.getCertificateCode());
        assertNotNull(cert.getIssueDate());

        // 5. Verify certificate can be looked up by code
        Optional<Certificate> verifiedCert = certificateRepository.findByCertificateCode(cert.getCertificateCode());
        assertTrue(verifiedCert.isPresent());
        assertEquals(emp.getId(), verifiedCert.get().getUser().getId());

        // 6. Verify Overview reflects completed course, certificate count, and activity
        EmployeeDashboardOverviewDto overview = employeeDashboardService.getOverview(emp.getEmail());
        assertEquals(1, overview.completedCoursesCount());
        assertEquals(1, overview.certificatesCount());
        assertTrue(overview.recentActivities().stream().anyMatch(a -> "CERTIFICATE".equals(a.type())));
    }

    @Test
    @DisplayName("Skill Evidence Submission: Upload Evidence, Mentor/Manager Review, and Skill Verification")
    void testSkillEvidenceSubmissionAndManagerReview() {
        User reviewer = createTestUser("reviewer_" + UUID.randomUUID() + "@skillsphere.com", "Reviewing Manager", Role.MANAGER);
        User emp = createTestUser("emp_ev_" + UUID.randomUUID() + "@skillsphere.com", "Evidence Submitter", Role.EMPLOYEE);

        Department dept = departmentRepository.save(new Department("Engineering Division " + UUID.randomUUID().toString().substring(0, 6), "ENG-" + UUID.randomUUID().toString().substring(0, 4), "Dept", null));
        Team team = teamRepository.save(new Team(dept, "Squad Review", reviewer));
        membershipRepository.save(new TeamMembership(team, emp));

        SkillCategory cat = categoryRepository.findAll().stream().findFirst()
                .orElseGet(() -> categoryRepository.save(new SkillCategory("General", "General skills")));
        Skill skill = skillRepository.save(new Skill(cat, "System Architecture " + UUID.randomUUID().toString().substring(0, 6), "Architecture"));

        EmployeeSkill es = new EmployeeSkill(emp, skill, "Advanced");
        es.setVerified(false);
        employeeSkillRepository.save(es);

        // Submit evidence
        SubmitEvidenceRequest submitReq = new SubmitEvidenceRequest(
                es.getId(),
                "Production Microservices Architecture",
                "Designed and deployed event-driven payment system handling 10k RPS",
                "https://github.com/example/payments-system"
        );
        SkillEvidenceDto submitted = skillService.submitEvidence(emp.getEmail(), submitReq);
        assertNotNull(submitted);
        assertEquals("PENDING", submitted.status());
        assertEquals("Production Microservices Architecture", submitted.title());

        // Review evidence: Approve
        SkillEvidenceDto reviewed = skillService.reviewEvidence(reviewer.getEmail(), submitted.id(), "APPROVED");
        assertNotNull(reviewed);
        assertEquals("APPROVED", reviewed.status());
        assertEquals(reviewer.getFullName(), reviewed.reviewedByName());

        // Verify underlying EmployeeSkill is now verified
        EmployeeSkill updatedEs = employeeSkillRepository.findById(es.getId()).orElseThrow();
        assertTrue(updatedEs.isVerified());
    }

    @Test
    @DisplayName("Mentorship Hub & Cross-Dashboard Visibility: Mentorship Request and Manager Visibility")
    void testMentorshipAndCrossDashboardVisibility() {
        Department dept = departmentRepository.save(new Department("AI Research", "AI-01", "Machine learning labs", null));
        User manager = createTestUser("manager_ai_" + UUID.randomUUID() + "@skillsphere.com", "Dr. AI Manager", Role.MANAGER);
        Team team = teamRepository.save(new Team(dept, "Cognitive Team", manager));

        User mentor = createTestUser("mentor_ai_" + UUID.randomUUID() + "@skillsphere.com", "Senior Mentor", Role.MENTOR);
        User emp = createTestUser("emp_ai_" + UUID.randomUUID() + "@skillsphere.com", "Junior Data Scientist", Role.EMPLOYEE);
        membershipRepository.save(new TeamMembership(team, emp));

        // Submit mentorship request
        CreateMentorshipRequestDto reqDto = new CreateMentorshipRequestDto(mentor.getId(), "Looking for mentoring in PyTorch and Transformer architectures.");
        MentorshipRequestResponseDto reqResp = mentorshipRequestService.createRequest(emp.getEmail(), reqDto);
        assertNotNull(reqResp);
        assertEquals("PENDING", reqResp.status());

        // Verify request exists in mentorship_requests table
        List<MentorshipRequest> pendingRequests = mentorshipRequestRepository.findByMentorIdOrderByCreatedAtDesc(mentor.getId());
        assertFalse(pendingRequests.isEmpty());
        assertEquals(emp.getId(), pendingRequests.get(0).getEmployee().getId());

        // Cross-Dashboard: Manager inspects team members and skill gaps
        List<ManagerTeamMemberDto> teamMembers = managerService.getTeamMembers(manager.getEmail(), null, null);
        assertTrue(teamMembers.stream().anyMatch(m -> m.userId().equals(emp.getId())), "Manager sees employee in team roster");
    }
}
