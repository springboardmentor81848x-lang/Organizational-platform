package com.skillsphere.repository;

import com.skillsphere.entity.MessageReport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MessageReportRepository extends JpaRepository<MessageReport, Long> {
    boolean existsByMessageIdAndReporterId(Long messageId, Long reporterId);
    List<MessageReport> findByStatusOrderByCreatedAtDesc(String status);
}
