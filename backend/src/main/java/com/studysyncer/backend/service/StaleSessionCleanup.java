package com.studysyncer.backend.service;

import com.studysyncer.backend.domain.StudySession;
import com.studysyncer.backend.repository.StudySessionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

@Component
public class StaleSessionCleanup {

    private static final Logger log = LoggerFactory.getLogger(StaleSessionCleanup.class);

    private final StudySessionRepository sessionRepo;

    public StaleSessionCleanup(StudySessionRepository sessionRepo) {
        this.sessionRepo = sessionRepo;
    }

    @Scheduled(fixedDelay = 60_000)
    @Transactional
    public void sweep() {
        Instant cutoff = Instant.now().minusSeconds(FocusService.STALE_AFTER_SECONDS);
        List<StudySession> stale = sessionRepo.findByEndedAtIsNullAndLastHeartbeatAtBefore(cutoff);
        if (stale.isEmpty()) return;
        for (StudySession s : stale) {
            Instant end = s.getLastHeartbeatAt() != null ? s.getLastHeartbeatAt() : s.getStartedAt();
            s.setEndedAt(end);
            long secs = Duration.between(s.getStartedAt(), end).getSeconds();
            s.setDurationSeconds((int) Math.max(0, secs));
            sessionRepo.save(s);
        }
        log.info("StaleSessionCleanup finalized {} abandoned session(s)", stale.size());
    }
}
