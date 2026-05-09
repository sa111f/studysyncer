package com.studysyncer.backend.repository;

import com.studysyncer.backend.domain.ExamTopic;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ExamTopicRepository extends JpaRepository<ExamTopic, Long> {
}
