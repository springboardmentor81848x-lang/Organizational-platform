package com.skillsphere.repository;

import com.skillsphere.entity.TeamMembership;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface TeamMembershipRepository extends JpaRepository<TeamMembership, Long> {
    List<TeamMembership> findByTeamId(Long teamId);
    List<TeamMembership> findByUserId(Long userId);
    Optional<TeamMembership> findByTeamIdAndUserId(Long teamId, Long userId);
    boolean existsByTeamIdAndUserId(Long teamId, Long userId);

    @Query("select tm from TeamMembership tm join fetch tm.user join fetch tm.team t where t.manager.id = :managerId")
    List<TeamMembership> findByManagerUserId(@Param("managerId") Long managerId);

    @Query("select tm from TeamMembership tm join fetch tm.user join fetch tm.team t where t.department.id = :deptId")
    List<TeamMembership> findByDepartmentId(@Param("deptId") Long deptId);

    @Query("select tm from TeamMembership tm join fetch tm.team t left join fetch t.manager where tm.user.id = :userId")
    List<TeamMembership> findByUserIdWithTeamAndManager(@Param("userId") Long userId);
}
