package com.skillsphere.repository;

import com.skillsphere.entity.SkillGap;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface SkillGapRepository extends JpaRepository<SkillGap, Long> {
    @Query("select sg from SkillGap sg join fetch sg.skill s join fetch s.category where sg.user.id = :userId")
    List<SkillGap> findByUserId(@Param("userId") Long userId);

    Optional<SkillGap> findByUserIdAndSkillId(Long userId, Long skillId);

    @Query("select sg from SkillGap sg join fetch sg.user join fetch sg.skill where sg.user.id in :userIds")
    List<SkillGap> findByUserIds(@Param("userIds") List<Long> userIds);

    void deleteByUserId(Long userId);
}
