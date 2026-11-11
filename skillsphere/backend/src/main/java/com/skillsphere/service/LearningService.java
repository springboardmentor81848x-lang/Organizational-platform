package com.skillsphere.service;

import com.skillsphere.dto.*;
import com.skillsphere.entity.*;
import com.skillsphere.exception.ApiException;
import com.skillsphere.repository.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.*;

@Service
public class LearningService {

    private final CourseRepository courseRepository;
    private final SkillRepository skillRepository;
    private final EmployeeLearningRepository employeeLearningRepository;
    private final CertificateRepository certificateRepository;
    private final TrainingProgrammeRepository trainingProgrammeRepository;
    private final TrainingEnrollmentRepository trainingEnrollmentRepository;
    private final SkillGapRepository skillGapRepository;
    private final UserRepository userRepository;
    private final DepartmentRepository departmentRepository;
    private final NotificationService notificationService;

    public LearningService(CourseRepository courseRepository,
                           SkillRepository skillRepository,
                           EmployeeLearningRepository employeeLearningRepository,
                           CertificateRepository certificateRepository,
                           TrainingProgrammeRepository trainingProgrammeRepository,
                           TrainingEnrollmentRepository trainingEnrollmentRepository,
                           SkillGapRepository skillGapRepository,
                           UserRepository userRepository,
                           DepartmentRepository departmentRepository,
                           NotificationService notificationService) {
        this.courseRepository = courseRepository;
        this.skillRepository = skillRepository;
        this.employeeLearningRepository = employeeLearningRepository;
        this.certificateRepository = certificateRepository;
        this.trainingProgrammeRepository = trainingProgrammeRepository;
        this.trainingEnrollmentRepository = trainingEnrollmentRepository;
        this.skillGapRepository = skillGapRepository;
        this.userRepository = userRepository;
        this.departmentRepository = departmentRepository;
        this.notificationService = notificationService;
    }

    @Transactional(readOnly = true)
    public List<CourseDto> getAllCourses() {
        return courseRepository.findAll().stream().map(c -> toCourseDto(c, null)).toList();
    }

    @Transactional(readOnly = true)
    public List<CourseDto> getRecommendedCourses(String email) {
        User user = getUserByEmail(email);
        List<SkillGap> gaps = skillGapRepository.findByUserId(user.getId());

        if (gaps.isEmpty()) {
            return courseRepository.findAll().stream().limit(6)
                    .map(c -> toCourseDto(c, "RECOMMENDED")).toList();
        }

        Map<Long, String> skillPriorityMap = new HashMap<>();
        for (SkillGap g : gaps) {
            if ("HIGH".equalsIgnoreCase(g.getPriority()) || "MEDIUM".equalsIgnoreCase(g.getPriority())) {
                skillPriorityMap.put(g.getSkill().getId(), g.getPriority());
            }
        }

        if (skillPriorityMap.isEmpty()) {
            return courseRepository.findAll().stream().limit(6)
                    .map(c -> toCourseDto(c, "ON_TRACK")).toList();
        }

        List<Course> courses = courseRepository.findBySkillIds(new ArrayList<>(skillPriorityMap.keySet()));
        return courses.stream().map(c -> toCourseDto(c, skillPriorityMap.get(c.getSkill().getId()))).toList();
    }

    @Transactional(readOnly = true)
    public List<EmployeeLearningDto> getMyLearning(String email) {
        User user = getUserByEmail(email);
        return employeeLearningRepository.findByUserId(user.getId()).stream().map(this::toEmployeeLearningDto).toList();
    }

    @Transactional
    public EmployeeLearningDto enrollInCourse(String email, Long courseId) {
        User user = getUserByEmail(email);
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Course not found."));

        EmployeeLearning el = employeeLearningRepository.findByUserIdAndCourseId(user.getId(), courseId)
                .orElseGet(() -> new EmployeeLearning(user, course, "IN_PROGRESS", 0));

        if ("NOT_STARTED".equals(el.getStatus())) {
            el.setStatus("IN_PROGRESS");
        }

        EmployeeLearning saved = employeeLearningRepository.save(el);

        notificationService.createNotification(
                user,
                "COURSE_ENROLLMENT",
                "Enrolled: " + course.getTitle(),
                "You have enrolled in " + course.getTitle() + ". Good luck with your learning!",
                "/dashboard/employee"
        );

        return toEmployeeLearningDto(saved);
    }

