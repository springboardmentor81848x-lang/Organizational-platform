package com.skillsphere.repository;

import com.skillsphere.entity.LearningPathItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface LearningPathItemRepository extends JpaRepository<LearningPathItem, Long> {
    @Query("select item from LearningPathItem item join fetch item.course where item.learningPath.id = :pathId order by item.stepOrder asc")
    List<LearningPathItem> findByLearningPathIdOrderByStepOrderAsc(@Param("pathId") Long pathId);
}
