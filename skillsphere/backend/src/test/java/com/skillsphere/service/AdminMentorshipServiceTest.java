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

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminMentorshipServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private EmployeeProfileRepository profileRepository;
    @Mock
    private MentorProfileRepository mentorProfileRepository;
    @Mock
    private MentorAssignmentRepository assignmentRepository;

    @InjectMocks
    private AdminMentorshipService adminMentorshipService;

    private User adminUser;
    private User employeeUser;
    private User mentorUser;
    private MentorProfile mentorProfile;

    @BeforeEach
    void setUp() {
        adminUser = new User();
        setPrivateId(adminUser, 1L);
        adminUser.setEmail("admin@example.com");
        adminUser.setFullName("Admin User");

        employeeUser = new User();
        setPrivateId(employeeUser, 2L);
        employeeUser.setEmail("emp@example.com");
        employeeUser.setFullName("Regular Employee");

        mentorUser = new User();
        setPrivateId(mentorUser, 3L);
        mentorUser.setEmail("mentor@example.com");
        mentorUser.setFullName("Mentor Expert");

        mentorProfile = new MentorProfile(mentorUser, "Principal Architect", "Java, Spring, Distributed Systems", 2, adminUser);
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
    @DisplayName("Admin: rejects self-approval as mentor")
    void testPreventSelfApproval() {
        when(userRepository.findByEmail("admin@example.com")).thenReturn(Optional.of(adminUser));

        ApproveMentorRequest req = new ApproveMentorRequest("Headline", "Java", 3);

        ApiException ex = assertThrows(ApiException.class, () ->
                adminMentorshipService.approveMentor(1L, req, "admin@example.com"));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
        assertTrue(ex.getMessage().contains("cannot approve themselves"));
    }

    @Test
    @DisplayName("Admin: rejects self-mentoring assignment")
    void testPreventSelfMentoring() {
        when(userRepository.findByEmail("admin@example.com")).thenReturn(Optional.of(adminUser));

        CreateAssignmentRequest req = new CreateAssignmentRequest(2L, 2L, "Self mentoring notes");

        ApiException ex = assertThrows(ApiException.class, () ->
                adminMentorshipService.createAssignment(req, "admin@example.com"));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
        assertTrue(ex.getMessage().contains("cannot be assigned to mentor themselves"));
    }

    @Test
    @DisplayName("Admin: enforces mentor maximum capacity limit")
    void testEnforceMentorCapacity() {
        when(userRepository.findByEmail("admin@example.com")).thenReturn(Optional.of(adminUser));
        when(userRepository.findById(3L)).thenReturn(Optional.of(mentorUser));
        when(mentorProfileRepository.findByUserId(3L)).thenReturn(Optional.of(mentorProfile)); // max 2
        when(assignmentRepository.countByMentorIdAndStatus(3L, AssignmentStatus.ACTIVE)).thenReturn(2L); // already 2

        CreateAssignmentRequest req = new CreateAssignmentRequest(3L, 2L, "Notes");

        ApiException ex = assertThrows(ApiException.class, () ->
                adminMentorshipService.createAssignment(req, "admin@example.com"));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
        assertTrue(ex.getMessage().contains("maximum capacity"));
    }

    @Test
    @DisplayName("Admin: prevents duplicate active mentor for an employee")
    void testPreventDuplicateActiveMentor() {
        when(userRepository.findByEmail("admin@example.com")).thenReturn(Optional.of(adminUser));
        when(userRepository.findById(3L)).thenReturn(Optional.of(mentorUser));
        when(mentorProfileRepository.findByUserId(3L)).thenReturn(Optional.of(mentorProfile)); // max 2
        when(assignmentRepository.countByMentorIdAndStatus(3L, AssignmentStatus.ACTIVE)).thenReturn(0L);
        when(userRepository.findById(2L)).thenReturn(Optional.of(employeeUser));
        when(assignmentRepository.existsByEmployeeIdAndStatus(2L, AssignmentStatus.ACTIVE)).thenReturn(true);

        CreateAssignmentRequest req = new CreateAssignmentRequest(3L, 2L, "Notes");

        ApiException ex = assertThrows(ApiException.class, () ->
                adminMentorshipService.createAssignment(req, "admin@example.com"));
        assertEquals(HttpStatus.CONFLICT, ex.getStatus());
        assertTrue(ex.getMessage().contains("already has an active mentor"));
    }
}
