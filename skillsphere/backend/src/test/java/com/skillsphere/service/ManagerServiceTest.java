package com.skillsphere.service;

import com.skillsphere.dto.*;
import com.skillsphere.entity.*;
import com.skillsphere.exception.ApiException;
import com.skillsphere.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ManagerServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private EmployeeProfileRepository profileRepository;
    @Mock private TeamRepository teamRepository;
    @Mock private TeamMembershipRepository membershipRepository;
    @Mock private SkillRepository skillRepository;
    @Mock private SkillCategoryRepository categoryRepository;
    @Mock private RoleSkillRequirementRepository requirementRepository;
    @Mock private EmployeeSkillRepository employeeSkillRepository;
    @Mock private SkillGapRepository skillGapRepository;
    @Mock private AssessmentAttemptRepository assessmentAttemptRepository;
    @Mock private CourseRepository courseRepository;
    @Mock private EmployeeLearningRepository learningRepository;
    @Mock private MentorAssignmentRepository mentorAssignmentRepository;
    @Mock private AdminMentorshipService adminMentorshipService;
    @Mock private ReassessmentRequestRepository reassessmentRequestRepository;
    @Mock private ManagerCourseRecommendationRepository managerCourseRecommendationRepository;
    @Mock private NotificationRepository notificationRepository;
    @Mock private NotificationService notificationService;
    @Mock private GeminiRecommendationService geminiRecommendationService;

    private ManagerService managerService;

    private User manager;
    private User otherManager;
    private User admin;
    private User employee1;
    private User employee2;
    private Department dept;
    private Team team1;
    private Team team2;
    private Skill skillJava;
    private Course courseJava;

    @BeforeEach
    void setUp() {
        managerService = new ManagerService(
                userRepository, profileRepository, teamRepository, membershipRepository,
                skillRepository, categoryRepository, requirementRepository, employeeSkillRepository,
                skillGapRepository, assessmentAttemptRepository, courseRepository, learningRepository,
                mentorAssignmentRepository, adminMentorshipService, reassessmentRequestRepository,
                managerCourseRecommendationRepository, notificationRepository, notificationService,
                geminiRecommendationService
        );

        dept = new Department("Engineering", "ENG", "Engineering Dept", null);
        setId(dept, 101L);

        manager = createUser(10L, "Bob Manager", "manager@skillsphere.com", Role.MANAGER, "EMP-MGR");
        otherManager = createUser(20L, "Other Manager", "other@skillsphere.com", Role.MANAGER, "EMP-OTH");
        admin = createUser(1L, "Admin Super", "admin@skillsphere.com", Role.ADMIN, "EMP-ADM");
        employee1 = createUser(101L, "Alice Dev", "alice@skillsphere.com", Role.EMPLOYEE, "EMP-101");
        employee2 = createUser(102L, "Charlie Dev", "charlie@skillsphere.com", Role.EMPLOYEE, "EMP-102");

        team1 = new Team(dept, "Alpha Team", manager);
        setId(team1, 1001L);

        team2 = new Team(dept, "Beta Team", otherManager);
        setId(team2, 1002L);

        SkillCategory cat = new SkillCategory("Backend", "Backend development");
        setId(cat, 201L);

        skillJava = new Skill(cat, "Java", "Core Java and JVM");
        setId(skillJava, 501L);

        courseJava = new Course("Mastering Java", "Deep dive into Java", skillJava, "Intermediate", 15, "SkillSphere", "https://course");
        setId(courseJava, 301L);
    }

    private User createUser(Long id, String name, String email, Role role, String employeeId) {
        User u = new User();
        setId(u, id);
        u.setFullName(name);
        u.setEmail(email);
        u.setRole(role);
        u.setEmployeeId(employeeId);
        u.setPasswordHash("hash");
        u.setAccountStatus(AccountStatus.ACTIVE);
        return u;
    }

    private void setId(Object entity, Long id) {
        try {
            var field = entity.getClass().getDeclaredField("id");
            field.setAccessible(true);
            field.set(entity, id);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    @DisplayName("validateTeamAccess throws FORBIDDEN when manager accesses unmanaged team")
    void testValidateTeamAccess_Unauthorized_ThrowsForbidden() {
        when(teamRepository.findByManagerId(manager.getId())).thenReturn(List.of(team1));

        ApiException ex = assertThrows(ApiException.class, () ->
                managerService.validateTeamAccess(manager, team2.getId()));

        assertEquals(HttpStatus.FORBIDDEN, ex.getStatus());
        assertTrue(ex.getMessage().contains("You can only access teams assigned to you"));
    }

    @Test
    @DisplayName("validateTeamAccess succeeds when manager accesses their own team")
    void testValidateTeamAccess_Authorized_Success() {
        when(teamRepository.findByManagerId(manager.getId())).thenReturn(List.of(team1));

        assertDoesNotThrow(() -> managerService.validateTeamAccess(manager, team1.getId()));
    }

    @Test
    @DisplayName("validateTeamAccess allows ADMIN to access any team")
    void testValidateTeamAccess_Admin_AlwaysAllowed() {
        assertDoesNotThrow(() -> managerService.validateTeamAccess(admin, team2.getId()));
    }

    @Test
    @DisplayName("validateEmployeeAccess throws FORBIDDEN when manager accesses employee not in their team")
    void testValidateEmployeeAccess_NonMember_ThrowsForbidden() {
        TeamMembership tm1 = new TeamMembership(team1, employee1, "Developer");
        when(membershipRepository.findByManagerUserId(manager.getId())).thenReturn(List.of(tm1));

        ApiException ex = assertThrows(ApiException.class, () ->
                managerService.validateEmployeeAccess(manager, employee2.getId()));

        assertEquals(HttpStatus.FORBIDDEN, ex.getStatus());
        assertTrue(ex.getMessage().contains("You can only access employees in your managed teams"));
    }

    @Test
    @DisplayName("validateEmployeeAccess succeeds for employee in manager's team")
    void testValidateEmployeeAccess_TeamMember_Success() {
        TeamMembership tm1 = new TeamMembership(team1, employee1, "Developer");
        when(membershipRepository.findByManagerUserId(manager.getId())).thenReturn(List.of(tm1));

        assertDoesNotThrow(() -> managerService.validateEmployeeAccess(manager, employee1.getId()));
    }

    @Test
    @DisplayName("createReassessmentRequest saves request and sends notification")
    void testCreateReassessmentRequest_Success() {
        when(userRepository.findByEmail(manager.getEmail())).thenReturn(Optional.of(manager));
        TeamMembership tm1 = new TeamMembership(team1, employee1, "Developer");
        when(membershipRepository.findByManagerUserId(manager.getId())).thenReturn(List.of(tm1));
        when(userRepository.findById(employee1.getId())).thenReturn(Optional.of(employee1));
        when(skillRepository.findById(skillJava.getId())).thenReturn(Optional.of(skillJava));

        when(reassessmentRequestRepository.save(any(ReassessmentRequest.class))).thenAnswer(inv -> {
            ReassessmentRequest r = inv.getArgument(0);
            setId(r, 999L);
            return r;
        });

        CreateReassessmentRequest req = new CreateReassessmentRequest(employee1.getId(), skillJava.getId(), "Gap in Spring Boot");
        ReassessmentRequestDto result = managerService.createReassessmentRequest(manager.getEmail(), req);

        assertNotNull(result);
        assertEquals(999L, result.id());
        assertEquals("PENDING", result.status());
        assertEquals("Java", result.skillName());
        assertEquals(employee1.getFullName(), result.employeeName());

        verify(notificationService).createNotification(
                eq(employee1),
                eq("REASSESSMENT_REQUESTED"),
                eq("Reassessment Requested"),
                contains("Gap in Spring Boot"),
                eq("/dashboard/employee?tab=assessment")
        );
    }

    @Test
    @DisplayName("recommendCourse saves recommendation and notifies employee")
    void testRecommendCourse_Success() {
        when(userRepository.findByEmail(manager.getEmail())).thenReturn(Optional.of(manager));
        TeamMembership tm1 = new TeamMembership(team1, employee1, "Developer");
        when(membershipRepository.findByManagerUserId(manager.getId())).thenReturn(List.of(tm1));
        when(userRepository.findById(employee1.getId())).thenReturn(Optional.of(employee1));
        when(courseRepository.findById(courseJava.getId())).thenReturn(Optional.of(courseJava));
        when(skillRepository.findById(skillJava.getId())).thenReturn(Optional.of(skillJava));

        when(managerCourseRecommendationRepository.save(any(ManagerCourseRecommendation.class))).thenAnswer(inv -> {
            ManagerCourseRecommendation r = inv.getArgument(0);
            setId(r, 888L);
            return r;
        });

        CreateManagerCourseRecommendationRequest req = new CreateManagerCourseRecommendationRequest(
                employee1.getId(), courseJava.getId(), skillJava.getId(), "Recommended for Q4 objectives"
        );
        ManagerCourseRecommendationDto result = managerService.recommendCourse(manager.getEmail(), req);

        assertNotNull(result);
        assertEquals(888L, result.id());
        assertEquals("RECOMMENDED", result.status());
        assertEquals("Mastering Java", result.courseTitle());

        verify(notificationService).createNotification(
                eq(employee1),
                eq("COURSE_RECOMMENDED"),
                eq("Course Recommended by Manager"),
                contains("Recommended for Q4 objectives"),
                eq("/dashboard/employee?tab=learning")
        );
    }

    @Test
    @DisplayName("referMentor sends notifications to both mentor and employee")
    void testReferMentor_Success() {
        when(userRepository.findByEmail(manager.getEmail())).thenReturn(Optional.of(manager));
        TeamMembership tm1 = new TeamMembership(team1, employee1, "Developer");
        when(membershipRepository.findByManagerUserId(manager.getId())).thenReturn(List.of(tm1));

        User mentorUser = createUser(55L, "Jane Mentor", "mentor@skillsphere.com", Role.MENTOR, "EMP-MNT");

        when(userRepository.findById(employee1.getId())).thenReturn(Optional.of(employee1));
        when(userRepository.findById(mentorUser.getId())).thenReturn(Optional.of(mentorUser));
        when(skillRepository.findById(skillJava.getId())).thenReturn(Optional.of(skillJava));

        ManagerMentorReferralRequest req = new ManagerMentorReferralRequest(employee1.getId(), mentorUser.getId(), skillJava.getId(), "Please help with Java architecture");
        managerService.referMentor(manager.getEmail(), req);

        verify(notificationService).createNotification(
                eq(mentorUser),
                eq("MENTOR_REFERRAL"),
                eq("Manager Mentorship Referral"),
                contains("Please help with Java architecture"),
                eq("/dashboard/mentor")
        );

        verify(notificationService).createNotification(
                eq(employee1),
                eq("MENTOR_REFERRAL"),
                eq("Mentor Referral from Manager"),
                contains("Jane Mentor"),
                eq("/dashboard/employee?tab=mentors")
        );
    }

    @Test
    @DisplayName("getReport and getReportCsv return proper RFC-4180 CSV")
    void testGetReportCsv_Success() {
        when(userRepository.findByEmail(manager.getEmail())).thenReturn(Optional.of(manager));
        TeamMembership tm1 = new TeamMembership(team1, employee1, "Developer");
        when(membershipRepository.findByManagerUserId(manager.getId())).thenReturn(List.of(tm1));

        SkillGap gap = new SkillGap(employee1, skillJava, 2, 4, 2, "HIGH");
        when(skillGapRepository.findByUserIds(List.of(employee1.getId()))).thenReturn(List.of(gap));

        String csv = managerService.getReportCsv(manager.getEmail(), "TEAM_SKILL_GAP_SUMMARY", null);

        assertNotNull(csv);
        assertTrue(csv.contains("# Report: Team Skill Gap Summary"));
        assertTrue(csv.contains("Employee,Employee ID,Skill,Category"));
        assertTrue(csv.contains("Alice Dev"));
        assertTrue(csv.contains("Java"));
        assertTrue(csv.contains("HIGH"));
    }
}
