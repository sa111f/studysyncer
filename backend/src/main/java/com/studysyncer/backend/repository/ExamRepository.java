package com.studysyncer.backend.repository;

import com.studysyncer.backend.domain.Exam;
import com.studysyncer.backend.domain.ExamStatus;
import com.studysyncer.backend.domain.User;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;

public interface ExamRepository extends JpaRepository<Exam, Long> {

    @EntityGraph(attributePaths = "course")
    List<Exam> findTop2ByUserAndStatusOrderByStartsAtAsc(User user, ExamStatus status);

    @EntityGraph(attributePaths = "course")
    List<Exam> findByUserAndStatusOrderByStartsAtAsc(User user, ExamStatus status);

    @EntityGraph(attributePaths = "course")
    List<Exam> findByUserAndStatusOrderByStartsAtDesc(User user, ExamStatus status);

    @EntityGraph(attributePaths = "course")
    List<Exam> findByUserOrderByStartsAtAsc(User user);

    @EntityGraph(attributePaths = "course")
    List<Exam> findByUserAndStartsAtBetweenOrderByStartsAtAsc(User user, Instant start, Instant end);

    long countByUser(User user);

    long countByUserAndStatus(User user, ExamStatus status);

    void deleteByUser(User user);
}
