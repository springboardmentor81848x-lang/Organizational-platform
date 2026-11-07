package com.skillsphere.service;

import com.skillsphere.dto.*;
import com.skillsphere.entity.*;
import com.skillsphere.exception.ApiException;
import com.skillsphere.repository.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
public class MentorshipRequestService {

    private final MentorshipRequestRepository requestRepository;
    private final MentorAssignmentRepository assignmentRepository;
    private final SharedResourceRepository sharedResourceRepository;
    private final UserRepository userRepository;
    private final EmployeeProfileRepository profileRepository;
    private final NotificationService notificationService;

    public MentorshipRequestService(MentorshipRequestRepository requestRepository,
                                    MentorAssignmentRepository assignmentRepository,
                                    SharedResourceRepository sharedResourceRepository,
                                    UserRepository userRepository,
                                    EmployeeProfileRepository profileRepository,
                                    NotificationService notificationService) {
        this.requestRepository = requestRepository;
        this.assignmentRepository = assignmentRepository;
        this.sharedResourceRepository = sharedResourceRepository;
        this.userRepository = userRepository;
        this.profileRepository = profileRepository;
        this.notificationService = notificationService;
    }

    @Transactional
    public MentorshipRequestResponseDto createRequest(String employeeEmail, CreateMentorshipRequestDto dto) {
        User employee = getUserByEmail(employeeEmail);
        User mentor = userRepository.findById(dto.mentorId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Mentor not found."));

        if (employee.getId().equals(mentor.getId())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "You cannot request mentorship from yourself.");
        }

        if (requestRepository.existsByMentorIdAndEmployeeIdAndStatus(mentor.getId(), employee.getId(), "PENDING")) {
            throw new ApiException(HttpStatus.CONFLICT, "A pending mentorship request to this mentor already exists.");
        }

        if (assignmentRepository.existsByMentorIdAndEmployeeIdAndStatus(mentor.getId(), employee.getId(), AssignmentStatus.ACTIVE)) {
            throw new ApiException(HttpStatus.CONFLICT, "You are already actively paired with this mentor.");
        }

        MentorshipRequest request = new MentorshipRequest(mentor, employee, dto.message());
        MentorshipRequest saved = requestRepository.save(request);

        notificationService.createNotification(
                mentor,
                "MENTORSHIP_REQUEST",
                "New Mentorship Request",
                employee.getFullName() + " has sent you a mentorship request.",
                "/dashboard/mentor"
        );

        return toResponseDto(saved);
    }

    @Transactional(readOnly = true)
    public List<MentorshipRequestResponseDto> getMySentRequests(String employeeEmail) {
        User employee = getUserByEmail(employeeEmail);
        return requestRepository.findByEmployeeIdOrderByCreatedAtDesc(employee.getId())
                .stream().map(this::toResponseDto).toList();
    }

    @Transactional(readOnly = true)
    public List<MentorshipRequestResponseDto> getMyIncomingRequests(String mentorEmail) {
        User mentor = getUserByEmail(mentorEmail);
        return requestRepository.findByMentorIdOrderByCreatedAtDesc(mentor.getId())
                .stream().map(this::toResponseDto).toList();
    }

    @Transactional
    public MentorshipRequestResponseDto respondToRequest(String mentorEmail, Long requestId, RespondMentorshipRequestDto dto) {
        User mentor = getUserByEmail(mentorEmail);
        MentorshipRequest request = requestRepository.findById(requestId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Mentorship request not found."));

        if (!request.getMentor().getId().equals(mentor.getId())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "You are not authorized to respond to this request.");
        }

        if (!"PENDING".equalsIgnoreCase(request.getStatus())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "This request has already been answered.");
        }

        request.setStatus(dto.status());
        request.setResponseNote(dto.responseNote());
        request.setRespondedAt(Instant.now());
        MentorshipRequest saved = requestRepository.save(request);

        if ("ACCEPTED".equalsIgnoreCase(dto.status())) {
            // Check if active assignment already exists for this pair
            boolean exists = assignmentRepository.existsByMentorIdAndEmployeeIdAndStatus(
                    mentor.getId(), request.getEmployee().getId(), AssignmentStatus.ACTIVE);
            if (!exists) {
                MentorAssignment assignment = new MentorAssignment(
                        mentor,
                        request.getEmployee(),
                        mentor,
                        "Mentorship request accepted: " + (dto.responseNote() != null ? dto.responseNote() : "Welcome!")
                );
                assignmentRepository.save(assignment);
            }
        }

