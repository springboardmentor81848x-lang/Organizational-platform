package com.skillsphere.service;

import com.skillsphere.dto.MentorDtos.*;
import com.skillsphere.dto.MentorshipRequestResponseDto;
import com.skillsphere.dto.RespondMentorshipRequestDto;
import com.skillsphere.entity.*;
import com.skillsphere.exception.ApiException;
import com.skillsphere.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.time.Instant;
import java.time.LocalDate;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MentorDashboardServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private EmployeeProfileRepository employeeProfileRepository;
    @Mock
    private MentorProfileRepository mentorProfileRepository;
    @Mock
    private MentorAvailabilityRepository mentorAvailabilityRepository;
    @Mock
    private MentorAssignmentRepository mentorAssignmentRepository;
    @Mock
    private MentorshipRequestRepository mentorshipRequestRepository;
    @Mock
    private MentorshipGoalRepository mentorshipGoalRepository;
    @Mock
    private GoalActionItemRepository goalActionItemRepository;
    @Mock
    private MentorshipSessionRepository mentorshipSessionRepository;
    @Mock
    private MentorshipFeedbackRepository mentorshipFeedbackRepository;
    @Mock
    private MentorEvaluationRepository mentorEvaluationRepository;
    @Mock
    private MenteeSkillConsentRepository menteeSkillConsentRepository;
    @Mock
    private SharedResourceRepository sharedResourceRepository;
    @Mock
    private KnowledgeArticleRepository knowledgeArticleRepository;
    @Mock
    private KnowledgeCategoryRepository knowledgeCategoryRepository;
    @Mock
    private EmployeeSkillRepository employeeSkillRepository;
    @Mock
    private SkillGapRepository skillGapRepository;
    @Mock
    private AssessmentAttemptRepository assessmentAttemptRepository;
    @Mock
    private SkillRepository skillRepository;
    @Mock
    private NotificationRepository notificationRepository;
    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private MentorDashboardService service;

    private User mentor;
    private User employee;
    private MentorProfile mentorProfile;
    private MentorAssignment assignment;

    private User createUser(Long id, String email, String fullName, Role role) {
        User u = new User();
        u.setEmail(email);
        u.setFullName(fullName);
        u.setRole(role);
        u.setAccountStatus(AccountStatus.ACTIVE);
        setEntityId(u, id);
        return u;
    }

    @BeforeEach
    void setUp() {
        mentor = createUser(4L, "mentor@skillsphere.com", "Elena Rostova", Role.MENTOR);
        employee = createUser(2L, "emp@skillsphere.com", "Jane Developer", Role.EMPLOYEE);

        mentorProfile = new MentorProfile(mentor, "Lead Architect", "Java, Cloud", 3, mentor);
        setEntityId(mentorProfile, 1L);

        assignment = new MentorAssignment(mentor, employee, mentor, "Active Pairing");
        setEntityId(assignment, 10L);
    }

    private void setEntityId(Object entity, Long id) {
        try {
            java.lang.reflect.Field field = entity.getClass().getDeclaredField("id");
            field.setAccessible(true);
            field.set(entity, id);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    @DisplayName("getOverview calculates accurate counts and goal completion rate")
    void testGetOverview_Success() {
        when(userRepository.findByEmail("mentor@skillsphere.com")).thenReturn(Optional.of(mentor));
        when(mentorProfileRepository.findByUserId(4L)).thenReturn(Optional.of(mentorProfile));
        when(mentorAssignmentRepository.countByMentorIdAndStatus(4L, AssignmentStatus.ACTIVE)).thenReturn(2L);
        when(mentorshipRequestRepository.findByMentorIdOrderByCreatedAtDesc(4L)).thenReturn(List.of());
        when(mentorshipSessionRepository.countByAssignmentMentorIdAndStatus(4L, SessionStatus.COMPLETED)).thenReturn(6L);
        when(mentorshipSessionRepository.findByMentorIdOrderByScheduledAtDesc(4L)).thenReturn(List.of());
        when(mentorshipGoalRepository.countTotalGoalsByMentorId(4L)).thenReturn(4L);
        when(mentorshipGoalRepository.countCompletedGoalsByMentorId(4L)).thenReturn(3L);
        when(sharedResourceRepository.countByAssignmentMentorId(4L)).thenReturn(5L);
        when(knowledgeArticleRepository.countByAuthorIdAndStatus(4L, "PUBLISHED")).thenReturn(2L);
        when(mentorAssignmentRepository.findByMentorIdAndStatus(4L, AssignmentStatus.ACTIVE)).thenReturn(List.of(assignment));

        MentorDashboardOverviewDto overview = service.getOverview("mentor@skillsphere.com");

        assertNotNull(overview);
        assertEquals(2, overview.activeMenteesCount());
        assertEquals(6, overview.completedSessionsCount());
        assertEquals(75.0, overview.goalCompletionRate());
        assertEquals(5, overview.sharedResourcesCount());
        assertEquals(2, overview.knowledgeArticlesCount());
    }

    @Test
    @DisplayName("respondToRequest accepts and pairs mentee with capacity verification")
    void testRespondToRequest_Accept_Success() {
        MentorshipRequest req = new MentorshipRequest(mentor, employee, "Please mentor me");
        setEntityId(req, 100L);

        when(userRepository.findByEmail("mentor@skillsphere.com")).thenReturn(Optional.of(mentor));
        when(mentorshipRequestRepository.findById(100L)).thenReturn(Optional.of(req));
        when(mentorProfileRepository.findByUserId(4L)).thenReturn(Optional.of(mentorProfile));
        when(mentorAssignmentRepository.countByMentorIdAndStatus(4L, AssignmentStatus.ACTIVE)).thenReturn(1L);
        when(mentorAssignmentRepository.existsByMentorIdAndEmployeeIdAndStatus(4L, 2L, AssignmentStatus.ACTIVE)).thenReturn(false);
        when(mentorshipRequestRepository.save(any(MentorshipRequest.class))).thenReturn(req);
        when(menteeSkillConsentRepository.findByEmployeeIdAndMentorId(2L, 4L)).thenReturn(Optional.empty());

        RespondMentorshipRequestDto dto = new RespondMentorshipRequestDto("ACCEPTED", "Happy to help!");
        MentorshipRequestResponseDto res = service.respondToRequest("mentor@skillsphere.com", 100L, dto);

        assertNotNull(res);
        assertEquals("ACCEPTED", res.status());
        verify(mentorAssignmentRepository).save(any(MentorAssignment.class));
        verify(notificationService).createNotification(eq(employee), eq("MENTORSHIP_RESPONSE"), anyString(), anyString(), anyString());
    }

    @Test
    @DisplayName("respondToRequest rejects accept when mentor capacity is full (HTTP 409)")
    void testRespondToRequest_CapacityExceeded_ThrowsConflict() {
        MentorshipRequest req = new MentorshipRequest(mentor, employee, "Please mentor me");
        setEntityId(req, 100L);

        when(userRepository.findByEmail("mentor@skillsphere.com")).thenReturn(Optional.of(mentor));
        when(mentorshipRequestRepository.findById(100L)).thenReturn(Optional.of(req));
        when(mentorProfileRepository.findByUserId(4L)).thenReturn(Optional.of(mentorProfile));
        // max mentees is 3, active is 3
        when(mentorAssignmentRepository.countByMentorIdAndStatus(4L, AssignmentStatus.ACTIVE)).thenReturn(3L);

        RespondMentorshipRequestDto dto = new RespondMentorshipRequestDto("ACCEPTED", "Happy to help!");
        ApiException ex = assertThrows(ApiException.class,
                () -> service.respondToRequest("mentor@skillsphere.com", 100L, dto));

        assertEquals(HttpStatus.CONFLICT, ex.getStatus());
        assertTrue(ex.getMessage().contains("capacity"));
    }

    @Test
    @DisplayName("getMenteeDossier throws 403 Forbidden on IDOR attempt for unassigned assignment")
    void testGetMenteeDossier_IDOR_ThrowsForbidden() {
        User otherMentor = createUser(99L, "other@skillsphere.com", "Other Mentor", Role.MENTOR);
        MentorAssignment otherAssignment = new MentorAssignment(otherMentor, employee, otherMentor, "Notes");
        setEntityId(otherAssignment, 55L);

        when(userRepository.findByEmail("mentor@skillsphere.com")).thenReturn(Optional.of(mentor));
        when(mentorAssignmentRepository.findById(55L)).thenReturn(Optional.of(otherAssignment));

        ApiException ex = assertThrows(ApiException.class,
                () -> service.getMenteeDossier("mentor@skillsphere.com", 55L));

        assertEquals(HttpStatus.FORBIDDEN, ex.getStatus());
    }

    @Test
    @DisplayName("createSession enforces conflict detection (HTTP 409 on overlap)")
    void testCreateSession_TimeConflict_ThrowsConflict() {
        when(userRepository.findByEmail("mentor@skillsphere.com")).thenReturn(Optional.of(mentor));
        when(mentorAssignmentRepository.findById(10L)).thenReturn(Optional.of(assignment));

        Instant scheduledTime = Instant.now().plusSeconds(7200);
        MentorshipSession existing = new MentorshipSession(assignment, "Existing Session", "Notes", scheduledTime, 45, "link");
        when(mentorshipSessionRepository.findByMentorIdOrderByScheduledAtDesc(4L)).thenReturn(List.of(existing));

        // Overlapping session starting 15 minutes into existing session
        CreateMentorSessionRequest req = new CreateMentorSessionRequest(
                10L, "New Conflicting Session", "Description",
                scheduledTime.plusSeconds(900), 45, "https://meet.google.com/test"
        );

        ApiException ex = assertThrows(ApiException.class,
                () -> service.createSession("mentor@skillsphere.com", req));

        assertEquals(HttpStatus.CONFLICT, ex.getStatus());
        assertTrue(ex.getMessage().contains("conflict"));
    }

    @Test
    @DisplayName("createEvaluation allows rating for mentored employee and notifies mentee")
    void testCreateEvaluation_Success() {
        when(userRepository.findByEmail("mentor@skillsphere.com")).thenReturn(Optional.of(mentor));
        when(userRepository.findById(2L)).thenReturn(Optional.of(employee));
        when(mentorAssignmentRepository.existsByMentorIdAndEmployeeIdAndStatus(4L, 2L, AssignmentStatus.ACTIVE)).thenReturn(true);

        MentorEvaluation saved = new MentorEvaluation(mentor, employee, null, 5, "Strong architecture skills", "Test coverage", "Great mentee");
        setEntityId(saved, 500L);
        when(mentorEvaluationRepository.save(any(MentorEvaluation.class))).thenReturn(saved);

        CreateMentorEvaluationRequest req = new CreateMentorEvaluationRequest(
                2L, null, 5, "Strong architecture skills", "Test coverage", "Great mentee"
        );

        MentorEvaluationDto result = service.createEvaluation("mentor@skillsphere.com", req);

        assertNotNull(result);
        assertEquals(5, result.rating());
        assertEquals("Great mentee", result.comments());
        verify(notificationService).createNotification(eq(employee), eq("MENTOR_EVALUATION"), anyString(), anyString(), anyString());
    }
}
