package com.skillsphere.repository;

import com.skillsphere.entity.HrAnnouncementRead;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface HrAnnouncementReadRepository extends JpaRepository<HrAnnouncementRead, Long> {
    boolean existsByAnnouncementIdAndUserId(Long announcementId, Long userId);
    Optional<HrAnnouncementRead> findByAnnouncementIdAndUserId(Long announcementId, Long userId);
    long countByAnnouncementId(Long announcementId);
}
