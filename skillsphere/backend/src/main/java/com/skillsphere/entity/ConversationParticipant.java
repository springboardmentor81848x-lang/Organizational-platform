package com.skillsphere.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "conversation_participants", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"conversation_id", "user_id"})
})
public class ConversationParticipant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "conversation_id", nullable = false)
    private Conversation conversation;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "joined_at", nullable = false, updatable = false)
    private Instant joinedAt = Instant.now();

    @Column(name = "last_read_at")
    private Instant lastReadAt;

    @Column(name = "is_muted", nullable = false)
    private boolean isMuted = false;

    @Column(name = "is_archived", nullable = false)
    private boolean isArchived = false;

    public ConversationParticipant() {}

    public ConversationParticipant(Conversation conversation, User user) {
        this.conversation = conversation;
        this.user = user;
        this.joinedAt = Instant.now();
    }

    public Long getId() { return id; }
    public Conversation getConversation() { return conversation; }
    public void setConversation(Conversation conversation) { this.conversation = conversation; }
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    public Instant getJoinedAt() { return joinedAt; }
    public Instant getLastReadAt() { return lastReadAt; }
    public void setLastReadAt(Instant lastReadAt) { this.lastReadAt = lastReadAt; }
    public boolean isMuted() { return isMuted; }
    public void setMuted(boolean muted) { this.isMuted = muted; }
    public boolean isArchived() { return isArchived; }
    public void setArchived(boolean archived) { this.isArchived = archived; }
}
