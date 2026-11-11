package com.skillsphere.repository;

import com.skillsphere.entity.PlatformFeedback;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface PlatformFeedbackRepository extends JpaRepository<PlatformFeedback, Long> {
    @Query("select pf from PlatformFeedback pf left join fetch pf.user order by pf.createdAt desc")
    List<PlatformFeedback> findAllWithUser();
}
