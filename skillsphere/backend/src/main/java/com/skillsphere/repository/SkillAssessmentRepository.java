package com.skillsphere.repository;

import com.skillsphere.entity.SkillAssessment;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface SkillAssessmentRepository extends JpaRepository<SkillAssessment, Long> {
    List<SkillAssessment> findByTargetRole(String targetRole);
    List<SkillAssessment> findByCategoryId(Long categoryId);
}
