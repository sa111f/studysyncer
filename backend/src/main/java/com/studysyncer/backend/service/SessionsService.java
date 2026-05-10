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
import com.studysyncer.backend.web.api.dto.SessionLogRequest;
import com.studysyncer.backend.web.util.Inputs;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
public class SessionsService {

    private static final int POMO_LENGTH_SECONDS = 25 * 60;

    private final StudySessionRepository sessionRepository;
    private final PomodoroRepository pomodoroRepository;
    private final CourseRepository courseRepository;
    private final TaskRepository taskRepository;

    public SessionsService(StudySessionRepository sessionRepository,
                           PomodoroRepository pomodoroRepository,
                           CourseRepository courseRepository,
                           TaskRepository taskRepository) {
        this.sessionRepository = sessionRepository;
        this.pomodoroRepository = pomodoroRepository;
        this.courseRepository = courseRepository;
        this.taskRepository = taskRepository;
    }

    @Transactional
    public StudySession logManual(User user, SessionLogRequest req) {
        Course course = courseRepository.findById(req.getCourseId())
                .filter(c -> c.getUser().getId().equals(user.getId()))
                .orElseThrow(() -> new IllegalArgumentException("Course not found or not yours"));

        Task task = null;
        if (req.getTaskId() != null) {
            task = taskRepository.findById(req.getTaskId())
                    .filter(t -> t.getUser().getId().equals(user.getId()))
                    .orElse(null);
        }

        Instant startedAt = Inputs.parseLocalDateTime(req.getStartedAtLocal());
        if (startedAt == null) throw new IllegalArgumentException("Start time required");
        if (startedAt.isAfter(Instant.now().plusSeconds(60))) {
            throw new IllegalArgumentException("Start time can't be in the future");
        }
        long durSec = req.getDurationMinutes() * 60L;
        Instant endedAt = startedAt.plusSeconds(durSec);

        StudySession s = new StudySession();
        s.setUser(user);
        s.setCourse(course);
        s.setTask(task);
        s.setTitle(req.getTitle().trim());
        s.setItalicSuffix(Inputs.blankToNull(req.getItalicSuffix()));
        s.setSessionType(req.getSessionType() != null ? req.getSessionType() : SessionType.WORK);
        s.setStartedAt(startedAt);
        s.setEndedAt(endedAt);
        s.setDurationSeconds((int) durSec);
        s.setLastHeartbeatAt(endedAt);
        s = sessionRepository.save(s);

        synthesizePomodoros(s, startedAt, req);
        return s;
    }

    @Transactional
    public StudySession update(User user, Long id, SessionLogRequest req) {
        StudySession s = sessionRepository.findById(id)
                .filter(x -> x.getUser().getId().equals(user.getId()))
                .orElseThrow(() -> new AccessDeniedException("Not your session"));
        if (s.getEndedAt() == null) {
            throw new IllegalArgumentException("Can't edit a session that's still in progress — end it first");
        }
        Course course = courseRepository.findById(req.getCourseId())
                .filter(c -> c.getUser().getId().equals(user.getId()))
                .orElseThrow(() -> new IllegalArgumentException("Course not found or not yours"));
        Task task = null;
        if (req.getTaskId() != null) {
            task = taskRepository.findById(req.getTaskId())
                    .filter(t -> t.getUser().getId().equals(user.getId()))
                    .orElse(null);
        }

        Instant startedAt = Inputs.parseLocalDateTime(req.getStartedAtLocal());
        if (startedAt == null) throw new IllegalArgumentException("Start time required");
        long durSec = req.getDurationMinutes() * 60L;
        Instant endedAt = startedAt.plusSeconds(durSec);

        s.setCourse(course);
        s.setTask(task);
        s.setTitle(req.getTitle().trim());
        s.setItalicSuffix(Inputs.blankToNull(req.getItalicSuffix()));
        s.setSessionType(req.getSessionType() != null ? req.getSessionType() : s.getSessionType());
        s.setStartedAt(startedAt);
        s.setEndedAt(endedAt);
        s.setDurationSeconds((int) durSec);
        s.setLastHeartbeatAt(endedAt);
        sessionRepository.save(s);

        // Reconcile completed pomodoros: replace all with synthesized ones
        List<Pomodoro> existing = pomodoroRepository.findBySession(s);
        pomodoroRepository.deleteAll(existing);
        synthesizePomodoros(s, startedAt, req);
        return s;
    }

    @Transactional
    public void delete(User user, Long id) {
        StudySession s = sessionRepository.findById(id)
                .filter(x -> x.getUser().getId().equals(user.getId()))
                .orElseThrow(() -> new AccessDeniedException("Not your session"));
        if (s.getEndedAt() == null) {
            throw new IllegalArgumentException("Can't delete a session that's still in progress — end it first");
        }
        sessionRepository.delete(s);
    }

    private void synthesizePomodoros(StudySession s, Instant startedAt, SessionLogRequest req) {
        int pomos = req.getCompletedPomodoros() != null
                ? req.getCompletedPomodoros()
                : Math.max(0, req.getDurationMinutes() / 25);
        for (int i = 0; i < pomos; i++) {
            Pomodoro p = new Pomodoro();
            p.setSession(s);
            p.setStartedAt(startedAt.plusSeconds((long) i * POMO_LENGTH_SECONDS));
            p.setDurationSeconds(POMO_LENGTH_SECONDS);
            p.setPlannedDurationSeconds(POMO_LENGTH_SECONDS);
            p.setCompleted(true);
            pomodoroRepository.save(p);
        }
    }
}
