package com.skillsphere.service;

import com.skillsphere.dto.*;
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
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MentorshipServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private EmployeeProfileRepository profileRepository;
    @Mock
    private MentorProfileRepository mentorProfileRepository;
    @Mock
    private MentorAssignmentRepository assignmentRepository;
    @Mock
    private MentorshipSessionRepository sessionRepository;
    @Mock
    private MentorshipGoalRepository goalRepository;
    @Mock
    private MentorshipFeedbackRepository feedbackRepository;

    @InjectMocks
    private MentorshipService mentorshipService;

    private User mentorUser;
    private User employeeUser;
    private User adminUser;
    private MentorProfile mentorProfile;
    private MentorAssignment assignment;

    @BeforeEach
    void setUp() {
        mentorUser = new User();
        setPrivateId(mentorUser, 1L);
        mentorUser.setEmail("mentor@example.com");
        mentorUser.setFullName("Senior Mentor");

        employeeUser = new User();
        setPrivateId(employeeUser, 2L);
        employeeUser.setEmail("employee@example.com");
        employeeUser.setFullName("Junior Mentee");

        adminUser = new User();
        setPrivateId(adminUser, 3L);
        adminUser.setEmail("admin@example.com");
        adminUser.setFullName("System Admin");

        mentorProfile = new MentorProfile(mentorUser, "Cloud Architect", "Java, AWS, Microservices", 3, adminUser);
        assignment = new MentorAssignment(mentorUser, employeeUser, adminUser, "Q3 Growth Track");
    }

    private void setPrivateId(User user, Long id) {
        try {
            var field = User.class.getDeclaredField("id");
            field.setAccessible(true);
            field.set(user, id);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    @DisplayName("Expert Directory: filters mentors by expertise keyword correctly")
    void testExpertDirectoryFiltering() {
        when(mentorProfileRepository.findActiveMentors()).thenReturn(List.of(mentorProfile));
        when(assignmentRepository.countByMentorIdAndStatus(any(), eq(AssignmentStatus.ACTIVE))).thenReturn(1L);

        List<MentorDirectoryItem> result = mentorshipService.getExpertDirectory("AWS", null, null);
        assertEquals(1, result.size());
        assertEquals("Senior Mentor", result.get(0).fullName());
        assertEquals(1, result.get(0).currentMenteesCount());
        assertTrue(result.get(0).isAvailable());

        List<MentorDirectoryItem> noMatch = mentorshipService.getExpertDirectory("Python", null, null);
        assertEquals(0, noMatch.size());
    }

    @Test
    @DisplayName("Security: Mentee progress access is rejected if caller is not the assigned mentor")
    void testUnauthorizedMenteeProgressAccess() {
        when(userRepository.findByEmail("mentor@example.com")).thenReturn(Optional.of(mentorUser));
        when(mentorProfileRepository.findByUserId(any())).thenReturn(Optional.of(mentorProfile));
        // Return empty because this employee is not assigned to this mentor
        when(assignmentRepository.findByEmployeeIdAndStatus(999L, AssignmentStatus.ACTIVE)).thenReturn(Optional.empty());

        ApiException ex = assertThrows(ApiException.class, () ->
                mentorshipService.getMenteeProgress("mentor@example.com", 999L));
        assertEquals(HttpStatus.FORBIDDEN, ex.getStatus());
        assertTrue(ex.getMessage().contains("active mentorship assignment"));
    }

    @Test
    @DisplayName("Feedback: rejects submission if session is not COMPLETED")
    void testFeedbackRejectsIncompleteSession() {
        MentorshipSession session = new MentorshipSession(assignment, "Architecture Sync", "Agenda",
                Instant.now(), 45, "https://meet.google.com/abc-xyz");
        session.setStatus(SessionStatus.SCHEDULED);

        when(userRepository.findByEmail("employee@example.com")).thenReturn(Optional.of(employeeUser));
        when(sessionRepository.findById(10L)).thenReturn(Optional.of(session));

        CreateFeedbackRequest request = new CreateFeedbackRequest(5, "Great session!");

        ApiException ex = assertThrows(ApiException.class, () ->
                mentorshipService.submitFeedback("employee@example.com", 10L, request));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
        assertTrue(ex.getMessage().contains("completed sessions"));
    }

    @Test
    @DisplayName("Feedback: prevents duplicate feedback on the same session")
    void testFeedbackPreventsDuplicates() {
        MentorshipSession session = new MentorshipSession(assignment, "Architecture Sync", "Agenda",
                Instant.now(), 45, "https://meet.google.com/abc-xyz");
        session.setStatus(SessionStatus.COMPLETED);

        when(userRepository.findByEmail("employee@example.com")).thenReturn(Optional.of(employeeUser));
        when(sessionRepository.findById(10L)).thenReturn(Optional.of(session));
        when(feedbackRepository.existsBySessionId(10L)).thenReturn(true);

        CreateFeedbackRequest request = new CreateFeedbackRequest(5, "Great session!");

        ApiException ex = assertThrows(ApiException.class, () ->
                mentorshipService.submitFeedback("employee@example.com", 10L, request));
        assertEquals(HttpStatus.CONFLICT, ex.getStatus());
        assertTrue(ex.getMessage().contains("already been submitted"));
    }
}
