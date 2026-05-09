package com.studysyncer.backend.repository;

import com.studysyncer.backend.domain.SessionType;
import com.studysyncer.backend.domain.StudySession;
import com.studysyncer.backend.domain.User;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface StudySessionRepository extends JpaRepository<StudySession, Long> {

    @EntityGraph(attributePaths = "course")
    List<StudySession> findByUserAndStartedAtBetween(User user, Instant start, Instant end);

    @EntityGraph(attributePaths = "course")
    List<StudySession> findByUserAndStartedAtBetweenOrderByStartedAtDesc(User user, Instant start, Instant end);

    @EntityGraph(attributePaths = "course")
    Optional<StudySession> findFirstByUserAndEndedAtIsNullOrderByStartedAtDesc(User user);

    @EntityGraph(attributePaths = {"course", "task"})
    Optional<StudySession> findFirstByUserAndEndedAtIsNullAndSessionTypeOrderByStartedAtDesc(User user, SessionType sessionType);

    List<StudySession> findByEndedAtIsNullAndLastHeartbeatAtBefore(Instant cutoff);

    long countByUser(User user);

    void deleteByUser(User user);
}
