package com.studysyncer.backend.repository;

import com.studysyncer.backend.domain.StudySession;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StudySessionRepository extends JpaRepository<StudySession, Long> {
}
