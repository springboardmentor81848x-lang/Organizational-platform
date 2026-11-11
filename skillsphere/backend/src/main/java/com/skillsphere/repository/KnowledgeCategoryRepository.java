package com.skillsphere.repository;

import com.skillsphere.entity.KnowledgeCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface KnowledgeCategoryRepository extends JpaRepository<KnowledgeCategory, Long> {
    Optional<KnowledgeCategory> findByName(String name);
}
