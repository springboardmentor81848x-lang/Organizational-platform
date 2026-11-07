package com.skillsphere.repository;

import com.skillsphere.entity.SkillCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface SkillCategoryRepository extends JpaRepository<SkillCategory, Long> {
    Optional<SkillCategory> findByName(String name);
}
