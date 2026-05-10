package com.studysyncer.backend.service;

import com.studysyncer.backend.domain.ColorKey;
import com.studysyncer.backend.domain.ColorVariant;
import com.studysyncer.backend.domain.Course;
import com.studysyncer.backend.domain.Exam;
import com.studysyncer.backend.domain.ExamStatus;
import com.studysyncer.backend.domain.ScheduleItem;
import com.studysyncer.backend.domain.ScheduleItemType;
import com.studysyncer.backend.domain.StudySession;
import com.studysyncer.backend.domain.Task;
import com.studysyncer.backend.domain.TaskStatus;
import com.studysyncer.backend.domain.User;
import com.studysyncer.backend.repository.CourseRepository;
import com.studysyncer.backend.repository.ExamRepository;
import com.studysyncer.backend.repository.ScheduleItemRepository;
import com.studysyncer.backend.repository.StudySessionRepository;
import com.studysyncer.backend.repository.TaskRepository;
import com.studysyncer.backend.web.api.dto.CourseCreateRequest;
import com.studysyncer.backend.web.api.dto.ScheduleItemRequest;
import com.studysyncer.backend.web.util.Inputs;
import com.studysyncer.backend.web.view.Formatters;
import com.studysyncer.backend.web.view.SpacesView;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class SpacesService {

    private static final DateTimeFormatter EEE_MMM_D = DateTimeFormatter.ofPattern("EEE MMM d", Locale.ENGLISH);

    private final CourseRepository courseRepository;
    private final TaskRepository taskRepository;
    private final ExamRepository examRepository;
    private final ScheduleItemRepository scheduleItemRepository;
    private final StudySessionRepository studySessionRepository;
    private final Formatters formatters;

    public SpacesService(CourseRepository courseRepository,
                         TaskRepository taskRepository,
                         ExamRepository examRepository,
                         ScheduleItemRepository scheduleItemRepository,
                         StudySessionRepository studySessionRepository,
                         Formatters formatters) {
        this.courseRepository = courseRepository;
        this.taskRepository = taskRepository;
        this.examRepository = examRepository;
        this.scheduleItemRepository = scheduleItemRepository;
        this.studySessionRepository = studySessionRepository;
        this.formatters = formatters;
    }

    @Transactional(readOnly = true)
    public Optional<SpacesView> buildFor(User user, String courseSlug) {
        if (courseSlug == null || courseSlug.isBlank()) {
            return Optional.empty();
        }
        String code = courseSlug.replace('-', ' ');
        Course course = courseRepository.findByUserAndCode(user, code).orElse(null);
        if (course == null) {
            return Optional.empty();
        }

        ZoneId tz = ZoneId.systemDefault();
        ZonedDateTime now = ZonedDateTime.now(tz);
        LocalDate today = now.toLocalDate();
        ZonedDateTime startOfToday = today.atStartOfDay(tz);
        ZonedDateTime endOfToday = startOfToday.plusDays(1);
        ZonedDateTime startOfWeek = startOfToday.with(DayOfWeek.MONDAY);
        if (startOfWeek.isAfter(startOfToday)) {
            startOfWeek = startOfWeek.minusWeeks(1);
        }
        ZonedDateTime endOfWeek = startOfWeek.plusWeeks(1);
        ZonedDateTime startOfLastWeek = startOfWeek.minusWeeks(1);

        // Schedule items
        List<ScheduleItem> allScheduleItems = scheduleItemRepository
                .findByCourseOrderByDayOfWeekAscStartMinuteAsc(course);

        // Stats
        long openTasksOpen = taskRepository.countByUserAndCourseAndStatus(user, course, TaskStatus.PENDING);
        long openTasksTotal = taskRepository.countByUserAndCourse(user, course);
        long openTasksDueToday = taskRepository.countByUserAndCourseAndStatusAndDueAtBetween(
                user, course, TaskStatus.PENDING, startOfToday.toInstant(), endOfToday.toInstant());
        long openTasksOverdue = taskRepository.countByUserAndCourseAndStatusAndDueAtBefore(
                user, course, TaskStatus.PENDING, startOfToday.toInstant());

        List<StudySession> weekSessions = studySessionRepository
                .findByUserAndStartedAtBetween(user, startOfWeek.toInstant(), endOfWeek.toInstant())
                .stream()
                .filter(s -> s.getCourse() != null && s.getCourse().getId().equals(course.getId()))
                .toList();
        int weekMinutes = (int) (weekSessions.stream()
                .mapToLong(s -> s.getDurationSeconds() == null ? 0L : s.getDurationSeconds())
                .sum() / 60);

        List<StudySession> lastWeekSessions = studySessionRepository
                .findByUserAndStartedAtBetween(user, startOfLastWeek.toInstant(), startOfWeek.toInstant())
                .stream()
                .filter(s -> s.getCourse() != null && s.getCourse().getId().equals(course.getId()))
                .toList();
        int lastWeekMinutes = (int) (lastWeekSessions.stream()
                .mapToLong(s -> s.getDurationSeconds() == null ? 0L : s.getDurationSeconds())
                .sum() / 60);
        int weekMinutesDelta = weekMinutes - lastWeekMinutes;
        String weekDeltaLabel = formatDelta(weekMinutesDelta);
        String weekHoursLabel = formatHoursMins(weekMinutes);

        // Next exam for this course
        Exam nextExamEntity = examRepository
                .findByUserAndStatusOrderByStartsAtAsc(user, ExamStatus.UPCOMING)
                .stream()
                .filter(e -> e.getCourse().getId().equals(course.getId()))
                .findFirst()
                .orElse(null);
        Long nextExamDays = null;
        SpacesView.NextExam nextExam = null;
        if (nextExamEntity != null) {
            LocalDate examDate = nextExamEntity.getStartsAt().atZone(tz).toLocalDate();
            long daysUntil = ChronoUnit.DAYS.between(today, examDate);
            nextExamDays = daysUntil;
            String dateLine = EEE_MMM_D.format(examDate);
            String typeText = nextExamEntity.getExamType().name().toLowerCase(Locale.ENGLISH);
            String typeItalic = "— " + typeText + " exam.";
            String[] split = nextExamEntity.getTitle().split(" — ", 2);
            String titleHead = split[0];
            String titleEm = split.length > 1 ? "— " + split[1] : typeItalic;
            nextExam = new SpacesView.NextExam(
                    nextExamEntity,
                    daysUntil,
                    dateLine,
                    typeItalic,
                    titleHead,
                    titleEm
            );
        }

        SpacesView.Stats stats = new SpacesView.Stats(
                openTasksOpen,
                openTasksTotal,
                openTasksDueToday,
                openTasksOverdue,
                weekMinutes,
                weekHoursLabel,
                weekMinutesDelta,
                weekDeltaLabel,
                nextExamDays
        );

        // Open tasks (top 3, ordered by overdue → today → upcoming, then due asc)
        List<Task> pendingForCourse = taskRepository
                .findByUserAndStatusOrderByDueAtAsc(user, TaskStatus.PENDING)
                .stream()
                .filter(t -> t.getCourse() != null && t.getCourse().getId().equals(course.getId()))
                .toList();
        List<SpacesView.OpenTask> openTasks = pendingForCourse.stream()
                .limit(3)
                .map(t -> buildOpenTask(t, today, tz))
                .toList();

        // Time invested (Sunday-first, 7 days)
        SpacesView.TimeInvested timeInvested = buildTimeInvested(weekSessions, weekMinutes, today, tz);

        // Schedule for the rail (this week's remaining)
        List<SpacesView.UpcomingScheduleEntry> schedule = buildSchedule(allScheduleItems, now);

        // Schedule summary for hero
        String scheduleSummary = buildScheduleSummary(allScheduleItems);

        String pillClass = formatters.pillClass(course);
        String courseInitial = course.getCode() == null || course.getCode().isEmpty()
                ? ""
                : course.getCode().substring(0, 1);

        String nameHead = "";
        String nameEm = "";
        String courseName = course.getName();
        if (courseName != null && !courseName.isBlank()) {
            int lastSpace = courseName.lastIndexOf(' ');
            if (lastSpace > 0) {
                nameHead = courseName.substring(0, lastSpace + 1);
                nameEm = courseName.substring(lastSpace + 1) + ".";
            } else {
                nameEm = courseName + ".";
            }
        }

        return Optional.of(new SpacesView(
                course,
                courseInitial,
                pillClass,
                pillClass,
                nameHead,
                nameEm,
                scheduleSummary,
                stats,
                nextExam,
                openTasks,
                openTasksOpen,
                timeInvested,
                schedule
        ));
    }

    @Transactional
    public Course createCourse(User user, CourseCreateRequest req, List<ScheduleItemRequest> schedule) {
        String code = req.getCode().trim();
        if (courseRepository.findByUserAndCode(user, code).isPresent()) {
            throw new IllegalArgumentException("A course with code '" + code + "' already exists");
        }
        int nextOrder = courseRepository.findByUserOrderByDisplayOrderAsc(user).size();

        Course c = new Course();
        c.setUser(user);
        apply(c, req);
        c.setDisplayOrder(nextOrder);
        c = courseRepository.save(c);
        replaceScheduleItems(c, schedule);
        return c;
    }

    @Transactional
    public Course updateCourse(User user, Long id, CourseCreateRequest req, List<ScheduleItemRequest> schedule) {
        Course c = courseRepository.findById(id)
                .filter(x -> x.getUser().getId().equals(user.getId()))
                .orElseThrow(() -> new AccessDeniedException("Not your course"));

        String newCode = req.getCode().trim();
        if (!c.getCode().equals(newCode)) {
            courseRepository.findByUserAndCode(user, newCode).ifPresent(existing -> {
                throw new IllegalArgumentException("A course with code '" + newCode + "' already exists");
            });
        }
        apply(c, req);
        courseRepository.save(c);
        if (schedule != null) {
            replaceScheduleItems(c, schedule);
        }
        return c;
    }

    @Transactional
    public void deleteCourse(User user, Long id) {
        Course c = courseRepository.findById(id)
                .filter(x -> x.getUser().getId().equals(user.getId()))
                .orElseThrow(() -> new AccessDeniedException("Not your course"));
        // FK constraints (V1):
        //   tasks.course_id      → ON DELETE SET NULL  (tasks survive, attribution dropped)
        //   exams.course_id      → ON DELETE CASCADE   (exams + topics removed)
        //   study_sessions       → ON DELETE CASCADE   (sessions removed)
        //   schedule_items       → ON DELETE CASCADE   (schedule rows removed)
        courseRepository.delete(c);
    }

    private void apply(Course c, CourseCreateRequest req) {
        c.setCode(req.getCode().trim());
        c.setName(req.getName().trim());
        c.setColorKey(req.getColorKey() != null ? req.getColorKey() : ColorKey.OTHER);
        c.setColorVariant(req.getColorVariant() != null ? req.getColorVariant() : ColorVariant.DEFAULT);
        c.setSection(Inputs.blankToNull(req.getSection()));
        c.setTerm(Inputs.blankToNull(req.getTerm()));
        c.setProfessor(Inputs.blankToNull(req.getProfessor()));
        c.setRoom(Inputs.blankToNull(req.getRoom()));
        c.setCredits(req.getCredits());
        c.setDescription(Inputs.blankToNull(req.getDescription()));
    }

    /** Replace schedule items: keep matching ids (updated in place), insert new ones, delete dropped ones. */
    private void replaceScheduleItems(Course c, List<ScheduleItemRequest> incoming) {
        if (incoming == null) incoming = List.of();
        List<ScheduleItem> existing = scheduleItemRepository.findByCourseOrderByDayOfWeekAscStartMinuteAsc(c);
        Map<Long, ScheduleItem> byId = existing.stream()
                .filter(s -> s.getId() != null)
                .collect(Collectors.toMap(ScheduleItem::getId, s -> s));

        Set<Long> keptIds = new HashSet<>();
        int order = 0;
        for (ScheduleItemRequest req : incoming) {
            // Skip rows where required fields are absent (e.g. an empty cloned row the user added then ignored)
            if (req.getTitle() == null || req.getTitle().isBlank()) continue;
            if (req.getDayOfWeek() == null) continue;
            if (req.getStartTimeLocal() == null || req.getStartTimeLocal().isBlank()) continue;

            ScheduleItem item = req.getId() != null ? byId.get(req.getId()) : null;
            if (item == null) {
                item = new ScheduleItem();
                item.setCourse(c);
                item.setCreatedAt(Instant.now());
            }
            item.setTitle(req.getTitle().trim());
            item.setLocation(Inputs.blankToNull(req.getLocation()));
            item.setDayOfWeek(req.getDayOfWeek());
            item.setStartMinute(parseTimeToMinutes(req.getStartTimeLocal()));
            item.setDurationMinutes(req.getDurationMinutes());
            item.setItemType(req.getItemType() != null ? req.getItemType() : ScheduleItemType.LECTURE);
            item.setDisplayOrder(order++);
            item = scheduleItemRepository.save(item);
            keptIds.add(item.getId());
        }
        for (ScheduleItem old : existing) {
            if (!keptIds.contains(old.getId())) {
                scheduleItemRepository.delete(old);
            }
        }
    }

    private static int parseTimeToMinutes(String hhmm) {
        if (hhmm == null || hhmm.isBlank()) throw new IllegalArgumentException("Time is required");
        String[] parts = hhmm.split(":");
        if (parts.length < 2) throw new IllegalArgumentException("Time must be HH:mm");
        try {
            int h = Integer.parseInt(parts[0]);
            int m = Integer.parseInt(parts[1]);
            if (h < 0 || h > 23 || m < 0 || m > 59) throw new IllegalArgumentException("Invalid time");
            return h * 60 + m;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid time");
        }
    }

    private SpacesView.OpenTask buildOpenTask(Task t, LocalDate today, ZoneId tz) {
        String estLabel = t.getEstimatedMinutes() != null && t.getEstimatedMinutes() > 0
                ? formatters.fmtEstimate(t.getEstimatedMinutes())
                : null;
        Instant due = t.getDueAt();
        String dueLabel;
        String dueClass;
        if (due == null) {
            dueLabel = "";
            dueClass = "";
        } else {
            LocalDate dueDate = due.atZone(tz).toLocalDate();
            if (dueDate.isBefore(today)) {
                dueLabel = formatters.fmtOverdue(due);
                dueClass = "late";
            } else if (dueDate.isEqual(today)) {
                dueLabel = "Today · " + formatters.fmtToday(due);
                dueClass = "today";
            } else {
                long days = ChronoUnit.DAYS.between(today, dueDate);
                dueLabel = (days >= 0 && days <= 6) ? formatters.fmtWeekday(due) : formatters.fmtMonthDay(due);
                dueClass = "";
            }
        }
        return new SpacesView.OpenTask(t, estLabel, dueLabel, dueClass);
    }

    private SpacesView.TimeInvested buildTimeInvested(List<StudySession> weekSessions,
                                                      int weekMinutes,
                                                      LocalDate today,
                                                      ZoneId tz) {
        // Sunday-first 7 days, anchored on the week containing `today`.
        // If today is Mon..Sat, Sunday is the previous Sunday. If today is Sun, that Sunday IS today.
        DayOfWeek dow = today.getDayOfWeek();
        int daysBackToSunday = dow == DayOfWeek.SUNDAY ? 0 : dow.getValue();
        LocalDate sunday = today.minusDays(daysBackToSunday);
        String[] letters = {"S", "M", "T", "W", "T", "F", "S"};

        int[] perDay = new int[7];
        for (StudySession s : weekSessions) {
            LocalDate sd = s.getStartedAt().atZone(tz).toLocalDate();
            for (int i = 0; i < 7; i++) {
                if (sd.equals(sunday.plusDays(i))) {
                    perDay[i] += (s.getDurationSeconds() == null ? 0 : s.getDurationSeconds()) / 60;
                    break;
                }
            }
        }

        int max = 0;
        for (int v : perDay) {
            if (v > max) max = v;
        }
        int denom = max == 0 ? 60 : max;

        List<SpacesView.MiniBar> bars = new ArrayList<>(7);
        for (int i = 0; i < 7; i++) {
            LocalDate d = sunday.plusDays(i);
            boolean isToday = d.equals(today);
            int minutes = perDay[i];
            boolean filled = minutes > 0;
            double height = filled ? Math.max(6.0, (minutes * 100.0) / denom) : 6.0;
            bars.add(new SpacesView.MiniBar(minutes, height, filled, isToday, letters[i]));
        }
        return new SpacesView.TimeInvested(weekMinutes, formatHoursMins(weekMinutes), bars);
    }

    private List<SpacesView.UpcomingScheduleEntry> buildSchedule(List<ScheduleItem> items,
                                                                 ZonedDateTime now) {
        if (items.isEmpty()) {
            return List.of();
        }
        int todayDow = now.getDayOfWeek().getValue();
        int todayMinute = now.getHour() * 60 + now.getMinute();

        List<ScheduleItem> remaining = items.stream()
                .filter(it -> {
                    int d = it.getDayOfWeek();
                    return d > todayDow || (d == todayDow && it.getStartMinute() >= todayMinute);
                })
                .toList();
        if (remaining.isEmpty()) {
            // Week is over for this course — fall back to next week
            remaining = items;
        }

        List<SpacesView.UpcomingScheduleEntry> out = new ArrayList<>();
        for (int i = 0; i < remaining.size() && i < 6; i++) {
            ScheduleItem it = remaining.get(i);
            DayOfWeek dow = DayOfWeek.of(it.getDayOfWeek());
            String dayLabel = dow.getDisplayName(TextStyle.SHORT, Locale.ENGLISH);
            String timeLabel = formatStartMinute(it.getStartMinute());
            String typeBadge = humanizeType(it.getItemType().name());
            out.add(new SpacesView.UpcomingScheduleEntry(
                    it,
                    dayLabel,
                    timeLabel,
                    i == 0,
                    typeBadge,
                    it.getLocation(),
                    it.getDurationMinutes()
            ));
        }
        return out;
    }

    private String buildScheduleSummary(List<ScheduleItem> items) {
        if (items.isEmpty()) {
            return null;
        }
        // Group by (startMinute, durationMinutes, location). Pick the largest group.
        record Key(int startMinute, Integer durationMinutes, String location) {}
        Map<Key, List<ScheduleItem>> groups = new LinkedHashMap<>();
        for (ScheduleItem it : items) {
            Key k = new Key(it.getStartMinute(), it.getDurationMinutes(), it.getLocation());
            groups.computeIfAbsent(k, kk -> new ArrayList<>()).add(it);
        }
        List<ScheduleItem> primary = groups.values().stream()
                .max(Comparator.comparingInt(List::size))
                .orElse(items);

        String dayLetters = primary.stream()
                .sorted(Comparator.comparingInt(ScheduleItem::getDayOfWeek))
                .map(it -> dayLetter(it.getDayOfWeek()))
                .reduce("", (a, b) -> a + b);

        ScheduleItem first = primary.get(0);
        String startLabel = formatStartMinuteHmm(first.getStartMinute());
        if (first.getDurationMinutes() == null) {
            return dayLetters + " " + startLabel;
        }
        int endMin = first.getStartMinute() + first.getDurationMinutes();
        String endLabel = formatStartMinuteHmm(endMin);
        return dayLetters + " " + startLabel + " — " + endLabel;
    }

    private static String dayLetter(int dow) {
        return switch (dow) {
            case 1 -> "M";
            case 2 -> "T";
            case 3 -> "W";
            case 4 -> "Th";
            case 5 -> "F";
            case 6 -> "Sa";
            case 7 -> "Su";
            default -> "";
        };
    }

    private static String formatStartMinute(int totalMinutes) {
        int hour = (totalMinutes / 60) % 24;
        int min = totalMinutes % 60;
        LocalTime t = LocalTime.of(hour, min);
        return min == 0
                ? t.format(DateTimeFormatter.ofPattern("h a", Locale.ENGLISH))
                : t.format(DateTimeFormatter.ofPattern("h:mm a", Locale.ENGLISH));
    }

    private static String formatStartMinuteHmm(int totalMinutes) {
        int hour = (totalMinutes / 60) % 24;
        int min = totalMinutes % 60;
        return String.format("%d:%02d", hour, min);
    }

    private static String humanizeType(String enumName) {
        if (enumName == null || enumName.isEmpty()) return "";
        return enumName.charAt(0) + enumName.substring(1).toLowerCase(Locale.ENGLISH);
    }

    private static String formatHoursMins(int minutes) {
        int m = Math.max(0, minutes);
        int h = m / 60;
        int mm = m % 60;
        if (h == 0) return mm + "m";
        if (mm == 0) return h + "h";
        return h + "h " + mm + "m";
    }

    private static String formatDelta(int delta) {
        if (Math.abs(delta) < 5) return "Steady this week";
        String sign = delta > 0 ? "+ " : "- ";
        int abs = Math.abs(delta);
        return sign + formatHoursMins(abs) + " vs last week";
    }
}
