package com.wki.auth.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "employees")
public class Employee {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long employeeId;

    private Long departmentId;
    private Long roleId;
    private Long designationId;
    private String firstName;
    private String lastName;

    @Column(unique = true)
    private String email;

    private String passwordHash;
    private String status = "ACTIVE";

    /** Set on every successful login (column last_login_at, added automatically at startup). */
    private LocalDateTime lastLoginAt;

    public LocalDateTime getLastLoginAt() { return lastLoginAt; }
    public void setLastLoginAt(LocalDateTime v) { this.lastLoginAt = v; }

    public Long getEmployeeId() { return employeeId; }
    public Long getDepartmentId() { return departmentId; }
    public void setDepartmentId(Long v) { this.departmentId = v; }
    public Long getRoleId() { return roleId; }
    public void setRoleId(Long v) { this.roleId = v; }
    public Long getDesignationId() { return designationId; }
    public void setDesignationId(Long v) { this.designationId = v; }
    public String getFirstName() { return firstName; }
    public void setFirstName(String v) { this.firstName = v; }
    public String getLastName() { return lastName; }
    public void setLastName(String v) { this.lastName = v; }
    public String getEmail() { return email; }
    public void setEmail(String v) { this.email = v; }
    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String v) { this.passwordHash = v; }
    public String getStatus() { return status; }
    public void setStatus(String v) { this.status = v; }
}
