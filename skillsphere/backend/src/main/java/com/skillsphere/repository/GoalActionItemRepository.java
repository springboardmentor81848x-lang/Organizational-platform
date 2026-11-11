package com.skillsphere.repository;

import com.skillsphere.entity.GoalActionItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface GoalActionItemRepository extends JpaRepository<GoalActionItem, Long> {
    List<GoalActionItem> findByGoalIdOrderByCreatedAtAsc(Long goalId);
}
