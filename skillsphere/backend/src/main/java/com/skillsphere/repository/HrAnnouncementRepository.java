package com.skillsphere.repository;

import com.skillsphere.entity.HrAnnouncement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface HrAnnouncementRepository extends JpaRepository<HrAnnouncement, Long> {

    @Query("select a from HrAnnouncement a join fetch a.author left join fetch a.department order by a.createdAt desc")
    List<HrAnnouncement> findAllWithDetails();

    @Query("select a from HrAnnouncement a join fetch a.author left join fetch a.department " +
           "where a.targetType = 'ORGANIZATION' " +
           "or (a.targetType = 'DEPARTMENT' and a.department.id = :deptId) " +
           "or (a.targetType = 'ROLE' and lower(a.targetRole) = lower(:role)) " +
           "order by a.createdAt desc")
    List<HrAnnouncement> findForUser(@Param("deptId") Long deptId, @Param("role") String role);
}
