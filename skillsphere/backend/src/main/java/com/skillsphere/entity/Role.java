package com.skillsphere.entity;

public enum Role {
    EMPLOYEE("Employee"),
    MANAGER("Manager"),
    MENTOR("Mentor"),
    HR_SPECIALIST("HR Specialist"),
    DEPARTMENT_HEAD("Department Head"),
    ADMIN("Admin");

    private final String label;

    Role(String label) { this.label = label; }

    public String getLabel() { return label; }
}
