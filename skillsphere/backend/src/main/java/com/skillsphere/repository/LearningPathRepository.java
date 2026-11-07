package com.skillsphere.repository;

import com.skillsphere.entity.LearningPath;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LearningPathRepository extends JpaRepository<LearningPath, Long> {
    List<LearningPath> findByTargetRole(String targetRole);

    @Query("select lp from LearningPath lp left join fetch lp.skill left join fetch lp.createdBy order by lp.createdAt desc")
    List<LearningPath> findAllWithDetails();
}
