package com.studysyncer.backend.service;

import com.studysyncer.backend.domain.ColorKey;
import com.studysyncer.backend.domain.ColorVariant;
import com.studysyncer.backend.domain.Course;
import com.studysyncer.backend.domain.Exam;
import com.studysyncer.backend.domain.ExamStatus;
import com.studysyncer.backend.domain.ExamType;
import com.studysyncer.backend.domain.Priority;
import com.studysyncer.backend.domain.StudySession;
import com.studysyncer.backend.domain.Tag;
import com.studysyncer.backend.domain.Task;
import com.studysyncer.backend.domain.TaskStatus;
import com.studysyncer.backend.domain.User;
import com.studysyncer.backend.repository.CourseRepository;
import com.studysyncer.backend.repository.ExamRepository;
import com.studysyncer.backend.repository.StudySessionRepository;
import com.studysyncer.backend.repository.TagRepository;
import com.studysyncer.backend.repository.TaskRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class DemoSeeder {

    private final CourseRepository courseRepository;
    private final TagRepository tagRepository;
    private final TaskRepository taskRepository;
    private final ExamRepository examRepository;
    private final StudySessionRepository studySessionRepository;

    public DemoSeeder(CourseRepository courseRepository,
                      TagRepository tagRepository,
                      TaskRepository taskRepository,
                      ExamRepository examRepository,
                      StudySessionRepository studySessionRepository) {
        this.courseRepository = courseRepository;
        this.tagRepository = tagRepository;
        this.taskRepository = taskRepository;
        this.examRepository = examRepository;
        this.studySessionRepository = studySessionRepository;
    }

    @Transactional
    public void seedIfEmpty(User user) {
        if (!courseRepository.findByUserOrderByDisplayOrderAsc(user).isEmpty()) {
            return;
        }

        ZoneId tz = ZoneId.systemDefault();
        ZonedDateTime now = ZonedDateTime.now(tz);
        ZonedDateTime today = now.toLocalDate().atStartOfDay(tz);
        Instant nowInstant = now.toInstant();

        Map<String, Course> courses = new HashMap<>();
        courses.put("PHYS 211", saveCourse(user, "PHYS 211", "Classical Mechanics", ColorKey.PHYSICS, ColorVariant.DEFAULT, 0));
        courses.put("PHYS 311", saveCourse(user, "PHYS 311", "Quantum Mechanics", ColorKey.PHYSICS, ColorVariant.DEEP, 1));
        courses.put("CS 232",   saveCourse(user, "CS 232",   "Discrete Mathematics", ColorKey.CS,      ColorVariant.DEFAULT, 2));
        courses.put("ENG 102",  saveCourse(user, "ENG 102",  "Composition",          ColorKey.ENG,     ColorVariant.DEFAULT, 3));
        courses.put("PHIL 240", saveCourse(user, "PHIL 240", "Modern Philosophy",    ColorKey.PHIL,    ColorVariant.DEFAULT, 4));

        for (String name : List.of("midterm", "writeup", "reading", "lab")) {
            Tag tag = new Tag();
            tag.setUser(user);
            tag.setName(name);
            tagRepository.save(tag);
        }

        // Tasks — overdue
        saveTask(user, courses.get("PHYS 211"), "Lab 2 writeup", null,
                today.minusDays(3).with(LocalTime.of(23, 59)).toInstant(),
                45, Priority.NORMAL, nowInstant);

        // Tasks — today
        saveTask(user, courses.get("PHYS 211"), "Read Ch. 4", "Equilibrium",
                today.with(LocalTime.of(21, 0)).toInstant(),
                60, Priority.NORMAL, nowInstant);
        saveTask(user, courses.get("CS 232"), "Problem set 3", null,
                today.with(LocalTime.of(23, 0)).toInstant(),
                90, Priority.HIGH, nowInstant);
        saveTask(user, courses.get("ENG 102"), "Draft intro paragraph", null,
                today.with(LocalTime.of(23, 59)).toInstant(),
                30, Priority.NORMAL, nowInstant);

        // Tasks — this week
        saveTask(user, courses.get("PHIL 240"), "Reading response", "Foucault",
                today.plusDays(1).with(LocalTime.of(23, 59)).toInstant(),
                45, Priority.NORMAL, nowInstant);
        saveTask(user, courses.get("CS 232"), "Group meeting prep", null,
                today.plusDays(3).with(LocalTime.of(17, 0)).toInstant(),
                30, Priority.NORMAL, nowInstant);
        saveTask(user, courses.get("ENG 102"), "Outline for term paper", null,
                today.plusDays(6).with(LocalTime.of(23, 59)).toInstant(),
                60, Priority.NORMAL, nowInstant);

        // Exams
        saveExam(user, courses.get("PHYS 311"), "Midterm — Quantum Mechanics", ExamType.MIDTERM,
                today.plusDays(12).with(LocalTime.of(14, 0)).toInstant(),
                90, "Hewitt 204", nowInstant);
        saveExam(user, courses.get("CS 232"), "Final — Discrete Math", ExamType.FINAL,
                today.plusDays(26).with(LocalTime.of(9, 0)).toInstant(),
                180, "Wexler 110", nowInstant);

        // Study sessions — completed history this week (drives streak + week-stat + 52m today)
        saveSession(user, courses.get("PHYS 311"), "Quantum review",       "Ch. 1",
                today.minusDays(4).with(LocalTime.of(10, 0)).toInstant(), 45 * 60);
        saveSession(user, courses.get("CS 232"),   "Problem set 2",        null,
                today.minusDays(3).with(LocalTime.of(19, 0)).toInstant(), 60 * 60);
        saveSession(user, courses.get("PHIL 240"), "Reading",              "Foucault Ch. 1",
                today.minusDays(2).with(LocalTime.of(15, 0)).toInstant(), 25 * 60);
        saveSession(user, courses.get("PHYS 211"), "Lab 2 setup",          null,
                today.minusDays(2).with(LocalTime.of(20, 0)).toInstant(), 90 * 60);
        saveSession(user, courses.get("CS 232"),   "Algorithm exercises",  null,
                today.minusDays(1).with(LocalTime.of(11, 0)).toInstant(), 50 * 60);
        saveSession(user, courses.get("CS 232"),   "Problem set 3",        "recursion",
                today.with(LocalTime.of(14, 0)).toInstant(),               52 * 60);
    }

    private Course saveCourse(User user, String code, String name, ColorKey ck, ColorVariant cv, int order) {
        Course c = new Course();
        c.setUser(user);
        c.setCode(code);
        c.setName(name);
        c.setColorKey(ck);
        c.setColorVariant(cv);
        c.setDisplayOrder(order);
        return courseRepository.save(c);
    }

    private void saveTask(User user, Course course, String title, String italic, Instant dueAt,
                          int estMinutes, Priority priority, Instant nowInstant) {
        Task t = new Task();
        t.setUser(user);
        t.setCourse(course);
        t.setTitle(title);
        t.setItalicSuffix(italic);
        t.setDueAt(dueAt);
        t.setEstimatedMinutes(estMinutes);
        t.setPriority(priority);
        t.setStatus(TaskStatus.PENDING);
        t.setCompletedAt(null);
        t.setCreatedAt(nowInstant);
        taskRepository.save(t);
    }

    private void saveExam(User user, Course course, String title, ExamType type, Instant startsAt,
                          int durationMin, String location, Instant nowInstant) {
        Exam e = new Exam();
        e.setUser(user);
        e.setCourse(course);
        e.setTitle(title);
        e.setExamType(type);
        e.setStartsAt(startsAt);
        e.setDurationMinutes(durationMin);
        e.setLocation(location);
        e.setStatus(ExamStatus.UPCOMING);
        e.setCreatedAt(nowInstant);
        examRepository.save(e);
    }

    private void saveSession(User user, Course course, String title, String italic,
                             Instant startedAt, int durationSeconds) {
        StudySession s = new StudySession();
        s.setUser(user);
        s.setCourse(course);
        s.setTitle(title);
        s.setItalicSuffix(italic);
        s.setStartedAt(startedAt);
        s.setEndedAt(startedAt.plusSeconds(durationSeconds));
        s.setDurationSeconds(durationSeconds);
        studySessionRepository.save(s);
    }
}
