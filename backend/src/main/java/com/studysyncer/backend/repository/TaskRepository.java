package com.studysyncer.backend.repository;

import com.studysyncer.backend.domain.Course;
import com.studysyncer.backend.domain.Task;
import com.studysyncer.backend.domain.TaskStatus;
import com.studysyncer.backend.domain.User;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;

public interface TaskRepository extends JpaRepository<Task, Long> {

    @EntityGraph(attributePaths = "course")
    List<Task> findByUserAndStatusAndDueAtBeforeOrderByDueAtAsc(User user, TaskStatus status, Instant cutoff);

    @EntityGraph(attributePaths = "course")
    List<Task> findByUserAndStatusAndDueAtBetweenOrderByDueAtAsc(User user, TaskStatus status, Instant start, Instant end);

    @EntityGraph(attributePaths = "course")
    List<Task> findByUserOrderByDueAtAsc(User user);

    @EntityGraph(attributePaths = "course")
    List<Task> findByUserAndStatusOrderByDueAtAsc(User user, TaskStatus status);

    @EntityGraph(attributePaths = "course")
    List<Task> findByUserAndStatusAndDueAtGreaterThanEqualOrderByDueAtAsc(User user, TaskStatus status, Instant from);

    @EntityGraph(attributePaths = "course")
    List<Task> findByUserAndStatusAndCompletedAtBetweenOrderByCompletedAtDesc(User user, TaskStatus status, Instant a, Instant b);

    long countByUser(User user);

    long countByUserAndStatus(User user, TaskStatus status);

    long countByUserAndCourseAndStatus(User user, Course course, TaskStatus status);

    long countByUserAndCourseAndStatusAndDueAtBetween(User user, Course course, TaskStatus status, Instant start, Instant end);

    long countByUserAndCourseAndStatusAndDueAtBefore(User user, Course course, TaskStatus status, Instant cutoff);

    long countByUserAndCourse(User user, Course course);

    long countByUserAndStatusAndCompletedAtBetween(User user, TaskStatus status, Instant start, Instant end);

    long countByUserAndStatusAndDueAtBetween(User user, TaskStatus status, Instant start, Instant end);

    long countByUserAndStatusAndDueAtBefore(User user, TaskStatus status, Instant cutoff);

    void deleteByUser(User user);
}
