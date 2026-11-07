package com.skillsphere.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "hr_announcement_reads", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"announcement_id", "user_id"})
})
public class HrAnnouncementRead {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "announcement_id", nullable = false)
    private HrAnnouncement announcement;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "read_at", nullable = false, updatable = false)
    private Instant readAt = Instant.now();

    public HrAnnouncementRead() {}

    public HrAnnouncementRead(HrAnnouncement announcement, User user) {
        this.announcement = announcement;
        this.user = user;
        this.readAt = Instant.now();
    }

    public Long getId() { return id; }
    public HrAnnouncement getAnnouncement() { return announcement; }
    public void setAnnouncement(HrAnnouncement announcement) { this.announcement = announcement; }
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    public Instant getReadAt() { return readAt; }
}
