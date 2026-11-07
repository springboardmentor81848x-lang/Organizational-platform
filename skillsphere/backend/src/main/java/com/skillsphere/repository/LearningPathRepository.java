package com.skillsphere.repository;

import com.skillsphere.entity.LearningPath;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface LearningPathRepository extends JpaRepository<LearningPath, Long> {
    List<LearningPath> findByTargetRole(String targetRole);
}
