package com.skillsphere.service;

import com.skillsphere.dto.CreateMentorshipRequestDto;
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

import java.lang.reflect.Field;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MentorshipRequestServiceTest {

    @Mock
    private MentorshipRequestRepository requestRepository;
    @Mock
    private MentorAssignmentRepository assignmentRepository;
    @Mock
    private SharedResourceRepository sharedResourceRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private EmployeeProfileRepository profileRepository;
    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private MentorshipRequestService mentorshipRequestService;

    private User employee;
    private User mentor;

    @BeforeEach
    void setUp() throws Exception {
        employee = new User();
        employee.setFullName("Jane Employee");
        employee.setEmail("jane@example.com");
        employee.setRole(Role.EMPLOYEE);

        mentor = new User();
        mentor.setFullName("Mark Mentor");
        mentor.setEmail("mark@example.com");
        mentor.setRole(Role.MENTOR);

        // Set IDs via reflection
        Field idField = User.class.getDeclaredField("id");
        idField.setAccessible(true);
        idField.set(employee, 100L);
        idField.set(mentor, 200L);
    }

    @Test
    @DisplayName("Self mentorship request throws 400 Bad Request")
    void testCreateRequest_SelfRequest_ThrowsBadRequest() {
        when(userRepository.findByEmail("jane@example.com")).thenReturn(Optional.of(employee));
        when(userRepository.findById(100L)).thenReturn(Optional.of(employee));

        CreateMentorshipRequestDto dto = new CreateMentorshipRequestDto(100L, "I want to mentor myself");
        ApiException ex = assertThrows(ApiException.class, () -> mentorshipRequestService.createRequest("jane@example.com", dto));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
    }

    @Test
    @DisplayName("Duplicate pending request throws 409 Conflict")
    void testCreateRequest_DuplicatePending_ThrowsConflict() {
        when(userRepository.findByEmail("jane@example.com")).thenReturn(Optional.of(employee));
        when(userRepository.findById(200L)).thenReturn(Optional.of(mentor));
        when(requestRepository.existsByMentorIdAndEmployeeIdAndStatus(200L, 100L, "PENDING")).thenReturn(true);

        CreateMentorshipRequestDto dto = new CreateMentorshipRequestDto(200L, "Please mentor me");
        ApiException ex = assertThrows(ApiException.class, () -> mentorshipRequestService.createRequest("jane@example.com", dto));

        assertEquals(HttpStatus.CONFLICT, ex.getStatus());
    }

    @Test
    @DisplayName("Mentor accepting request updates status and creates MentorAssignment")
    void testRespondToRequest_Accepted_CreatesAssignment() {
        MentorshipRequest request = new MentorshipRequest(mentor, employee, "Looking for guidance in Java");

        when(userRepository.findByEmail("mark@example.com")).thenReturn(Optional.of(mentor));
        when(requestRepository.findById(1L)).thenReturn(Optional.of(request));
        when(requestRepository.save(any(MentorshipRequest.class))).thenAnswer(inv -> inv.getArgument(0));
        when(assignmentRepository.existsByMentorIdAndEmployeeIdAndStatus(200L, 100L, AssignmentStatus.ACTIVE)).thenReturn(false);

        RespondMentorshipRequestDto dto = new RespondMentorshipRequestDto("ACCEPTED", "Glad to mentor you!");
        MentorshipRequestResponseDto res = mentorshipRequestService.respondToRequest("mark@example.com", 1L, dto);

        assertNotNull(res);
        assertEquals("ACCEPTED", res.status());
        verify(assignmentRepository, times(1)).save(any(MentorAssignment.class));
        verify(notificationService, times(1)).createNotification(eq(employee), anyString(), anyString(), anyString(), anyString());
    }
}
