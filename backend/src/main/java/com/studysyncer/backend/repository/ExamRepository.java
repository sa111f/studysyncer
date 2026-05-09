package com.studysyncer.backend.repository;

import com.studysyncer.backend.domain.Exam;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ExamRepository extends JpaRepository<Exam, Long> {
}
