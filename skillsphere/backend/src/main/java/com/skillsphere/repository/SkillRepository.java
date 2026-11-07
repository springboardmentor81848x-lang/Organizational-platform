package com.skillsphere.repository;

import com.skillsphere.entity.Skill;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface SkillRepository extends JpaRepository<Skill, Long> {
    Optional<Skill> findByName(String name);
    Optional<Skill> findByNameIgnoreCase(String name);
    List<Skill> findByCategoryId(Long categoryId);
    boolean existsByNameIgnoreCase(String name);
}
