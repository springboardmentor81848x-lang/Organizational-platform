package com.skillsphere.repository;

import com.skillsphere.entity.SupportTicket;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface SupportTicketRepository extends JpaRepository<SupportTicket, Long> {
    @Query("select t from SupportTicket t join fetch t.user where t.user.id = :userId order by t.updatedAt desc")
    List<SupportTicket> findByUserId(@Param("userId") Long userId);

    @Query("select t from SupportTicket t join fetch t.user order by t.updatedAt desc")
    List<SupportTicket> findAllWithUser();
}
