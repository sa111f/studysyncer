package com.studysyncer.backend.service;

import com.studysyncer.backend.domain.Course;
import com.studysyncer.backend.domain.Task;
import com.studysyncer.backend.domain.TaskStatus;
import com.studysyncer.backend.domain.User;
import com.studysyncer.backend.repository.CourseRepository;
import com.studysyncer.backend.repository.TaskRepository;
import com.studysyncer.backend.web.view.Formatters;
import com.studysyncer.backend.web.view.TasksView;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.WeekFields;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Service
public class TasksService {

    private static final DateTimeFormatter WEEKDAY_LONG = DateTimeFormatter.ofPattern("EEEE", Locale.ENGLISH);

    private final TaskRepository taskRepository;
    private final CourseRepository courseRepository;
    private final Formatters formatters;

    public TasksService(TaskRepository taskRepository,
                        CourseRepository courseRepository,
                        Formatters formatters) {
        this.taskRepository = taskRepository;
        this.courseRepository = courseRepository;
        this.formatters = formatters;
    }

    @Transactional(readOnly = true)
    public TasksView buildFor(User user, String filter) {
        ZoneId tz = ZoneId.systemDefault();
        ZonedDateTime now = ZonedDateTime.now(tz);
        ZonedDateTime startOfToday = now.toLocalDate().atStartOfDay(tz);
        ZonedDateTime endOfToday = startOfToday.plusDays(1);
        ZonedDateTime startOfWeek = startOfToday.with(DayOfWeek.MONDAY);
        if (startOfWeek.isAfter(startOfToday)) {
            startOfWeek = startOfWeek.minusWeeks(1);
        }
        ZonedDateTime endOfWeek = startOfWeek.plusWeeks(1);

        long countAll = taskRepository.countByUser(user);
        long countActive = taskRepository.countByUserAndStatus(user, TaskStatus.PENDING);
        long countCompleted = taskRepository.countByUserAndStatus(user, TaskStatus.COMPLETED);
        long countOverdue = taskRepository.countByUserAndStatusAndDueAtBefore(
                user, TaskStatus.PENDING, startOfToday.toInstant());

        List<TasksView.TaskGroup> groups = new ArrayList<>();

        switch (filter) {
            case "overdue" -> {
                List<Task> overdue = taskRepository.findByUserAndStatusAndDueAtBeforeOrderByDueAtAsc(
                        user, TaskStatus.PENDING, startOfToday.toInstant());
                addIfNonEmpty(groups, "Overdue", "— attend first", "alert", overdue);
            }
            case "completed" -> {
                List<Task> completed = taskRepository
                        .findByUserAndStatusAndCompletedAtBetweenOrderByCompletedAtDesc(
                                user, TaskStatus.COMPLETED,
                                Instant.EPOCH, Instant.now());
                addIfNonEmpty(groups, "Completed", null, null, completed);
            }
            default -> {
                List<Task> overdue = taskRepository.findByUserAndStatusAndDueAtBeforeOrderByDueAtAsc(
                        user, TaskStatus.PENDING, startOfToday.toInstant());
                List<Task> today = taskRepository.findByUserAndStatusAndDueAtBetweenOrderByDueAtAsc(
                        user, TaskStatus.PENDING, startOfToday.toInstant(), endOfToday.toInstant());
                List<Task> week = taskRepository.findByUserAndStatusAndDueAtBetweenOrderByDueAtAsc(
                        user, TaskStatus.PENDING, endOfToday.toInstant(), endOfWeek.toInstant());
                List<Task> later = taskRepository.findByUserAndStatusAndDueAtGreaterThanEqualOrderByDueAtAsc(
                        user, TaskStatus.PENDING, endOfWeek.toInstant());

                addIfNonEmpty(groups, "Overdue", "— attend first", "alert", overdue);
                addIfNonEmpty(groups, "Today", null, null, today);
                addIfNonEmpty(groups, "This week", "— ahead", null, week);
                addIfNonEmpty(groups, "Later", null, null, later);

                if ("all".equals(filter)) {
                    List<Task> doneToday = taskRepository
                            .findByUserAndStatusAndCompletedAtBetweenOrderByCompletedAtDesc(
                                    user, TaskStatus.COMPLETED,
                                    startOfToday.toInstant(), endOfToday.toInstant());
                    addIfNonEmpty(groups, "Completed today", "— well done", null, doneToday);
                }
            }
        }

        long weekDone = taskRepository.countByUserAndStatusAndCompletedAtBetween(
                user, TaskStatus.COMPLETED, startOfWeek.toInstant(), endOfWeek.toInstant());
        long weekActiveInWeek = taskRepository.countByUserAndStatusAndDueAtBetween(
                user, TaskStatus.PENDING, startOfWeek.toInstant(), endOfWeek.toInstant());
        long weekTotal = weekDone + weekActiveInWeek;

        // Estimate remaining: sum estimatedMinutes of PENDING tasks due this week
        List<Task> weekPending = taskRepository.findByUserAndStatusAndDueAtBetweenOrderByDueAtAsc(
                user, TaskStatus.PENDING, startOfWeek.toInstant(), endOfWeek.toInstant());
        int remainingMinutes = weekPending.stream()
                .mapToInt(t -> t.getEstimatedMinutes() == null ? 0 : t.getEstimatedMinutes())
                .sum();
        int remainingHours = (int) Math.round(remainingMinutes / 60.0);

        // Build by-course progress
        List<TasksView.CourseProgress> byCourse = new ArrayList<>();
        for (Course course : courseRepository.findByUserOrderByDisplayOrderAsc(user)) {
            long total = taskRepository.countByUserAndCourse(user, course);
            long done = taskRepository.countByUserAndCourseAndStatus(user, course, TaskStatus.COMPLETED);
            int percent = total == 0 ? 0 : (int) Math.round(100.0 * done / total);
            byCourse.add(new TasksView.CourseProgress(
                    course, done, total, percent,
                    formatters.pebbleClass(course),
                    formatters.fillGradient(course)));
        }

        // AI suggestion: reference the most overdue task title
        List<Task> overdueForAi = taskRepository.findByUserAndStatusAndDueAtBeforeOrderByDueAtAsc(
                user, TaskStatus.PENDING, startOfToday.toInstant());
        String aiSuggestion;
        if (!overdueForAi.isEmpty()) {
            Task t = overdueForAi.get(0);
            String est = t.getEstimatedMinutes() == null
                    ? "a quick"
                    : "~" + t.getEstimatedMinutes() + " min";
            aiSuggestion = "Tackle " + t.getTitle() + " first — it's overdue and will only take "
                    + est + ". Then ride momentum into what's next.";
        } else {
            aiSuggestion = "All caught up — start with what's next.";
        }

        // Counts for week legend
        long weekOverdue = taskRepository.countByUserAndStatusAndDueAtBefore(
                user, TaskStatus.PENDING, startOfToday.toInstant());

        // Page eyebrow + subtitle
        int weekNum = now.toLocalDate().get(WeekFields.ISO.weekOfWeekBasedYear());
        String eyebrow = WEEKDAY_LONG.format(now.toLocalDate()) + " · Week " + weekNum;

        long todayCount = taskRepository.countByUserAndStatusAndDueAtBetween(
                user, TaskStatus.PENDING, startOfToday.toInstant(), endOfToday.toInstant());
        long thisWeekRemainCount = taskRepository.countByUserAndStatusAndDueAtBetween(
                user, TaskStatus.PENDING, endOfToday.toInstant(), endOfWeek.toInstant());
        String subtitle = composeSubtitle(countOverdue, todayCount, thisWeekRemainCount);

        return new TasksView(
                eyebrow,
                subtitle,
                countAll,
                countActive,
                countCompleted,
                countOverdue,
                filter,
                groups,
                (int) weekDone,
                (int) weekTotal,
                remainingHours,
                weekOverdue,
                weekActiveInWeek,
                weekDone,
                byCourse,
                aiSuggestion
        );
    }

