package com.skillsphere.repository;

import com.skillsphere.entity.SkillEvidence;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface SkillEvidenceRepository extends JpaRepository<SkillEvidence, Long> {
    List<SkillEvidence> findByEmployeeSkillId(Long employeeSkillId);
    List<SkillEvidence> findByStatus(String status);
}
