package com.studysyncer.backend.repository;

import com.studysyncer.backend.domain.Course;
import com.studysyncer.backend.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CourseRepository extends JpaRepository<Course, Long> {
    List<Course> findByUserOrderByDisplayOrderAsc(User user);
}
