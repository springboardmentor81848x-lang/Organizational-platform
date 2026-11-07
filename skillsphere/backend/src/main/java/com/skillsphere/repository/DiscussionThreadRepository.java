package com.skillsphere.repository;

import com.skillsphere.entity.DiscussionThread;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface DiscussionThreadRepository extends JpaRepository<DiscussionThread, Long> {
    @Query("select t from DiscussionThread t join fetch t.author order by t.updatedAt desc")
    List<DiscussionThread> findAllWithAuthor();

    @Query("select t from DiscussionThread t join fetch t.author " +
           "where lower(t.title) like lower(concat('%', :q, '%')) or lower(t.content) like lower(concat('%', :q, '%')) order by t.updatedAt desc")
    List<DiscussionThread> search(@Param("q") String query);
}