    @Transactional
    public EmployeeLearningDto updateProgress(String email, Long courseId, int progressPct) {
        User user = getUserByEmail(email);
        EmployeeLearning el = employeeLearningRepository.findByUserIdAndCourseId(user.getId(), courseId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "You are not enrolled in this course."));

        int pct = Math.min(100, Math.max(0, progressPct));
        el.setProgressPct(pct);

        if (pct >= 100) {
            el.setStatus("COMPLETED");
            if (el.getCompletedAt() == null) {
                el.setCompletedAt(Instant.now());
            }

            // Issue certificate if none exists
            List<Certificate> certs = certificateRepository.findByUserId(user.getId());
            boolean hasCert = certs.stream().anyMatch(c -> c.getCourse() != null && c.getCourse().getId().equals(courseId));
            if (!hasCert) {
                String certCode = "CERT-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
                Certificate cert = new Certificate(
                        user,
                        el.getCourse(),
                        certCode,
                        el.getCourse().getTitle() + " Certificate of Completion",
                        LocalDate.now(),
                        LocalDate.now().plusYears(2),
                        null
                );
                certificateRepository.save(cert);

                notificationService.createNotification(
                        user,
                        "CERTIFICATE_EARNED",
                        "Certificate Issued!",
                        "Congratulations! You completed " + el.getCourse().getTitle() + " and earned a certificate.",
                        "/dashboard/employee"
                );
            }
        } else if (pct > 0) {
            el.setStatus("IN_PROGRESS");
        }

