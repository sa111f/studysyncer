package com.studysyncer.backend.repository;

import com.studysyncer.backend.domain.Course;
import com.studysyncer.backend.domain.ScheduleItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ScheduleItemRepository extends JpaRepository<ScheduleItem, Long> {
    List<ScheduleItem> findByCourseOrderByDayOfWeekAscStartMinuteAsc(Course course);

    long countByCourse(Course course);

    void deleteByCourse(Course course);
}
