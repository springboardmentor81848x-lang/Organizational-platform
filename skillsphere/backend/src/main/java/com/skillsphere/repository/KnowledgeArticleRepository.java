package com.skillsphere.repository;

import com.skillsphere.entity.KnowledgeArticle;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface KnowledgeArticleRepository extends JpaRepository<KnowledgeArticle, Long> {
    @Query("select a from KnowledgeArticle a join fetch a.author left join fetch a.category where a.status = :status order by a.createdAt desc")
    List<KnowledgeArticle> findByStatusOrderByCreatedAtDesc(@Param("status") String status);

    @Query("select a from KnowledgeArticle a join fetch a.author left join fetch a.category " +
           "where a.status = 'PUBLISHED' and (lower(a.title) like lower(concat('%', :q, '%')) or lower(a.content) like lower(concat('%', :q, '%')) or lower(a.tags) like lower(concat('%', :q, '%')))")
    List<KnowledgeArticle> searchPublished(@Param("q") String query);

    List<KnowledgeArticle> findByAuthorId(Long authorId);
}
