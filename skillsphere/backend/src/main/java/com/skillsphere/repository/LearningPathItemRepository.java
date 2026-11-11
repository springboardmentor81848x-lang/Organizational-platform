package com.skillsphere.repository;

import com.skillsphere.entity.LearningPathItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LearningPathItemRepository extends JpaRepository<LearningPathItem, Long> {
    @Query("select item from LearningPathItem item join fetch item.course where item.learningPath.id = :pathId order by item.stepOrder asc")
    List<LearningPathItem> findByLearningPathIdOrderByStepOrderAsc(@Param("pathId") Long pathId);

    void deleteByLearningPathId(Long learningPathId);
}
