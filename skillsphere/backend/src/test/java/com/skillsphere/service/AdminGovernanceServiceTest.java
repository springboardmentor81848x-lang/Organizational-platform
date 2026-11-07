package com.skillsphere.service;

import com.skillsphere.dto.PlatformFeedbackDto;
import com.skillsphere.dto.SupportTicketDto;
import com.skillsphere.entity.PlatformFeedback;
import com.skillsphere.entity.Role;
import com.skillsphere.entity.SupportTicket;
import com.skillsphere.entity.User;
import com.skillsphere.exception.ApiException;
import com.skillsphere.repository.AuditLogRepository;
import com.skillsphere.repository.PlatformFeedbackRepository;
import com.skillsphere.repository.SupportTicketRepository;
import com.skillsphere.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminGovernanceServiceTest {

    @Mock
    private AuditLogRepository auditLogRepository;
    @Mock
    private SupportTicketRepository supportTicketRepository;
    @Mock
    private PlatformFeedbackRepository feedbackRepository;
    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private AdminGovernanceService governanceService;

    private User employee;
    private User otherEmployee;

    @BeforeEach
    void setUp() {
        employee = new User();
        employee.setFullName("Alice Employee");
        employee.setEmail("alice@example.com");
        employee.setRole(Role.EMPLOYEE);
        ReflectionTestUtils.setField(employee, "id", 101L);

        otherEmployee = new User();
        otherEmployee.setFullName("Bob Colleague");
        otherEmployee.setEmail("bob@example.com");
        otherEmployee.setRole(Role.EMPLOYEE);
        ReflectionTestUtils.setField(otherEmployee, "id", 102L);
    }

    @Test
    @DisplayName("createTicket successfully persists support ticket")
    void testCreateTicket_Success() {
        when(userRepository.findByEmail("alice@example.com")).thenReturn(Optional.of(employee));
        when(supportTicketRepository.save(any(SupportTicket.class))).thenAnswer(inv -> inv.getArgument(0));

        SupportTicketDto.CreateRequest req = new SupportTicketDto.CreateRequest("Login issue", "Cannot login with SSO", "HIGH");
        SupportTicketDto result = governanceService.createTicket("alice@example.com", req);

        assertNotNull(result);
        assertEquals("Login issue", result.subject());
        assertEquals("HIGH", result.priority());
        assertEquals("OPEN", result.status());
        verify(supportTicketRepository).save(any(SupportTicket.class));
    }

    @Test
    @DisplayName("closeMyTicket marks ticket as CLOSED when called by owner")
    void testCloseMyTicket_Success() {
        SupportTicket ticket = new SupportTicket(employee, "Assessment Question Bug", "Typo in question 3", "LOW");
        ReflectionTestUtils.setField(ticket, "id", 501L);

        when(userRepository.findByEmail("alice@example.com")).thenReturn(Optional.of(employee));
        when(supportTicketRepository.findById(501L)).thenReturn(Optional.of(ticket));
        when(supportTicketRepository.save(any(SupportTicket.class))).thenAnswer(inv -> inv.getArgument(0));

        SupportTicketDto result = governanceService.closeMyTicket("alice@example.com", 501L);

        assertNotNull(result);
        assertEquals("CLOSED", result.status());
        verify(supportTicketRepository).save(ticket);
    }

    @Test
    @DisplayName("closeMyTicket throws FORBIDDEN when called on another employee's ticket")
    void testCloseMyTicket_ForbiddenForNonOwner() {
        SupportTicket ticket = new SupportTicket(employee, "Private Ticket", "Sensitive issue", "MEDIUM");
        ReflectionTestUtils.setField(ticket, "id", 501L);

        when(userRepository.findByEmail("bob@example.com")).thenReturn(Optional.of(otherEmployee));
        when(supportTicketRepository.findById(501L)).thenReturn(Optional.of(ticket));

        ApiException ex = assertThrows(ApiException.class, () ->
                governanceService.closeMyTicket("bob@example.com", 501L));

        assertEquals(HttpStatus.FORBIDDEN, ex.getStatus());
        assertTrue(ex.getMessage().contains("You are not authorized to close this ticket"));
        verify(supportTicketRepository, never()).save(ticket);
    }

    @Test
    @DisplayName("submitFeedback saves user feedback record")
    void testSubmitFeedback_Success() {
        when(userRepository.findByEmail("alice@example.com")).thenReturn(Optional.of(employee));
        when(feedbackRepository.save(any(PlatformFeedback.class))).thenAnswer(inv -> inv.getArgument(0));

        PlatformFeedbackDto.CreateRequest req = new PlatformFeedbackDto.CreateRequest("Assessments", 5, "Great adaptive quizzes!");
        PlatformFeedbackDto dto = governanceService.submitFeedback("alice@example.com", req);

        assertNotNull(dto);
        assertEquals("Assessments", dto.category());
        assertEquals(5, dto.rating());
        verify(feedbackRepository).save(any(PlatformFeedback.class));
    }
}
