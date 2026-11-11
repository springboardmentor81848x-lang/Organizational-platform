package com.skillsphere.repository;

import com.skillsphere.entity.Course;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface CourseRepository extends JpaRepository<Course, Long> {
    List<Course> findBySkillId(Long skillId);
    List<Course> findByLevel(String level);

    @Query("select c from Course c join fetch c.skill s where s.id in :skillIds")
    List<Course> findBySkillIds(@Param("skillIds") List<Long> skillIds);

    @Query("select c from Course c left join fetch c.skill s where c.isActive = true")
    List<Course> findAllActiveWithSkill();

    @Query("select c from Course c left join fetch c.skill s")
    List<Course> findAllWithSkill();
}
