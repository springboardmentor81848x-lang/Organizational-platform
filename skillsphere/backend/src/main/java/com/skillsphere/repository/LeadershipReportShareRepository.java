package com.skillsphere.repository;

import com.skillsphere.entity.LeadershipReportShare;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LeadershipReportShareRepository extends JpaRepository<LeadershipReportShare, Long> {

    @Query("select s from LeadershipReportShare s join fetch s.sharedBy left join fetch s.department order by s.createdAt desc")
    List<LeadershipReportShare> findAllWithDetails();

    @Query("select s from LeadershipReportShare s join fetch s.sharedBy left join fetch s.department " +
           "where s.sharedWithRole in (:roles) " +
           "or (:deptId is not null and s.department.id = :deptId) " +
           "order by s.createdAt desc")
    List<LeadershipReportShare> findForLeadership(@Param("roles") List<String> roles, @Param("deptId") Long deptId);
}
