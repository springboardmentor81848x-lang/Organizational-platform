package com.skillsphere.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "team_memberships", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"team_id", "user_id"})
})
public class TeamMembership {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "team_id", nullable = false)
    private Team team;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "role_in_team", length = 50)
    private String roleInTeam = "MEMBER";

    @Column(name = "joined_at", nullable = false, updatable = false)
    private Instant joinedAt = Instant.now();

    public TeamMembership() {}

    public TeamMembership(Team team, User user) {
        this.team = team;
        this.user = user;
        this.roleInTeam = "MEMBER";
        this.joinedAt = Instant.now();
    }

    public TeamMembership(Team team, User user, String roleInTeam) {
        this.team = team;
        this.user = user;
        this.roleInTeam = roleInTeam != null ? roleInTeam : "MEMBER";
        this.joinedAt = Instant.now();
    }

    public Long getId() { return id; }
    public Team getTeam() { return team; }
    public void setTeam(Team team) { this.team = team; }
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    public String getRoleInTeam() { return roleInTeam != null ? roleInTeam : "MEMBER"; }
    public void setRoleInTeam(String roleInTeam) { this.roleInTeam = roleInTeam; }
    public Instant getJoinedAt() { return joinedAt; }
}
