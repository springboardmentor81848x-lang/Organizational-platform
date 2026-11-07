package com.skillsphere.service;

import com.skillsphere.dto.*;
import com.skillsphere.dto.ChatDtos.*;
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
    private final MessageReportRepository messageReportRepository;
    private final EmployeeProfileRepository employeeProfileRepository;

    public ChatService(ConversationRepository conversationRepository,
                       ConversationParticipantRepository participantRepository,
                       MessageRepository messageRepository,
                       UserRepository userRepository,
                       NotificationService notificationService,
                       TeamMembershipRepository teamMembershipRepository,
                       TeamRepository teamRepository,
                       MentorAssignmentRepository mentorAssignmentRepository,
                       MentorshipRequestRepository mentorshipRequestRepository,
                       MessageReportRepository messageReportRepository,
                       EmployeeProfileRepository employeeProfileRepository) {
        this.conversationRepository = conversationRepository;
        this.participantRepository = participantRepository;
        this.messageRepository = messageRepository;
        this.userRepository = userRepository;
        this.notificationService = notificationService;
        this.teamMembershipRepository = teamMembershipRepository;
        this.teamRepository = teamRepository;
        this.mentorAssignmentRepository = mentorAssignmentRepository;
        this.mentorshipRequestRepository = mentorshipRequestRepository;
        this.messageReportRepository = messageReportRepository;
        this.employeeProfileRepository = employeeProfileRepository;
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

            boolean isMuted = currentParticipant != null && currentParticipant.isMuted();
            boolean isArchived = currentParticipant != null && currentParticipant.isArchived();

            long unread = messageRepository.countUnreadMessages(c.getId(), lastRead, user.getId());

            List<Message> recent = messageRepository.findRecentByConversationId(c.getId(), PageRequest.of(0, 1));
            MessageDto latestMsg = recent.isEmpty() ? null : toMessageDto(recent.get(0), participants);

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
                    unread,
                    isMuted,
                    isArchived
            ));
        }

        return dtos;
    }

    @Transactional(readOnly = true)
    public long getGlobalUnreadCount(String email) {
        User user = getUserByEmail(email);
        return messageRepository.countTotalUnreadForUser(user.getId());
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

    @Transactional
    public ConversationDto createTeamConversation(String email, CreateTeamConversationRequest req) {
        User caller = getUserByEmail(email);
        Team team = teamRepository.findById(req.teamId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Team not found."));

        // Only the assigned manager of this team can create team channels
        boolean isManager = team.getManager() != null && team.getManager().getId().equals(caller.getId());
        if (!isManager) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Access denied. Only the assigned manager of this team can create team channels.");
        }

        String title = req.title() != null && !req.title().isBlank()
                ? req.title().trim()
                : team.getName() + " Team Channel";

        Conversation conv = new Conversation("TEAM", title);
        conv.setTeam(team);
        Conversation savedConv = conversationRepository.save(conv);

        // Add manager
        ConversationParticipant managerPart = new ConversationParticipant(savedConv, caller);
        participantRepository.save(managerPart);

        // Add all team members
        List<TeamMembership> memberships = teamMembershipRepository.findByTeamId(team.getId());
        for (TeamMembership tm : memberships) {
            if (!tm.getUser().getId().equals(caller.getId())) {
                participantRepository.save(new ConversationParticipant(savedConv, tm.getUser()));
            }
        }

        return toConversationDto(savedConv, caller);
    }

    @Transactional(readOnly = true)
    public List<ChatContactDto> searchPermittedContacts(String email, String query) {
        User caller = getUserByEmail(email);
        String q = query != null ? query.trim().toLowerCase() : "";

        Set<Long> permittedUserIds = new LinkedHashSet<>();
        Map<Long, String> relationshipMap = new HashMap<>();

        // 1. Mentorship relationships (accepted requests or active assignments)
        List<MentorAssignment> mentorAssignments = mentorAssignmentRepository.findByMentorIdAndStatus(caller.getId(), AssignmentStatus.ACTIVE);
        for (MentorAssignment ma : mentorAssignments) {
            permittedUserIds.add(ma.getEmployee().getId());
            relationshipMap.put(ma.getEmployee().getId(), "MENTEE");
        }
        mentorAssignmentRepository.findByEmployeeIdAndStatus(caller.getId(), AssignmentStatus.ACTIVE).ifPresent(ma -> {
            permittedUserIds.add(ma.getMentor().getId());
            relationshipMap.put(ma.getMentor().getId(), "MENTOR");
        });

        List<MentorshipRequest> mentorReqs = mentorshipRequestRepository.findByMentorIdOrderByCreatedAtDesc(caller.getId());
        for (MentorshipRequest mr : mentorReqs) {
            if ("ACCEPTED".equalsIgnoreCase(mr.getStatus())) {
                permittedUserIds.add(mr.getEmployee().getId());
                relationshipMap.putIfAbsent(mr.getEmployee().getId(), "MENTEE");
            }
        }
        List<MentorshipRequest> menteeReqs = mentorshipRequestRepository.findByEmployeeIdOrderByCreatedAtDesc(caller.getId());
        for (MentorshipRequest mr : menteeReqs) {
            if ("ACCEPTED".equalsIgnoreCase(mr.getStatus())) {
                permittedUserIds.add(mr.getMentor().getId());
                relationshipMap.putIfAbsent(mr.getMentor().getId(), "MENTOR");
            }
        }

        // 2. Team relationships
        List<TeamMembership> myMemberships = teamMembershipRepository.findByUserId(caller.getId());
        for (TeamMembership tm : myMemberships) {
            List<TeamMembership> peers = teamMembershipRepository.findByTeamId(tm.getTeam().getId());
            for (TeamMembership peer : peers) {
                if (!peer.getUser().getId().equals(caller.getId())) {
                    permittedUserIds.add(peer.getUser().getId());
                    relationshipMap.putIfAbsent(peer.getUser().getId(), "TEAM_MEMBER");
                }
            }
            if (tm.getTeam().getManager() != null && !tm.getTeam().getManager().getId().equals(caller.getId())) {
                permittedUserIds.add(tm.getTeam().getManager().getId());
                relationshipMap.putIfAbsent(tm.getTeam().getManager().getId(), "MANAGER");
            }
        }

        // 3. Manager relationships: members of teams managed by caller
        List<Team> managedTeams = teamRepository.findByManagerId(caller.getId());
        for (Team mt : managedTeams) {
            List<TeamMembership> members = teamMembershipRepository.findByTeamId(mt.getId());
            for (TeamMembership tm : members) {
                if (!tm.getUser().getId().equals(caller.getId())) {
                    permittedUserIds.add(tm.getUser().getId());
                    relationshipMap.putIfAbsent(tm.getUser().getId(), "TEAM_MEMBER");
                }
            }
        }

        // 4. Department peers for employees / mentors
        Optional<EmployeeProfile> myProfile = employeeProfileRepository.findByUserId(caller.getId());
        if (myProfile.isPresent() && myProfile.get().getDepartment() != null && !myProfile.get().getDepartment().isBlank()) {
            String myDept = myProfile.get().getDepartment().trim();
            List<EmployeeProfile> allProfiles = employeeProfileRepository.findAllWithUser();
            for (EmployeeProfile ep : allProfiles) {
                if (ep.getUser() != null && !ep.getUser().getId().equals(caller.getId())
                        && ep.getDepartment() != null && ep.getDepartment().equalsIgnoreCase(myDept)) {
                    permittedUserIds.add(ep.getUser().getId());
                    relationshipMap.putIfAbsent(ep.getUser().getId(), "COLLEAGUE");
                }
            }
        }

        if (permittedUserIds.isEmpty()) {
            return Collections.emptyList();
        }

        List<User> targetUsers = userRepository.findAllById(permittedUserIds);
        List<ChatContactDto> results = new ArrayList<>();

        for (User u : targetUsers) {
            if (u.getAccountStatus() != AccountStatus.ACTIVE) continue;

            boolean matchesQuery = q.isEmpty()
                    || (u.getFullName() != null && u.getFullName().toLowerCase().contains(q))
                    || (u.getEmail() != null && u.getEmail().toLowerCase().contains(q));

            if (matchesQuery) {
                String deptName = employeeProfileRepository.findByUserId(u.getId())
                        .map(p -> p.getDepartment() != null ? p.getDepartment() : "")
                        .orElse("");

                String relation = relationshipMap.getOrDefault(u.getId(), "COLLEAGUE");

                results.add(new ChatContactDto(
                        u.getId(),
                        u.getFullName(),
                        u.getEmail(),
                        u.getRole().name(),
                        deptName,
                        relation
                ));
            }
        }

        return results;
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

        // Same department peers
        Optional<EmployeeProfile> callerProf = employeeProfileRepository.findByUserId(caller.getId());
        Optional<EmployeeProfile> targetProf = employeeProfileRepository.findByUserId(target.getId());
        if (callerProf.isPresent() && targetProf.isPresent()
                && callerProf.get().getDepartment() != null
                && targetProf.get().getDepartment() != null
                && !callerProf.get().getDepartment().isBlank()
                && callerProf.get().getDepartment().equalsIgnoreCase(targetProf.get().getDepartment())) {
            return;
        }

        throw new ApiException(HttpStatus.FORBIDDEN,
                "Access denied. You can only start direct conversations with team members, departmental colleagues, or authorized mentors/mentees.");
    }

    @Transactional
    public List<MessageDto> getMessages(String email, Long conversationId) {
        User user = getUserByEmail(email);

        // STRICT AUTHORIZATION: Caller must be an active participant in this conversation
        ConversationParticipant participant = participantRepository.findByConversationIdAndUserId(conversationId, user.getId())
                .orElseThrow(() -> new ApiException(HttpStatus.FORBIDDEN, "Access denied. You are not a participant in this conversation."));

        participant.setLastReadAt(Instant.now());
        participantRepository.save(participant);

        List<ConversationParticipant> participants = participantRepository.findByConversationId(conversationId);

        return messageRepository.findByConversationIdOrderByCreatedAtAsc(conversationId).stream()
                .map(m -> toMessageDto(m, participants)).toList();
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

        // Notify other participants unless conversation is muted by recipient
        List<ConversationParticipant> participants = participantRepository.findByConversationId(conversationId);
        for (ConversationParticipant p : participants) {
            if (!p.getUser().getId().equals(user.getId()) && !p.isMuted()) {
                notificationService.createNotification(
                        p.getUser(),
                        "NEW_MESSAGE",
                        "New Message from " + user.getFullName(),
                        "You received a new message in '" + (conversation.getTitle() != null ? conversation.getTitle() : "Direct Chat") + "'.",
                        "/dashboard"
                );
            }
        }

        return toMessageDto(saved, participants);
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

    @Transactional
    public boolean toggleMute(String email, Long conversationId) {
        User user = getUserByEmail(email);
        ConversationParticipant participant = participantRepository.findByConversationIdAndUserId(conversationId, user.getId())
                .orElseThrow(() -> new ApiException(HttpStatus.FORBIDDEN, "Access denied. You are not a participant in this conversation."));

        participant.setMuted(!participant.isMuted());
        participantRepository.save(participant);
        return participant.isMuted();
    }

    @Transactional
    public boolean toggleArchive(String email, Long conversationId) {
        User user = getUserByEmail(email);
        ConversationParticipant participant = participantRepository.findByConversationIdAndUserId(conversationId, user.getId())
                .orElseThrow(() -> new ApiException(HttpStatus.FORBIDDEN, "Access denied. You are not a participant in this conversation."));

        participant.setArchived(!participant.isArchived());
        participantRepository.save(participant);
        return participant.isArchived();
    }

    @Transactional
    public MessageReportResponse reportMessage(String email, Long messageId, ReportMessageRequest req) {
        User reporter = getUserByEmail(email);

        Message message = messageRepository.findById(messageId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Message not found."));

        boolean isParticipant = participantRepository.existsByConversationIdAndUserId(message.getConversation().getId(), reporter.getId());
        if (!isParticipant) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Access denied. You can only report messages from conversations you participate in.");
        }

        if (messageReportRepository.existsByMessageIdAndReporterId(messageId, reporter.getId())) {
            throw new ApiException(HttpStatus.CONFLICT, "You have already reported this message.");
        }

        MessageReport report = new MessageReport(message, reporter, req.reason().trim(), req.details());
        MessageReport saved = messageReportRepository.save(report);

        return new MessageReportResponse(
                saved.getId(),
                message.getId(),
                reporter.getId(),
                saved.getReason(),
                saved.getStatus(),
                saved.getCreatedAt()
        );
    }

    private ConversationDto toConversationDto(Conversation c, User caller) {
        List<ConversationParticipant> participants = participantRepository.findByConversationId(c.getId());
        List<ConversationDto.ParticipantDto> participantDtos = participants.stream().map(p -> new ConversationDto.ParticipantDto(
                p.getUser().getId(),
                p.getUser().getFullName(),
                p.getUser().getEmail(),
                p.getUser().getRole().name()
        )).toList();

        ConversationParticipant currentPart = participants.stream()
                .filter(p -> p.getUser().getId().equals(caller.getId()))
                .findFirst().orElse(null);

        boolean isMuted = currentPart != null && currentPart.isMuted();
        boolean isArchived = currentPart != null && currentPart.isArchived();

        List<Message> recent = messageRepository.findRecentByConversationId(c.getId(), PageRequest.of(0, 1));
        MessageDto latestMsg = recent.isEmpty() ? null : toMessageDto(recent.get(0), participants);

        String title = c.getTitle();
        if (title == null || title.isBlank()) {
            title = participants.stream()
                    .filter(p -> !p.getUser().getId().equals(caller.getId()))
                    .map(p -> p.getUser().getFullName())
                    .findFirst().orElse("Direct Chat");
        }

        return new ConversationDto(c.getId(), c.getType(), title, c.getUpdatedAt(), participantDtos, latestMsg, 0, isMuted, isArchived);
    }

    private MessageDto toMessageDto(Message m, List<ConversationParticipant> participants) {
        boolean isRead = false;
        if (participants != null && participants.size() >= 2) {
            // For direct or group chat, all other recipients must have read the message
            List<ConversationParticipant> otherParticipants = participants.stream()
                    .filter(p -> !p.getUser().getId().equals(m.getSender().getId()))
                    .toList();

            if (!otherParticipants.isEmpty()) {
                isRead = otherParticipants.stream()
                        .allMatch(p -> p.getLastReadAt() != null && !p.getLastReadAt().isBefore(m.getCreatedAt()));
            }
        }

        return new MessageDto(
                m.getId(),
                m.getConversation().getId(),
                m.getSender().getId(),
                m.getSender().getFullName(),
                m.getContent(),
                m.getAttachmentUrl(),
                m.getAttachmentName(),
                m.getCreatedAt(),
                isRead
        );
    }

    private User getUserByEmail(String email) {
        return userRepository.findByEmail(email.trim().toLowerCase())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "User not found."));
    }
}