    @Transactional
    public void toggleTaskCompletion(User user, Long taskId) {
        Task t = taskRepository.findById(taskId)
                .orElseThrow(() -> new AccessDeniedException("Not your task"));
        if (!t.getUser().getId().equals(user.getId())) {
            throw new AccessDeniedException("Not your task");
        }
        if (t.getStatus() == TaskStatus.COMPLETED) {
            t.setStatus(TaskStatus.PENDING);
            t.setCompletedAt(null);
        } else {
            t.setStatus(TaskStatus.COMPLETED);
            t.setCompletedAt(Instant.now());
        }
        taskRepository.save(t);
    }

    private static void addIfNonEmpty(List<TasksView.TaskGroup> groups, String heading,
                                      String italic, String cssExtra, List<Task> tasks) {
        if (tasks != null && !tasks.isEmpty()) {
            groups.add(new TasksView.TaskGroup(heading, italic, cssExtra, tasks));
        }
    }

    private static String composeSubtitle(long overdue, long todayCount, long weekRemaining) {
        List<String> pieces = new ArrayList<>();
        if (overdue > 0) {
            pieces.add(spell(overdue) + " " + (overdue == 1 ? "writeup" : "writeups") + " overdue");
        }
        if (todayCount > 0) {
            pieces.add(spell(todayCount) + " " + (todayCount == 1 ? "thing" : "things") + " due tonight");
        }
        if (weekRemaining > 0) {
            pieces.add(spell(weekRemaining) + " before the weekend");
        }
        if (pieces.isEmpty()) {
            return "Inbox zero. Take a breath.";
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < pieces.size(); i++) {
            String p = pieces.get(i);
            if (i == 0) {
                sb.append(Character.toUpperCase(p.charAt(0))).append(p.substring(1));
            } else {
                sb.append(" ").append(Character.toUpperCase(p.charAt(0))).append(p.substring(1));
            }
            sb.append(".");
        }
        return sb.toString();
    }

    private static String spell(long n) {
        return switch ((int) n) {
            case 0 -> "zero";
            case 1 -> "one";
            case 2 -> "two";
            case 3 -> "three";
            case 4 -> "four";
            case 5 -> "five";
            case 6 -> "six";
            case 7 -> "seven";
            case 8 -> "eight";
            case 9 -> "nine";
            case 10 -> "ten";
            case 11 -> "eleven";
            case 12 -> "twelve";
            default -> Long.toString(n);
        };
    }
}
