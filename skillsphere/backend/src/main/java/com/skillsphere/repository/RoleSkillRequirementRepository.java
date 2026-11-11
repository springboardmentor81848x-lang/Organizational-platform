package com.skillsphere.repository;

import com.skillsphere.entity.RoleSkillRequirement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface RoleSkillRequirementRepository extends JpaRepository<RoleSkillRequirement, Long> {
    @Query("select r from RoleSkillRequirement r join fetch r.skill where r.jobRole = :jobRole")
    List<RoleSkillRequirement> findByJobRole(@Param("jobRole") String jobRole);

    Optional<RoleSkillRequirement> findByJobRoleAndSkillId(String jobRole, Long skillId);
}
