package com.skillsphere.service;

import com.skillsphere.dto.CertificateDto;
import com.skillsphere.dto.EmployeeLearningDto;
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
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LearningServiceTest {

    @Mock
    private CourseRepository courseRepository;
    @Mock
    private SkillRepository skillRepository;
    @Mock
    private EmployeeLearningRepository employeeLearningRepository;
    @Mock
    private CertificateRepository certificateRepository;
    @Mock
    private TrainingProgrammeRepository trainingProgrammeRepository;
    @Mock
    private TrainingEnrollmentRepository trainingEnrollmentRepository;
    @Mock
    private SkillGapRepository skillGapRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private DepartmentRepository departmentRepository;
    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private LearningService learningService;

    private User employee;
    private Course course;

    @BeforeEach
    void setUp() {
        employee = new User();
        employee.setFullName("Alice Employee");
        employee.setEmail("alice@example.com");
        employee.setRole(Role.EMPLOYEE);
        ReflectionTestUtils.setField(employee, "id", 101L);

        SkillCategory cat = new SkillCategory("Technical", "Technical skills");
        ReflectionTestUtils.setField(cat, "id", 1L);

        Skill skill = new Skill(cat, "Java", "Programming");
        ReflectionTestUtils.setField(skill, "id", 1L);

        course = new Course(
                "Enterprise Java 17",
                "Advanced Java course",
                skill,
                "INTERMEDIATE",
                40,
                "Coursera",
                "https://www.coursera.org/learn/java"
        );
        ReflectionTestUtils.setField(course, "id", 201L);
    }

    @Test
    @DisplayName("enrollInCourse creates new EmployeeLearning record and sends notification")
    void testEnrollInCourse_Success() {
        when(userRepository.findByEmail("alice@example.com")).thenReturn(Optional.of(employee));
        when(courseRepository.findById(201L)).thenReturn(Optional.of(course));
        when(employeeLearningRepository.findByUserIdAndCourseId(101L, 201L)).thenReturn(Optional.empty());
        when(employeeLearningRepository.save(any(EmployeeLearning.class))).thenAnswer(invocation -> invocation.getArgument(0));

        EmployeeLearningDto result = learningService.enrollInCourse("alice@example.com", 201L);

        assertNotNull(result);
        assertEquals("IN_PROGRESS", result.status());
        assertEquals(0, result.progressPct());
        verify(notificationService).createNotification(eq(employee), eq("COURSE_ENROLLMENT"), any(), any(), any());
    }

    @Test
    @DisplayName("updateProgress increments progress and does not issue certificate below 100%")
    void testUpdateProgress_Partial() {
        EmployeeLearning el = new EmployeeLearning(employee, course, "IN_PROGRESS", 20);

        when(userRepository.findByEmail("alice@example.com")).thenReturn(Optional.of(employee));
        when(employeeLearningRepository.findByUserIdAndCourseId(101L, 201L)).thenReturn(Optional.of(el));
        when(employeeLearningRepository.save(any(EmployeeLearning.class))).thenAnswer(inv -> inv.getArgument(0));

        EmployeeLearningDto result = learningService.updateProgress("alice@example.com", 201L, 75);

        assertNotNull(result);
        assertEquals(75, result.progressPct());
        assertEquals("IN_PROGRESS", result.status());
        verify(certificateRepository, never()).save(any());
    }

    @Test
    @DisplayName("updateProgress at 100% completes course and issues certificate")
    void testUpdateProgress_CompletionIssuesCertificate() {
        EmployeeLearning el = new EmployeeLearning(employee, course, "IN_PROGRESS", 80);

        when(userRepository.findByEmail("alice@example.com")).thenReturn(Optional.of(employee));
        when(employeeLearningRepository.findByUserIdAndCourseId(101L, 201L)).thenReturn(Optional.of(el));
        when(certificateRepository.findByUserId(101L)).thenReturn(Collections.emptyList());
        when(employeeLearningRepository.save(any(EmployeeLearning.class))).thenAnswer(inv -> inv.getArgument(0));

        EmployeeLearningDto result = learningService.updateProgress("alice@example.com", 201L, 100);

        assertNotNull(result);
        assertEquals(100, result.progressPct());
        assertEquals("COMPLETED", result.status());
        assertNotNull(el.getCompletedAt());
        verify(certificateRepository).save(any(Certificate.class));
        verify(notificationService).createNotification(eq(employee), eq("CERTIFICATE_EARNED"), any(), any(), any());
    }

    @Test
    @DisplayName("updateProgress prevents certificate duplication if already issued")
    void testUpdateProgress_PreventsDuplicateCertificate() {
        EmployeeLearning el = new EmployeeLearning(employee, course, "IN_PROGRESS", 90);
        Certificate existingCert = new Certificate(employee, course, "CERT-ALREADY", "Java Cert", LocalDate.now(), LocalDate.now().plusYears(1), null);

        when(userRepository.findByEmail("alice@example.com")).thenReturn(Optional.of(employee));
        when(employeeLearningRepository.findByUserIdAndCourseId(101L, 201L)).thenReturn(Optional.of(el));
        when(certificateRepository.findByUserId(101L)).thenReturn(List.of(existingCert));
        when(employeeLearningRepository.save(any(EmployeeLearning.class))).thenAnswer(inv -> inv.getArgument(0));

        EmployeeLearningDto result = learningService.updateProgress("alice@example.com", 201L, 100);

        assertNotNull(result);
        assertEquals(100, result.progressPct());
        verify(certificateRepository, never()).save(any(Certificate.class));
    }

    @Test
    @DisplayName("updateProgress throws NOT_FOUND when updating non-enrolled course")
    void testUpdateProgress_NotEnrolledThrows() {
        when(userRepository.findByEmail("alice@example.com")).thenReturn(Optional.of(employee));
        when(employeeLearningRepository.findByUserIdAndCourseId(101L, 999L)).thenReturn(Optional.empty());

        assertThrows(ApiException.class, () -> learningService.updateProgress("alice@example.com", 999L, 50));
    }

    @Test
    @DisplayName("getMyCertificates returns mapped certificate records for user")
    void testGetMyCertificates() {
        Certificate cert = new Certificate(employee, course, "CERT-12345678", "Enterprise Java 17 Certificate", LocalDate.now(), LocalDate.now().plusYears(2), "https://certs/123");
        ReflectionTestUtils.setField(cert, "id", 501L);

        when(userRepository.findByEmail("alice@example.com")).thenReturn(Optional.of(employee));
        when(certificateRepository.findByUserId(101L)).thenReturn(List.of(cert));

        List<CertificateDto> certs = learningService.getMyCertificates("alice@example.com");

        assertEquals(1, certs.size());
        assertEquals("CERT-12345678", certs.get(0).certificateCode());
        assertEquals("Enterprise Java 17 Certificate", certs.get(0).title());
    }
}
