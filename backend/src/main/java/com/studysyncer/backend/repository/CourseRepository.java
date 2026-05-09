package com.studysyncer.backend.repository;

import com.studysyncer.backend.domain.Course;
import com.studysyncer.backend.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CourseRepository extends JpaRepository<Course, Long> {
    List<Course> findByUserOrderByDisplayOrderAsc(User user);

    Optional<Course> findByUserAndCode(User user, String code);

    void deleteByUser(User user);
}
