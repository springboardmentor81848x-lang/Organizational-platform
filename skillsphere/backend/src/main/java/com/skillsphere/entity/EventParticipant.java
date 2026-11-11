package com.skillsphere.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "event_participants", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"event_id", "user_id"})
})
public class EventParticipant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "event_id", nullable = false)
    private CalendarEvent event;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, length = 20)
    private String status = "INVITED"; // INVITED, ACCEPTED, DECLINED

    public EventParticipant() {}

    public EventParticipant(CalendarEvent event, User user) {
        this.event = event;
        this.user = user;
        this.status = "INVITED";
    }

    public Long getId() { return id; }
    public CalendarEvent getEvent() { return event; }
    public void setEvent(CalendarEvent event) { this.event = event; }
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
