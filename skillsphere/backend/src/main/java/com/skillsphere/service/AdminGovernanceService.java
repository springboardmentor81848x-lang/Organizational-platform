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
public class AdminGovernanceService {

    private final AuditLogRepository auditLogRepository;
    private final SupportTicketRepository supportTicketRepository;
    private final PlatformFeedbackRepository platformFeedbackRepository;
    private final SystemConfigurationRepository systemConfigurationRepository;
    private final UserRepository userRepository;

    public AdminGovernanceService(AuditLogRepository auditLogRepository,
                                  SupportTicketRepository supportTicketRepository,
                                  PlatformFeedbackRepository platformFeedbackRepository,
                                  SystemConfigurationRepository systemConfigurationRepository,
                                  UserRepository userRepository) {
        this.auditLogRepository = auditLogRepository;
        this.supportTicketRepository = supportTicketRepository;
        this.platformFeedbackRepository = platformFeedbackRepository;
        this.systemConfigurationRepository = systemConfigurationRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public void recordAuditLog(User user, String action, String entityName, String entityId, String details, String ipAddress) {
        AuditLog log = new AuditLog(user, action, entityName, entityId, details, ipAddress);
        auditLogRepository.save(log);
    }

    @Transactional(readOnly = true)
    public List<AuditLogDto> getAllAuditLogs() {
        return auditLogRepository.findAllWithUser().stream().map(l -> new AuditLogDto(
                l.getId(),
                l.getUser() != null ? l.getUser().getId() : null,
                l.getUser() != null ? l.getUser().getFullName() : "System",
                l.getAction(),
                l.getEntityName(),
                l.getEntityId(),
                l.getDetails(),
                l.getIpAddress(),
                l.getCreatedAt()
        )).toList();
    }

    @Transactional
    public SupportTicketDto createTicket(String email, SupportTicketDto.CreateRequest req) {
        User user = getUserByEmail(email);
        SupportTicket ticket = new SupportTicket(
                user,
                req.subject().trim(),
                req.description().trim(),
                req.priority() != null ? req.priority().toUpperCase() : "MEDIUM"
        );
        SupportTicket saved = supportTicketRepository.save(ticket);
        return toTicketDto(saved);
    }

    @Transactional(readOnly = true)
    public List<SupportTicketDto> getMyTickets(String email) {
        User user = getUserByEmail(email);
        return supportTicketRepository.findByUserId(user.getId()).stream().map(this::toTicketDto).toList();
    }

    @Transactional(readOnly = true)
    public List<SupportTicketDto> getAllTickets() {
        return supportTicketRepository.findAllWithUser().stream().map(this::toTicketDto).toList();
    }

    @Transactional
    public SupportTicketDto updateTicket(Long ticketId, SupportTicketDto.UpdateRequest req) {
        SupportTicket t = supportTicketRepository.findById(ticketId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Ticket not found."));

        if (req.status() != null && !req.status().isBlank()) {
            t.setStatus(req.status().toUpperCase());
        }
        if (req.adminNotes() != null) {
            t.setAdminNotes(req.adminNotes());
        }
        t.setUpdatedAt(Instant.now());
        SupportTicket saved = supportTicketRepository.save(t);
        return toTicketDto(saved);
    }

    @Transactional
    public PlatformFeedbackDto submitFeedback(String email, PlatformFeedbackDto.CreateRequest req) {
        User user = getUserByEmail(email);
        PlatformFeedback fb = new PlatformFeedback(
                user,
                req.category() != null ? req.category() : "General",
                Math.max(1, Math.min(5, req.rating())),
                req.comments()
        );
        PlatformFeedback saved = platformFeedbackRepository.save(fb);
        return new PlatformFeedbackDto(
                saved.getId(),
                user.getId(),
                user.getFullName(),
                saved.getCategory(),
                saved.getRating(),
                saved.getComments(),
                saved.getCreatedAt()
        );
    }

    @Transactional(readOnly = true)
    public List<PlatformFeedbackDto> getAllFeedback() {
        return platformFeedbackRepository.findAllWithUser().stream().map(fb -> new PlatformFeedbackDto(
                fb.getId(),
                fb.getUser() != null ? fb.getUser().getId() : null,
                fb.getUser() != null ? fb.getUser().getFullName() : "Anonymous",
                fb.getCategory(),
                fb.getRating(),
                fb.getComments(),
                fb.getCreatedAt()
        )).toList();
    }

    @Transactional(readOnly = true)
    public List<SystemConfiguration> getAllConfigurations() {
        return systemConfigurationRepository.findAll();
    }

    @Transactional
    public SystemConfiguration updateConfiguration(String key, String value) {
        SystemConfiguration config = systemConfigurationRepository.findById(key)
                .orElseGet(() -> new SystemConfiguration(key, value, "Custom configuration"));
        config.setConfigValue(value);
        config.setUpdatedAt(Instant.now());
        return systemConfigurationRepository.save(config);
    }

    private SupportTicketDto toTicketDto(SupportTicket t) {
        return new SupportTicketDto(
                t.getId(),
                t.getUser().getId(),
                t.getUser().getFullName(),
                t.getUser().getEmail(),
                t.getSubject(),
                t.getDescription(),
                t.getPriority(),
                t.getStatus(),
                t.getAdminNotes(),
                t.getCreatedAt(),
                t.getUpdatedAt()
        );
    }

    private User getUserByEmail(String email) {
        return userRepository.findByEmail(email.trim().toLowerCase())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "User not found."));
    }
}
