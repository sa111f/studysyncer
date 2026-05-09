package com.studysyncer.backend.repository;

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

    long countByUserAndStatus(User user, TaskStatus status);

    long countByUserAndStatusAndCompletedAtBetween(User user, TaskStatus status, Instant start, Instant end);

    long countByUserAndStatusAndDueAtBetween(User user, TaskStatus status, Instant start, Instant end);
}