        EmployeeLearning saved = employeeLearningRepository.save(el);
        return toEmployeeLearningDto(saved);
    }

    @Transactional(readOnly = true)
    public List<CertificateDto> getMyCertificates(String email) {
        User user = getUserByEmail(email);
        return certificateRepository.findByUserId(user.getId()).stream().map(c -> new CertificateDto(
                c.getId(),
                c.getCourse() != null ? c.getCourse().getId() : null,
                c.getCourse() != null ? c.getCourse().getTitle() : "SkillSphere Specialization",
                c.getCertificateCode(),
                c.getTitle(),
                c.getIssueDate(),
                c.getExpiryDate(),
                c.getCredentialUrl()
        )).toList();
    }

    @Transactional(readOnly = true)
    public List<TrainingProgrammeDto> getAllTrainingProgrammes(String email) {
        User user = getUserByEmail(email);
        List<TrainingEnrollment> userEnrollments = trainingEnrollmentRepository.findByUserId(user.getId());
        Set<Long> enrolledProgrammeIds = new HashSet<>();
        for (TrainingEnrollment te : userEnrollments) {
            enrolledProgrammeIds.add(te.getProgramme().getId());
        }

        return trainingProgrammeRepository.findAll().stream().map(p -> new TrainingProgrammeDto(
                p.getId(),
                p.getTitle(),
                p.getDescription(),
                p.getDepartment() != null ? p.getDepartment().getId() : null,
                p.getDepartment() != null ? p.getDepartment().getName() : "All Departments",
                p.getStartDate(),
                p.getEndDate(),
                p.getCapacity(),
                p.getEnrolledCount(),
                p.getTrainerName(),
                p.getStatus(),
                enrolledProgrammeIds.contains(p.getId())
        )).toList();
    }

    @Transactional
    public TrainingProgrammeDto enrollInTrainingProgramme(String email, Long programmeId) {
        User user = getUserByEmail(email);
        TrainingProgramme programme = trainingProgrammeRepository.findById(programmeId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Training programme not found."));

        if (trainingEnrollmentRepository.findByProgrammeIdAndUserId(programmeId, user.getId()).isPresent()) {
            throw new ApiException(HttpStatus.CONFLICT, "You are already enrolled in this training programme.");
        }

        if (programme.getCapacity() > 0 && programme.getEnrolledCount() >= programme.getCapacity()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Training programme is at full capacity.");
        }

        TrainingEnrollment enrollment = new TrainingEnrollment(programme, user);
        trainingEnrollmentRepository.save(enrollment);

        programme.setEnrolledCount(programme.getEnrolledCount() + 1);
        trainingProgrammeRepository.save(programme);

        notificationService.createNotification(
                user,
                "TRAINING_ENROLLMENT",
                "Enrolled in Training: " + programme.getTitle(),
                "Your seat has been reserved. Starts: " + (programme.getStartDate() != null ? programme.getStartDate() : "TBD"),
                "/dashboard/employee"
        );

        return new TrainingProgrammeDto(
                programme.getId(),
                programme.getTitle(),
                programme.getDescription(),
                programme.getDepartment() != null ? programme.getDepartment().getId() : null,
                programme.getDepartment() != null ? programme.getDepartment().getName() : "All Departments",
                programme.getStartDate(),
                programme.getEndDate(),
                programme.getCapacity(),
                programme.getEnrolledCount(),
                programme.getTrainerName(),
                programme.getStatus(),
                true
        );
    }

    @Transactional
    public CourseDto createCourse(CourseDto dto) {
        Skill skill = skillRepository.findById(dto.skillId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Skill not found."));

        Course course = new Course(
                dto.title().trim(),
                dto.description(),
                skill,
                dto.level(),
                dto.durationHours(),
                dto.provider(),
                dto.courseUrl(),
                dto.thumbnailUrl()
        );
        Course saved = courseRepository.save(course);
        return toCourseDto(saved, null);
    }

    @Transactional
    public TrainingProgrammeDto createTrainingProgramme(TrainingProgrammeDto dto) {
        Department dept = null;
        if (dto.departmentId() != null) {
            dept = departmentRepository.findById(dto.departmentId()).orElse(null);
        }

        TrainingProgramme tp = new TrainingProgramme(
                dto.title().trim(),
                dto.description(),
                dept,
                dto.startDate(),
                dto.endDate(),
                dto.capacity(),
                dto.trainerName(),
                dto.status() != null ? dto.status() : "UPCOMING"
        );
        TrainingProgramme saved = trainingProgrammeRepository.save(tp);

        return new TrainingProgrammeDto(
                saved.getId(),
                saved.getTitle(),
                saved.getDescription(),
                saved.getDepartment() != null ? saved.getDepartment().getId() : null,
                saved.getDepartment() != null ? saved.getDepartment().getName() : "All Departments",
                saved.getStartDate(),
                saved.getEndDate(),
                saved.getCapacity(),
                saved.getEnrolledCount(),
                saved.getTrainerName(),
                saved.getStatus(),
                false
        );
    }

    private CourseDto toCourseDto(Course c, String priority) {
        return new CourseDto(
                c.getId(),
                c.getTitle(),
                c.getDescription(),
                c.getSkill() != null ? c.getSkill().getId() : null,
                c.getSkill() != null ? c.getSkill().getName() : null,
                c.getLevel(),
                c.getDurationHours(),
                c.getProvider(),
                c.getCourseUrl(),
                c.getThumbnailUrl(),
                priority
        );
    }

    private EmployeeLearningDto toEmployeeLearningDto(EmployeeLearning el) {
        Course c = el.getCourse();
        return new EmployeeLearningDto(
                el.getId(),
                c.getId(),
                c.getTitle(),
                c.getSkill() != null ? c.getSkill().getName() : null,
                c.getLevel(),
                c.getDurationHours(),
                c.getProvider(),
                c.getCourseUrl(),
                el.getStatus(),
                el.getProgressPct(),
                el.getEnrolledAt(),
                el.getCompletedAt(),
                el.getExpiresAt()
        );
    }

    private User getUserByEmail(String email) {
        return userRepository.findByEmail(email.trim().toLowerCase())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "User not found."));
    }
}
