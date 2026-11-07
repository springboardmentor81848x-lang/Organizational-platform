package com.skillsphere.repository;

import com.skillsphere.entity.SkillEvidence;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface SkillEvidenceRepository extends JpaRepository<SkillEvidence, Long> {
    List<SkillEvidence> findByEmployeeSkillId(Long employeeSkillId);
    List<SkillEvidence> findByStatus(String status);

    @Query("SELECT e FROM SkillEvidence e JOIN FETCH e.employeeSkill es JOIN FETCH es.skill WHERE es.user.id = :userId ORDER BY e.createdAt DESC")
    List<SkillEvidence> findByUserId(@Param("userId") Long userId);
}
