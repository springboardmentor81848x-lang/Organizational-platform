package com.skillsphere.service;

import com.skillsphere.dto.*;
import com.skillsphere.entity.*;
import com.skillsphere.exception.ApiException;
import com.skillsphere.repository.*;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ChatService {

    private final ConversationRepository conversationRepository;
    private final ConversationParticipantRepository participantRepository;
    private final MessageRepository messageRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;
    private final TeamMembershipRepository teamMembershipRepository;
    private final TeamRepository teamRepository;
    private final MentorAssignmentRepository mentorAssignmentRepository;
    private final MentorshipRequestRepository mentorshipRequestRepository;

    public ChatService(ConversationRepository conversationRepository,
                       ConversationParticipantRepository participantRepository,
                       MessageRepository messageRepository,
                       UserRepository userRepository,
                       NotificationService notificationService,
                       TeamMembershipRepository teamMembershipRepository,
                       TeamRepository teamRepository,
                       MentorAssignmentRepository mentorAssignmentRepository,
                       MentorshipRequestRepository mentorshipRequestRepository) {
        this.conversationRepository = conversationRepository;
        this.participantRepository = participantRepository;
        this.messageRepository = messageRepository;
        this.userRepository = userRepository;
        this.notificationService = notificationService;
        this.teamMembershipRepository = teamMembershipRepository;
        this.teamRepository = teamRepository;
        this.mentorAssignmentRepository = mentorAssignmentRepository;
        this.mentorshipRequestRepository = mentorshipRequestRepository;
    }

    @Transactional(readOnly = true)
    public List<ConversationDto> getUserConversations(String email) {
        User user = getUserByEmail(email);
        List<Conversation> conversations = conversationRepository.findByUserIdOrderByUpdatedAtDesc(user.getId());

        List<ConversationDto> dtos = new ArrayList<>();
        for (Conversation c : conversations) {
            List<ConversationParticipant> participants = participantRepository.findByConversationId(c.getId());
            ConversationParticipant currentParticipant = participants.stream()
                    .filter(p -> p.getUser().getId().equals(user.getId()))
                    .findFirst().orElse(null);

            Instant lastRead = currentParticipant != null && currentParticipant.getLastReadAt() != null
                    ? currentParticipant.getLastReadAt()
                    : Instant.EPOCH;

            long unread = messageRepository.countUnreadMessages(c.getId(), lastRead, user.getId());

            List<Message> recent = messageRepository.findRecentByConversationId(c.getId(), PageRequest.of(0, 1));
            MessageDto latestMsg = recent.isEmpty() ? null : toMessageDto(recent.get(0));

            List<ConversationDto.ParticipantDto> participantDtos = participants.stream().map(p -> new ConversationDto.ParticipantDto(
                    p.getUser().getId(),
                    p.getUser().getFullName(),
                    p.getUser().getEmail(),
                    p.getUser().getRole().name()
            )).toList();

            String displayTitle = c.getTitle();
            if (displayTitle == null || displayTitle.isBlank()) {
                // If DIRECT conversation, title is the other participant's name
                displayTitle = participants.stream()
                        .filter(p -> !p.getUser().getId().equals(user.getId()))
                        .map(p -> p.getUser().getFullName())
                        .findFirst()
                        .orElse("Direct Chat");
            }

            dtos.add(new ConversationDto(
                    c.getId(),
                    c.getType(),
                    displayTitle,
                    c.getUpdatedAt(),
                    participantDtos,
                    latestMsg,
                    unread
            ));
        }

        return dtos;
    }

    @Transactional
    public ConversationDto getOrCreateDirectConversation(String email, Long targetUserId) {
        User caller = getUserByEmail(email);
        User target = userRepository.findById(targetUserId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Recipient user not found."));

        if (caller.getId().equals(target.getId())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "You cannot start a direct conversation with yourself.");
        }

        List<Conversation> callerConvs = conversationRepository.findByUserIdOrderByUpdatedAtDesc(caller.getId());
        for (Conversation c : callerConvs) {
            if ("DIRECT".equalsIgnoreCase(c.getType())) {
                List<ConversationParticipant> participants = participantRepository.findByConversationId(c.getId());
                boolean hasTarget = participants.stream().anyMatch(p -> p.getUser().getId().equals(target.getId()));
                if (hasTarget && participants.size() == 2) {
                    return toConversationDto(c, caller);
                }
            }
        }

        // Verify authorization before creating a new direct conversation
        assertCanInitiateDirectConversation(caller, target);

        // Create new direct conversation
        Conversation conv = new Conversation("DIRECT", null);
        Conversation savedConv = conversationRepository.save(conv);

        ConversationParticipant cp1 = new ConversationParticipant(savedConv, caller);
        ConversationParticipant cp2 = new ConversationParticipant(savedConv, target);
        participantRepository.save(cp1);
        participantRepository.save(cp2);

        return toConversationDto(savedConv, caller);
    }

    private void assertCanInitiateDirectConversation(User caller, User target) {
        // Active mentorship assignment or accepted mentorship request
        boolean isMentorshipPair = mentorAssignmentRepository.existsByMentorIdAndEmployeeIdAndStatus(caller.getId(), target.getId(), AssignmentStatus.ACTIVE)
                || mentorAssignmentRepository.existsByMentorIdAndEmployeeIdAndStatus(target.getId(), caller.getId(), AssignmentStatus.ACTIVE)
                || mentorshipRequestRepository.existsByMentorIdAndEmployeeIdAndStatus(caller.getId(), target.getId(), "ACCEPTED")
                || mentorshipRequestRepository.existsByMentorIdAndEmployeeIdAndStatus(target.getId(), caller.getId(), "ACCEPTED");

        if (isMentorshipPair) {
            return;
        }

        // Shared team membership
        Set<Long> callerTeamIds = teamMembershipRepository.findByUserId(caller.getId()).stream()
                .map(tm -> tm.getTeam().getId()).collect(Collectors.toSet());
        Set<Long> targetTeamIds = teamMembershipRepository.findByUserId(target.getId()).stream()
                .map(tm -> tm.getTeam().getId()).collect(Collectors.toSet());

        boolean sharesTeam = callerTeamIds.stream().anyMatch(targetTeamIds::contains);
        if (sharesTeam) {
            return;
        }

        // Caller manages a team that target belongs to
        boolean callerManagesTarget = teamRepository.findByManagerId(caller.getId()).stream()
                .anyMatch(t -> targetTeamIds.contains(t.getId()));
        if (callerManagesTarget) {
            return;
        }

        // Target manages a team that caller belongs to
        boolean targetManagesCaller = teamRepository.findByManagerId(target.getId()).stream()
                .anyMatch(t -> callerTeamIds.contains(t.getId()));
        if (targetManagesCaller) {
            return;
        }

        throw new ApiException(HttpStatus.FORBIDDEN,
                "Access denied. You can only start direct conversations with team members or assigned mentors/mentees.");
    }

    @Transactional
    public List<MessageDto> getMessages(String email, Long conversationId) {
        User user = getUserByEmail(email);

        // STRICT AUTHORIZATION: Caller must be an active participant in this conversation
        ConversationParticipant participant = participantRepository.findByConversationIdAndUserId(conversationId, user.getId())
                .orElseThrow(() -> new ApiException(HttpStatus.FORBIDDEN, "Access denied. You are not a participant in this conversation."));

        participant.setLastReadAt(Instant.now());
        participantRepository.save(participant);

        return messageRepository.findByConversationIdOrderByCreatedAtAsc(conversationId).stream()
                .map(this::toMessageDto).toList();
    }

    @Transactional
    public MessageDto sendMessage(String email, Long conversationId, SendMessageRequest req) {
        User user = getUserByEmail(email);

        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Conversation not found."));

        // STRICT AUTHORIZATION: Sender must be a participant in this conversation
        boolean isParticipant = participantRepository.existsByConversationIdAndUserId(conversationId, user.getId());
        if (!isParticipant) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Access denied. You cannot send messages in this conversation.");
        }

        Message msg = new Message(
                conversation,
                user,
                req.content().trim(),
                req.attachmentUrl(),
                req.attachmentName()
        );
        Message saved = messageRepository.save(msg);

        conversation.setUpdatedAt(Instant.now());
        conversationRepository.save(conversation);

        // Notify other participants
        List<ConversationParticipant> participants = participantRepository.findByConversationId(conversationId);
        for (ConversationParticipant p : participants) {
            if (!p.getUser().getId().equals(user.getId())) {
                notificationService.createNotification(
                        p.getUser(),
                        "NEW_MESSAGE",
                        "New Message from " + user.getFullName(),
                        req.content().length() > 50 ? req.content().substring(0, 47) + "..." : req.content(),
                        "/dashboard"
                );
            }
        }

        return toMessageDto(saved);
    }

    @Transactional
    public void markAsRead(String email, Long conversationId) {
        User user = getUserByEmail(email);
        participantRepository.findByConversationIdAndUserId(conversationId, user.getId())
                .ifPresent(p -> {
                    p.setLastReadAt(Instant.now());
                    participantRepository.save(p);
                });
    }

    private ConversationDto toConversationDto(Conversation c, User caller) {
        List<ConversationParticipant> participants = participantRepository.findByConversationId(c.getId());
        List<ConversationDto.ParticipantDto> participantDtos = participants.stream().map(p -> new ConversationDto.ParticipantDto(
                p.getUser().getId(),
                p.getUser().getFullName(),
                p.getUser().getEmail(),
                p.getUser().getRole().name()
        )).toList();

        List<Message> recent = messageRepository.findRecentByConversationId(c.getId(), PageRequest.of(0, 1));
        MessageDto latestMsg = recent.isEmpty() ? null : toMessageDto(recent.get(0));

        String title = c.getTitle();
        if (title == null || title.isBlank()) {
            title = participants.stream()
                    .filter(p -> !p.getUser().getId().equals(caller.getId()))
                    .map(p -> p.getUser().getFullName())
                    .findFirst().orElse("Direct Chat");
        }

        return new ConversationDto(c.getId(), c.getType(), title, c.getUpdatedAt(), participantDtos, latestMsg, 0);
    }

    private MessageDto toMessageDto(Message m) {
        return new MessageDto(
                m.getId(),
                m.getConversation().getId(),
                m.getSender().getId(),
                m.getSender().getFullName(),
                m.getContent(),
                m.getAttachmentUrl(),
                m.getAttachmentName(),
                m.getCreatedAt()
        );
    }

    private User getUserByEmail(String email) {
        return userRepository.findByEmail(email.trim().toLowerCase())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "User not found."));
    }
}
