package com.studysyncer.backend.repository;

import com.studysyncer.backend.domain.Exam;
import com.studysyncer.backend.domain.ExamStatus;
import com.studysyncer.backend.domain.User;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ExamRepository extends JpaRepository<Exam, Long> {

    @EntityGraph(attributePaths = "course")
    List<Exam> findTop2ByUserAndStatusOrderByStartsAtAsc(User user, ExamStatus status);

    long countByUserAndStatus(User user, ExamStatus status);
}
