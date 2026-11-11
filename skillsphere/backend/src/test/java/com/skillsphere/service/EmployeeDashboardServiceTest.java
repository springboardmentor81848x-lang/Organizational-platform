package com.skillsphere.service;

import com.skillsphere.dto.EmployeeDashboardOverviewDto;
import com.skillsphere.entity.*;
import com.skillsphere.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class EmployeeDashboardServiceTest {

    private UserRepository userRepository;
    private EmployeeProfileRepository profileRepository;
    private EmployeeSkillRepository employeeSkillRepository;
    private SkillGapRepository skillGapRepository;
    private AssessmentAttemptRepository attemptRepository;
    private EmployeeLearningRepository learningRepository;
    private CertificateRepository certificateRepository;
    private MentorAssignmentRepository mentorAssignmentRepository;
    private MentorshipGoalRepository goalRepository;
    private NotificationRepository notificationRepository;
    private TeamMembershipRepository teamMembershipRepository;

    private EmployeeDashboardService dashboardService;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        profileRepository = mock(EmployeeProfileRepository.class);
        employeeSkillRepository = mock(EmployeeSkillRepository.class);
        skillGapRepository = mock(SkillGapRepository.class);
        attemptRepository = mock(AssessmentAttemptRepository.class);
        learningRepository = mock(EmployeeLearningRepository.class);
        certificateRepository = mock(CertificateRepository.class);
        mentorAssignmentRepository = mock(MentorAssignmentRepository.class);
        goalRepository = mock(MentorshipGoalRepository.class);
        notificationRepository = mock(NotificationRepository.class);
        teamMembershipRepository = mock(TeamMembershipRepository.class);

        dashboardService = new EmployeeDashboardService(
                userRepository,
                profileRepository,
                employeeSkillRepository,
                skillGapRepository,
                attemptRepository,
                learningRepository,
                certificateRepository,
                mentorAssignmentRepository,
                goalRepository,
                notificationRepository,
                teamMembershipRepository
        );
    }

    @Test
    void getOverview_returnsRealAggregatedData() {
        User user = new User();
        ReflectionTestUtils.setField(user, "id", 42L);
        user.setEmail("employee@skillsphere.com");
        user.setFullName("Jane Doe");
        user.setEmployeeId("EMP-0042");

        when(userRepository.findByEmail("employee@skillsphere.com")).thenReturn(Optional.of(user));

        EmployeeProfile profile = new EmployeeProfile();
        profile.setUser(user);
        profile.setJobRole("Full Stack Developer");
        profile.setDepartment("Engineering");
        profile.setExperienceLevel("Senior");
        when(profileRepository.findByUserId(42L)).thenReturn(Optional.of(profile));

        Skill skill1 = new Skill(null, "Java", "Backend");
        EmployeeSkill es1 = new EmployeeSkill(user, skill1, "Intermediate");
        es1.setVerified(true);

        Skill skill2 = new Skill(null, "SQL", "Database");
        EmployeeSkill es2 = new EmployeeSkill(user, skill2, "Advanced");
        es2.setVerified(false);

        when(employeeSkillRepository.findByUserId(42L)).thenReturn(List.of(es1, es2));

        SkillGap gap1 = new SkillGap();
        gap1.setGapSize(1);
        when(skillGapRepository.findByUserId(42L)).thenReturn(List.of(gap1));

        when(attemptRepository.findByUserIdOrderByStartedAtDesc(42L)).thenReturn(List.of());
        when(learningRepository.findByUserId(42L)).thenReturn(List.of());
        when(certificateRepository.findByUserId(42L)).thenReturn(List.of());
        when(mentorAssignmentRepository.findByEmployeeIdAndStatus(42L, AssignmentStatus.ACTIVE)).thenReturn(Optional.empty());
        when(notificationRepository.countByRecipientIdAndIsReadFalse(42L)).thenReturn(3L);
        when(teamMembershipRepository.findByUserIdWithTeamAndManager(42L)).thenReturn(List.of());

        EmployeeDashboardOverviewDto overview = dashboardService.getOverview("employee@skillsphere.com");

        assertNotNull(overview);
        assertEquals("Jane Doe", overview.fullName());
        assertEquals("EMP-0042", overview.employeeId());
        assertEquals("Full Stack Developer", overview.jobRole());
        assertEquals(2, overview.skillsCount());
        assertEquals(1, overview.verifiedSkillsCount());
        assertEquals(3.5, overview.averageProficiency());
        assertEquals(1, overview.activeGapsCount());
        assertEquals(3L, overview.unreadNotificationsCount());
        assertFalse(overview.hasMentor());
    }
}