        notificationService.createNotification(
                request.getEmployee(),
                "MENTORSHIP_RESPONSE",
                "Mentorship Request " + dto.status(),
                mentor.getFullName() + " has " + dto.status().toLowerCase() + " your mentorship request.",
                "/dashboard/employee"
        );

        return toResponseDto(saved);
    }

    @Transactional
    public MentorshipRequestResponseDto cancelRequest(String employeeEmail, Long requestId) {
        User employee = getUserByEmail(employeeEmail);
        MentorshipRequest request = requestRepository.findById(requestId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Mentorship request not found."));

        if (!request.getEmployee().getId().equals(employee.getId())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "You are not authorized to cancel this request.");
        }

        if (!"PENDING".equalsIgnoreCase(request.getStatus())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Only pending requests can be cancelled.");
        }

        request.setStatus("CANCELLED");
        request.setRespondedAt(Instant.now());
        request.setResponseNote("Cancelled by employee.");
        MentorshipRequest saved = requestRepository.save(request);

        return toResponseDto(saved);
    }

    @Transactional
    public SharedResourceDto shareResource(String userEmail, SharedResourceDto.CreateRequest dto) {
        User user = getUserByEmail(userEmail);
        MentorAssignment assignment = assignmentRepository.findById(dto.assignmentId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Mentorship assignment not found."));

        boolean isMentor = assignment.getMentor().getId().equals(user.getId());
        boolean isEmployee = assignment.getEmployee().getId().equals(user.getId());

        if (!isMentor && !isEmployee) {
            throw new ApiException(HttpStatus.FORBIDDEN, "You are not a participant in this mentorship relationship.");
        }

        SharedResource resource = new SharedResource(assignment, user, dto.title().trim(), dto.urlOrContent().trim());
        SharedResource saved = sharedResourceRepository.save(resource);

        User recipient = isMentor ? assignment.getEmployee() : assignment.getMentor();
        notificationService.createNotification(
                recipient,
                "RESOURCE_SHARED",
                "New Shared Resource",
                user.getFullName() + " shared a resource: " + dto.title(),
                isMentor ? "/dashboard/employee" : "/dashboard/mentor"
        );

        return new SharedResourceDto(
                saved.getId(),
                assignment.getId(),
                saved.getTitle(),
                saved.getUrlOrContent(),
                user.getId(),
                user.getFullName(),
                saved.getCreatedAt()
        );
    }

    @Transactional(readOnly = true)
    public List<SharedResourceDto> getResourcesForAssignment(String userEmail, Long assignmentId) {
        User user = getUserByEmail(userEmail);
        MentorAssignment assignment = assignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Mentorship assignment not found."));

        boolean isMentor = assignment.getMentor().getId().equals(user.getId());
        boolean isEmployee = assignment.getEmployee().getId().equals(user.getId());

        if (!isMentor && !isEmployee) {
            throw new ApiException(HttpStatus.FORBIDDEN, "You are not a participant in this mentorship relationship.");
        }

        return sharedResourceRepository.findByAssignmentIdOrderByCreatedAtDesc(assignmentId)
                .stream().map(r -> new SharedResourceDto(
                        r.getId(),
                        assignmentId,
                        r.getTitle(),
                        r.getUrlOrContent(),
                        r.getSharedBy().getId(),
                        r.getSharedBy().getFullName(),
                        r.getCreatedAt()
                )).toList();
    }

    private MentorshipRequestResponseDto toResponseDto(MentorshipRequest r) {
        String mentorHeadline = profileRepository.findByUserId(r.getMentor().getId())
                .map(p -> p.getDepartment() + " • " + p.getExperienceLevel())
                .orElse("Mentor");

        String empRole = profileRepository.findByUserId(r.getEmployee().getId())
                .map(EmployeeProfile::getJobRole)
                .orElse(r.getEmployee().getRequestedRole() != null ? r.getEmployee().getRequestedRole().name() : "Employee");

        return new MentorshipRequestResponseDto(
                r.getId(),
                r.getMentor().getId(),
                r.getMentor().getFullName(),
                r.getMentor().getEmail(),
                mentorHeadline,
                r.getEmployee().getId(),
                r.getEmployee().getFullName(),
                r.getEmployee().getEmail(),
                empRole,
                r.getMessage(),
                r.getStatus(),
                r.getCreatedAt(),
                r.getRespondedAt(),
                r.getResponseNote()
        );
    }

    private User getUserByEmail(String email) {
        return userRepository.findByEmail(email.trim().toLowerCase())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "User not found."));
    }
}
