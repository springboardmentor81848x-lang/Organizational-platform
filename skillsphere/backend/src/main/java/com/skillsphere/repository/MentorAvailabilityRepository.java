package com.skillsphere.repository;

import com.skillsphere.entity.MentorAvailability;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MentorAvailabilityRepository extends JpaRepository<MentorAvailability, Long> {
    List<MentorAvailability> findByMentorIdOrderByDayOfWeekAscStartTimeAsc(Long mentorId);
    void deleteByMentorId(Long mentorId);
}
