package com.studysyncer.backend.repository;

import com.studysyncer.backend.domain.Exam;
import com.studysyncer.backend.domain.ExamTopic;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ExamTopicRepository extends JpaRepository<ExamTopic, Long> {
    List<ExamTopic> findByExamOrderByDisplayOrderAsc(Exam exam);
}
