package com.studysyncer.backend.service;

import com.studysyncer.backend.domain.ColorKey;
import com.studysyncer.backend.domain.ColorVariant;
import com.studysyncer.backend.domain.Course;
import com.studysyncer.backend.domain.Exam;
import com.studysyncer.backend.domain.ExamStatus;
import com.studysyncer.backend.domain.ExamTopic;
import com.studysyncer.backend.domain.ExamType;
import com.studysyncer.backend.domain.Priority;
import com.studysyncer.backend.domain.StudySession;
import com.studysyncer.backend.domain.Tag;
import com.studysyncer.backend.domain.Task;
import com.studysyncer.backend.domain.TaskStatus;
import com.studysyncer.backend.domain.TopicStatus;
import com.studysyncer.backend.domain.User;
import com.studysyncer.backend.repository.CourseRepository;
import com.studysyncer.backend.repository.ExamRepository;
import com.studysyncer.backend.repository.ExamTopicRepository;
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
    private final ExamTopicRepository examTopicRepository;
    private final StudySessionRepository studySessionRepository;

    public DemoSeeder(CourseRepository courseRepository,
                      TagRepository tagRepository,
                      TaskRepository taskRepository,
                      ExamRepository examRepository,
                      ExamTopicRepository examTopicRepository,
                      StudySessionRepository studySessionRepository) {
        this.courseRepository = courseRepository;
        this.tagRepository = tagRepository;
        this.taskRepository = taskRepository;
        this.examRepository = examRepository;
        this.examTopicRepository = examTopicRepository;
        this.studySessionRepository = studySessionRepository;
    }

    @Transactional
    public void wipeAndReseed(User user) {
        // FK-safe order: tasks → tags → study_sessions → exams → courses
        taskRepository.deleteByUser(user);
        tagRepository.deleteByUser(user);
        studySessionRepository.deleteByUser(user);
        examRepository.deleteByUser(user);
        courseRepository.deleteByUser(user);
        seedIfEmpty(user);
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

        // Additional overdue (Tasks page shows 2 overdue per design)
        saveTask(user, courses.get("PHIL 240"), "Reading response", "Foucault Ch. 1",
                today.minusDays(1).with(LocalTime.of(23, 59)).toInstant(),
                30, Priority.NORMAL, nowInstant);

        // Two completed-today tasks (drives "Completed today" section + "By course" fractions)
        Task done1 = new Task();
        done1.setUser(user);
        done1.setCourse(courses.get("PHIL 240"));
        done1.setTitle("Discussion post");
        done1.setItalicSuffix("Plato's cave");
        done1.setDueAt(today.with(LocalTime.of(23, 59)).toInstant());
        done1.setEstimatedMinutes(20);
        done1.setPriority(Priority.NORMAL);
        done1.setStatus(TaskStatus.COMPLETED);
        done1.setCompletedAt(today.with(LocalTime.of(14, 14)).toInstant());
        done1.setCreatedAt(nowInstant);
        taskRepository.save(done1);

        Task done2 = new Task();
        done2.setUser(user);
        done2.setCourse(courses.get("CS 232"));
        done2.setTitle("Skim lecture slides");
        done2.setItalicSuffix("week 11");
        done2.setDueAt(today.with(LocalTime.of(23, 59)).toInstant());
        done2.setEstimatedMinutes(15);
        done2.setPriority(Priority.NORMAL);
        done2.setStatus(TaskStatus.COMPLETED);
        done2.setCompletedAt(today.with(LocalTime.of(11, 8)).toInstant());
        done2.setCreatedAt(nowInstant);
        taskRepository.save(done2);

        // Upcoming exams + topics
        Exam quantum = saveExam(user, courses.get("PHYS 311"), "Quantum Mechanics — midterm",
                ExamType.MIDTERM,
                today.plusDays(12).with(LocalTime.of(14, 0)).toInstant(),
                90, "Hewitt 204", ExamStatus.UPCOMING, null, nowInstant);
        topic(quantum, "Wave functions",      TopicStatus.DONE,    0);
        topic(quantum, "Schrödinger eq.",     TopicStatus.DONE,    1);
        topic(quantum, "Operators",           TopicStatus.NEUTRAL, 2);
        topic(quantum, "Perturbation theory", TopicStatus.WEAK,    3);
        topic(quantum, "Spin",                TopicStatus.NEUTRAL, 4);

        Exam discrete = saveExam(user, courses.get("CS 232"), "Discrete Mathematics — final",
                ExamType.FINAL,
                today.plusDays(26).with(LocalTime.of(9, 0)).toInstant(),
                180, "Wexler 110", ExamStatus.UPCOMING, null, nowInstant);
        topic(discrete, "Logic & proofs",   TopicStatus.DONE,    0);
        topic(discrete, "Sets & relations", TopicStatus.DONE,    1);
        topic(discrete, "Graph theory",     TopicStatus.NEUTRAL, 2);
        topic(discrete, "Recursion",        TopicStatus.WEAK,    3);
        topic(discrete, "Counting",         TopicStatus.NEUTRAL, 4);

        Exam foucault = saveExam(user, courses.get("PHIL 240"), "Foucault & Power — take-home",
                ExamType.FINAL,
                today.plusDays(20).with(LocalTime.of(9, 0)).toInstant(),
                48 * 60, "Take-home · 48hr window", ExamStatus.UPCOMING, null, nowInstant);
        topic(foucault, "Discipline & Punish", TopicStatus.DONE,    0);
        topic(foucault, "Panopticism",         TopicStatus.NEUTRAL, 1);
        topic(foucault, "Biopower",            TopicStatus.NEUTRAL, 2);

        Exam persuasive = saveExam(user, courses.get("ENG 102"), "Persuasive essay — final",
                ExamType.FINAL,
                today.plusDays(27).with(LocalTime.of(23, 59)).toInstant(),
                0, "Due 11:59 PM · 2,000 words", ExamStatus.UPCOMING, null, nowInstant);
        topic(persuasive, "Outline",   TopicStatus.NEUTRAL, 0);
        topic(persuasive, "Draft",     TopicStatus.NEUTRAL, 1);
        topic(persuasive, "Revisions", TopicStatus.NEUTRAL, 2);

        Exam mechanics = saveExam(user, courses.get("PHYS 211"), "Mechanics — final",
                ExamType.FINAL,
                today.plusDays(34).with(LocalTime.of(10, 0)).toInstant(),
                180, "Hewitt 204", ExamStatus.UPCOMING, null, nowInstant);
        topic(mechanics, "Kinematics",    TopicStatus.NEUTRAL, 0);
        topic(mechanics, "Newton's laws", TopicStatus.NEUTRAL, 1);
        topic(mechanics, "Energy & work", TopicStatus.NEUTRAL, 2);
        topic(mechanics, "Oscillations",  TopicStatus.NEUTRAL, 3);
        topic(mechanics, "Rotational",    TopicStatus.NEUTRAL, 4);

        // Past exams with grades
        saveExam(user, courses.get("PHYS 311"), "Quiz 4 — Lagrangian mechanics", ExamType.QUIZ,
                today.minusDays(15).with(LocalTime.of(10, 0)).toInstant(),
                30, "Hewitt 204", ExamStatus.PAST, "A−", nowInstant);
        saveExam(user, courses.get("CS 232"), "Midterm — Algorithms", ExamType.MIDTERM,
                today.minusDays(24).with(LocalTime.of(13, 0)).toInstant(),
                90, "Wexler 110", ExamStatus.PAST, "B+", nowInstant);
        saveExam(user, courses.get("PHIL 240"), "Reading exam — Plato's Republic", ExamType.QUIZ,
                today.minusDays(36).with(LocalTime.of(15, 0)).toInstant(),
                60, "Library Hall", ExamStatus.PAST, "A", nowInstant);

        // Study sessions — full Sun..today coverage of current week (Sunday-first grid).
        // Tracker page bar chart needs at least one session per past weekday; this also drives
        // the streak, daily/week totals, recent-session list, and peak-hour insight.
        Course phys211 = courses.get("PHYS 211");
        Course phys311 = courses.get("PHYS 311");
        Course cs232   = courses.get("CS 232");
        Course eng102  = courses.get("ENG 102");
        Course phil240 = courses.get("PHIL 240");

        // Sunday at or before today (Sun=last day of ISO week → 0 days back when today is Sun).
        int dow = now.getDayOfWeek().getValue(); // Mon=1..Sun=7
        int daysBackToSunday = (dow == 7) ? 0 : dow;
        ZonedDateTime sun = today.minusDays(daysBackToSunday);
        ZonedDateTime mondayDay = sun.plusDays(1);
        ZonedDateTime tueDay   = sun.plusDays(2);
        ZonedDateTime wedDay   = sun.plusDays(3);
        ZonedDateTime thuDay   = sun.plusDays(4);
        ZonedDateTime friDay   = sun.plusDays(5);
        ZonedDateTime satDay   = sun.plusDays(6);
        java.time.LocalDate todayDate = today.toLocalDate();

        // Sunday
        if (!sun.toLocalDate().isAfter(todayDate) && !sun.toLocalDate().isEqual(todayDate)) {
            saveSession(user, phil240, "Reading review",   "Plato",   sun.with(LocalTime.of(18, 20)).toInstant(), 25 * 60);
            saveSession(user, cs232,   "Lecture catch-up", null,      sun.with(LocalTime.of(20,  0)).toInstant(), 60 * 60);
        }
        // Monday
        if (!mondayDay.toLocalDate().isAfter(todayDate) && !mondayDay.toLocalDate().isEqual(todayDate)) {
            saveSession(user, phil240, "Reading",            "Foucault Ch. 1", mondayDay.with(LocalTime.of(15, 40)).toInstant(), 25 * 60);
            saveSession(user, eng102,  "Outline brainstorm", null,             mondayDay.with(LocalTime.of(17,  0)).toInstant(), 35 * 60);
            saveSession(user, cs232,   "Algorithm exercises", null,            mondayDay.with(LocalTime.of(11,  0)).toInstant(), 50 * 60);
            saveSession(user, phys211, "Lab 2 setup",        null,             mondayDay.with(LocalTime.of(20, 15)).toInstant(), 90 * 60);
        }
        // Tuesday
        if (!tueDay.toLocalDate().isAfter(todayDate) && !tueDay.toLocalDate().isEqual(todayDate)) {
            saveSession(user, cs232,   "Problem set 3",  "recursion", tueDay.with(LocalTime.of(14, 0)).toInstant(), 72 * 60);
            saveSession(user, phys311, "Quantum review", "Ch. 2",     tueDay.with(LocalTime.of( 9, 30)).toInstant(), 60 * 60);
        }
        // Wednesday
        if (!wedDay.toLocalDate().isAfter(todayDate) && !wedDay.toLocalDate().isEqual(todayDate)) {
            saveSession(user, phil240, "Discussion prep",     null,    wedDay.with(LocalTime.of(13,  0)).toInstant(),  45 * 60);
            saveSession(user, cs232,   "Office hours notes",  null,    wedDay.with(LocalTime.of(15, 30)).toInstant(),  30 * 60);
            saveSession(user, phys311, "Quantum problems",    "Ch. 3", wedDay.with(LocalTime.of(20,  0)).toInstant(), 120 * 60);
        }
        // Thursday
        if (!thuDay.toLocalDate().isAfter(todayDate) && !thuDay.toLocalDate().isEqual(todayDate)) {
            saveSession(user, eng102,  "Draft writing",  null,    thuDay.with(LocalTime.of(10, 0)).toInstant(), 30 * 60);
            saveSession(user, phys311, "Quantum review", "Ch. 1", thuDay.with(LocalTime.of(19, 0)).toInstant(), 80 * 60);
        }
        // Friday
        if (!friDay.toLocalDate().isAfter(todayDate) && !friDay.toLocalDate().isEqual(todayDate)) {
            saveSession(user, cs232,   "Group meeting prep", null,    friDay.with(LocalTime.of(11, 0)).toInstant(), 60 * 60);
            saveSession(user, phys311, "Quantum problems",   "Ch. 4", friDay.with(LocalTime.of(16, 0)).toInstant(), 90 * 60);
        }
        // Saturday
        if (!satDay.toLocalDate().isAfter(todayDate) && !satDay.toLocalDate().isEqual(todayDate)) {
            saveSession(user, phil240, "Foucault notes", null, satDay.with(LocalTime.of(14, 0)).toInstant(), 35 * 60);
        }

        // Today — keep a guaranteed afternoon CS session (drives "52m today" and recent list "Today")
        saveSession(user, cs232,   "Problem set 3",  "recursion", today.with(LocalTime.of(14,  0)).toInstant(), 52 * 60);
        if (now.getHour() >= 12) {
            saveSession(user, phys311, "Quantum review", "intro",  today.with(LocalTime.of( 9, 30)).toInstant(), 60 * 60);
        }
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

    private Exam saveExam(User user, Course course, String title, ExamType type, Instant startsAt,
                          int durationMin, String location, ExamStatus status, String grade,
                          Instant nowInstant) {
        Exam e = new Exam();
        e.setUser(user);
        e.setCourse(course);
        e.setTitle(title);
        e.setExamType(type);
        e.setStartsAt(startsAt);
        e.setDurationMinutes(durationMin);
        e.setLocation(location);
        e.setStatus(status);
        e.setGrade(grade);
        e.setCreatedAt(nowInstant);
        return examRepository.save(e);
    }

    private ExamTopic topic(Exam exam, String name, TopicStatus status, int order) {
        ExamTopic t = new ExamTopic();
        t.setExam(exam);
        t.setName(name);
        t.setStatus(status);
        t.setDisplayOrder(order);
        return examTopicRepository.save(t);
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
