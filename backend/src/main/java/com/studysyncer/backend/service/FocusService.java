package com.studysyncer.backend.service;

import com.studysyncer.backend.domain.Course;
import com.studysyncer.backend.domain.Pomodoro;
import com.studysyncer.backend.domain.SessionType;
import com.studysyncer.backend.domain.StudySession;
import com.studysyncer.backend.domain.Task;
import com.studysyncer.backend.domain.User;
import com.studysyncer.backend.repository.CourseRepository;
import com.studysyncer.backend.repository.PomodoroRepository;
import com.studysyncer.backend.repository.StudySessionRepository;
import com.studysyncer.backend.repository.TaskRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

@Service
public class FocusService {

    public static final int POMODORO_SECONDS = 25 * 60;
    public static final int BREAK_SECONDS = 5 * 60;
    public static final int CYCLE_LENGTH = 4;
    public static final int STALE_AFTER_SECONDS = 90;

    private final StudySessionRepository sessionRepo;
    private final PomodoroRepository pomoRepo;
    private final TaskRepository taskRepo;
    private final CourseRepository courseRepo;

    public FocusService(StudySessionRepository sessionRepo,
                        PomodoroRepository pomoRepo,
                        TaskRepository taskRepo,
                        CourseRepository courseRepo) {
        this.sessionRepo = sessionRepo;
        this.pomoRepo = pomoRepo;
        this.taskRepo = taskRepo;
        this.courseRepo = courseRepo;
    }

    public Optional<StudySession> findActiveWorkSession(User user) {
        return sessionRepo.findFirstByUserAndEndedAtIsNullAndSessionTypeOrderByStartedAtDesc(user, SessionType.WORK);
    }

    public Optional<StudySession> findActiveBreakSession(User user) {
        return sessionRepo.findFirstByUserAndEndedAtIsNullAndSessionTypeOrderByStartedAtDesc(user, SessionType.BREAK);
    }

    @Transactional
    public StudySession startWorkSession(User user, Long courseId, Long taskId,
                                         String title, String italicSuffix) {
        if (findActiveWorkSession(user).isPresent() || findActiveBreakSession(user).isPresent()) {
            throw new IllegalStateException("A session is already in progress");
        }
        Course course = courseRepo.findById(courseId)
                .filter(c -> c.getUser().getId().equals(user.getId()))
                .orElseThrow(() -> new IllegalArgumentException("Course not found or not yours"));
        Task task = null;
        if (taskId != null) {
            task = taskRepo.findById(taskId)
                    .filter(t -> t.getUser().getId().equals(user.getId()))
                    .orElse(null);
        }
        StudySession s = new StudySession();
        s.setUser(user);
        s.setCourse(course);
        s.setTask(task);
        s.setTitle(title == null || title.isBlank() ? "Focus session" : title.trim());
        s.setItalicSuffix(italicSuffix == null || italicSuffix.isBlank() ? null : italicSuffix.trim());
        s.setSessionType(SessionType.WORK);
        Instant now = Instant.now();
        s.setStartedAt(now);
        s.setLastHeartbeatAt(now);
        return sessionRepo.save(s);
    }

    @Transactional
    public StudySession startBreak(User user) {
        if (findActiveBreakSession(user).isPresent()) {
            throw new IllegalStateException("Break already in progress");
        }
        StudySession lastWork = findActiveWorkSession(user).orElse(null);
        // If the work session is already finalized (just-completed), use it as attribution; otherwise no break.
        if (lastWork == null) {
            // No active work session — fall back to the most-recent ended session today for attribution.
            // Simplest: throw and let controller redirect.
            throw new IllegalStateException("No active work session — can't start break");
        }
        StudySession b = new StudySession();
        b.setUser(user);
        b.setCourse(lastWork.getCourse());
        b.setTask(lastWork.getTask());
        b.setTitle("Break");
        b.setItalicSuffix(null);
        b.setSessionType(SessionType.BREAK);
        Instant now = Instant.now();
        b.setStartedAt(now);
        b.setLastHeartbeatAt(now);
        // Finalize the work session first so we don't have two active sessions
        finalizeSession(lastWork);
        return sessionRepo.save(b);
    }

    @Transactional
    public boolean heartbeat(User user, Long sessionId) {
        return sessionRepo.findById(sessionId)
                .filter(s -> s.getUser().getId().equals(user.getId()) && s.getEndedAt() == null)
                .map(s -> {
                    s.setLastHeartbeatAt(Instant.now());
                    sessionRepo.save(s);
                    return true;
                })
                .orElse(false);
    }

    @Transactional
    public Pomodoro completePomodoro(User user, Long sessionId) {
        StudySession s = sessionRepo.findById(sessionId)
                .filter(x -> x.getUser().getId().equals(user.getId())
                        && x.getEndedAt() == null
                        && x.getSessionType() == SessionType.WORK)
                .orElseThrow(() -> new IllegalArgumentException("Active work session not found"));
        Pomodoro p = new Pomodoro();
        p.setSession(s);
        Instant startedAt = s.getLastHeartbeatAt() != null
                ? s.getLastHeartbeatAt().minusSeconds(POMODORO_SECONDS)
                : Instant.now().minusSeconds(POMODORO_SECONDS);
        p.setStartedAt(startedAt);
        p.setDurationSeconds(POMODORO_SECONDS);
        p.setPlannedDurationSeconds(POMODORO_SECONDS);
        p.setCompleted(Boolean.TRUE);
        s.setLastHeartbeatAt(Instant.now());
        sessionRepo.save(s);
        return pomoRepo.save(p);
    }

    @Transactional
    public void endActive(User user) {
        findActiveBreakSession(user).ifPresent(this::finalizeSession);
        findActiveWorkSession(user).ifPresent(this::finalizeSession);
    }

    @Transactional
    public void skipAndEnd(User user, Long sessionId) {
        sessionRepo.findById(sessionId)
                .filter(s -> s.getUser().getId().equals(user.getId())
                        && s.getEndedAt() == null
                        && s.getSessionType() == SessionType.WORK)
                .ifPresent(s -> {
                    Pomodoro p = new Pomodoro();
                    p.setSession(s);
                    p.setStartedAt(s.getLastHeartbeatAt() != null
                            ? s.getLastHeartbeatAt()
                            : Instant.now());
                    p.setDurationSeconds(0);
                    p.setPlannedDurationSeconds(POMODORO_SECONDS);
                    p.setCompleted(Boolean.FALSE);
                    pomoRepo.save(p);
                    finalizeSession(s);
                });
    }

    public int pomodorosInSession(StudySession s) {
        if (s.getPomodoros() == null) return 0;
        int count = 0;
        for (Pomodoro p : s.getPomodoros()) {
            if (Boolean.TRUE.equals(p.getCompleted())) count++;
        }
        return count;
    }

    private void finalizeSession(StudySession s) {
        if (s.getEndedAt() != null) return;
        Instant end = Instant.now();
        s.setEndedAt(end);
        long secs = Duration.between(s.getStartedAt(), end).getSeconds();
        s.setDurationSeconds((int) Math.max(0, secs));
        sessionRepo.save(s);
    }
}
