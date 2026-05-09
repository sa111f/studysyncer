package com.studysyncer.backend.service;

import com.studysyncer.backend.domain.Exam;
import com.studysyncer.backend.domain.ExamStatus;
import com.studysyncer.backend.domain.StudySession;
import com.studysyncer.backend.domain.Task;
import com.studysyncer.backend.domain.TaskStatus;
import com.studysyncer.backend.domain.User;
import com.studysyncer.backend.repository.DailyGoalRepository;
import com.studysyncer.backend.repository.ExamRepository;
import com.studysyncer.backend.repository.StudySessionRepository;
import com.studysyncer.backend.repository.TaskRepository;
import com.studysyncer.backend.web.view.TodayView;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.time.temporal.WeekFields;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

@Service
public class TodayService {

    private static final DateTimeFormatter DATE_LINE = DateTimeFormatter.ofPattern("EEEE, MMMM d", Locale.ENGLISH);
    private static final DateTimeFormatter WEEKDAY = DateTimeFormatter.ofPattern("EEEE", Locale.ENGLISH);

    private final TaskRepository taskRepository;
    private final ExamRepository examRepository;
    private final StudySessionRepository studySessionRepository;
    private final DailyGoalRepository dailyGoalRepository;

    public TodayService(TaskRepository taskRepository,
                        ExamRepository examRepository,
                        StudySessionRepository studySessionRepository,
                        DailyGoalRepository dailyGoalRepository) {
        this.taskRepository = taskRepository;
        this.examRepository = examRepository;
        this.studySessionRepository = studySessionRepository;
        this.dailyGoalRepository = dailyGoalRepository;
    }

    @Transactional(readOnly = true)
    public TodayView buildFor(User user) {
        ZoneId tz = ZoneId.systemDefault();
        ZonedDateTime now = ZonedDateTime.now(tz);
        ZonedDateTime startOfToday = now.toLocalDate().atStartOfDay(tz);
        ZonedDateTime endOfToday = startOfToday.plusDays(1);
        ZonedDateTime startOfWeek = startOfToday.with(DayOfWeek.MONDAY);
        if (startOfWeek.isAfter(startOfToday)) {
            startOfWeek = startOfWeek.minusWeeks(1);
        }
        ZonedDateTime endOfWeek = startOfWeek.plusWeeks(1);

        List<Task> overdue = taskRepository.findByUserAndStatusAndDueAtBeforeOrderByDueAtAsc(
                user, TaskStatus.PENDING, startOfToday.toInstant());
        List<Task> todayTasks = taskRepository.findByUserAndStatusAndDueAtBetweenOrderByDueAtAsc(
                user, TaskStatus.PENDING, startOfToday.toInstant(), endOfToday.toInstant());
        List<Task> weekTasks = taskRepository.findByUserAndStatusAndDueAtBetweenOrderByDueAtAsc(
                user, TaskStatus.PENDING, endOfToday.toInstant(), endOfWeek.toInstant());

        StudySession activeSession = studySessionRepository
                .findFirstByUserAndEndedAtIsNullOrderByStartedAtDesc(user)
                .orElse(null);
        Long activeElapsed = activeSession == null
                ? null
                : Duration.between(activeSession.getStartedAt(), Instant.now()).getSeconds();

        List<StudySession> todaysSessions = studySessionRepository.findByUserAndStartedAtBetween(
                user, startOfToday.toInstant(), endOfToday.toInstant());
        long todaySeconds = todaysSessions.stream()
                .mapToLong(s -> s.getDurationSeconds() == null ? 0L : s.getDurationSeconds())
                .sum();
        int minutesStudiedToday = (int) (todaySeconds / 60);

        int dailyGoalMinutes = dailyGoalRepository.findById(user.getId())
                .map(g -> g.getMinutesPerDay() == null ? 150 : g.getMinutesPerDay())
                .orElse(150);
        int dailyGoalPercent = Math.min(100, (int) Math.round(
                100.0 * minutesStudiedToday / Math.max(1, dailyGoalMinutes)));
        int dailyGoalRemaining = Math.max(0, dailyGoalMinutes - minutesStudiedToday);

        int streak = computeStreak(user, startOfToday, tz);

        List<Exam> upcomingExams = examRepository.findTop2ByUserAndStatusOrderByStartsAtAsc(
                user, ExamStatus.UPCOMING);

        List<StudySession> weekSessions = studySessionRepository.findByUserAndStartedAtBetween(
                user, startOfWeek.toInstant(), endOfWeek.toInstant());
        int weekMinutes = (int) (weekSessions.stream()
                .mapToLong(s -> s.getDurationSeconds() == null ? 0L : s.getDurationSeconds())
                .sum() / 60);

        long weekTasksCompleted = taskRepository.countByUserAndStatusAndCompletedAtBetween(
                user, TaskStatus.COMPLETED, startOfWeek.toInstant(), endOfWeek.toInstant());
        long weekTasksPending = taskRepository.countByUserAndStatusAndDueAtBetween(
                user, TaskStatus.PENDING, startOfWeek.toInstant(), endOfWeek.toInstant());
        int weekTotal = (int) (weekTasksCompleted + weekTasksPending);

        String timeOfDay = timeOfDay(now.getHour());
        String firstName = user.getFirstName() == null ? "there" : user.getFirstName();
        String dateLine = DATE_LINE.format(now.toLocalDate());
        int weekNum = now.toLocalDate().get(WeekFields.ISO.weekOfWeekBasedYear());
        String eyebrow = WEEKDAY.format(now.toLocalDate()) + " " + timeOfDay + " · Week " + weekNum;

        String subtitle = composeSubtitle(todayTasks.size(), overdue.size(), upcomingExams,
                startOfToday, tz);

        return new TodayView(
                timeOfDay,
                firstName,
                dateLine,
                eyebrow,
                subtitle,
                activeSession,
                activeElapsed,
                overdue,
                todayTasks,
                weekTasks,
                dailyGoalMinutes,
                minutesStudiedToday,
                dailyGoalPercent,
                dailyGoalRemaining,
                streak,
                upcomingExams,
                (int) weekTasksCompleted,
                weekTotal,
                weekMinutes
        );
    }

    private int computeStreak(User user, ZonedDateTime startOfToday, ZoneId tz) {
        int streak = 0;
        ZonedDateTime cursor = startOfToday;
        for (int i = 0; i < 366; i++) {
            ZonedDateTime dayStart = cursor;
            ZonedDateTime dayEnd = cursor.plusDays(1);
            List<StudySession> sessions = studySessionRepository.findByUserAndStartedAtBetween(
                    user, dayStart.toInstant(), dayEnd.toInstant());
            if (sessions.isEmpty()) {
                break;
            }
            streak++;
            cursor = cursor.minusDays(1);
        }
        return streak;
    }

    private static String timeOfDay(int hour) {
        if (hour < 5) return "evening";
        if (hour < 12) return "morning";
        if (hour < 17) return "afternoon";
        return "evening";
    }

    private static String composeSubtitle(int todayCount, int overdueCount, List<Exam> upcoming,
                                          ZonedDateTime startOfToday, ZoneId tz) {
        List<String> pieces = new ArrayList<>();
        if (todayCount > 0) {
            pieces.add(spell(todayCount) + " " + (todayCount == 1 ? "thing" : "things") + " due tonight");
        }
        if (overdueCount > 0) {
            pieces.add(spell(overdueCount) + " " + (overdueCount == 1 ? "writeup" : "items") + " overdue");
        }
        if (!upcoming.isEmpty()) {
            Exam e = upcoming.get(0);
            long days = ChronoUnit.DAYS.between(startOfToday.toLocalDate(),
                    e.getStartsAt().atZone(tz).toLocalDate());
            if (days >= 0) {
                pieces.add(e.getCourse().getCode() + " is in " + spell((int) days) + " "
                        + (days == 1 ? "day" : "days"));
            }
        }
        if (pieces.isEmpty()) {
            return "Nothing pressing today. Take a breath.";
        }
        String joined;
        if (pieces.size() == 1) {
            joined = pieces.get(0);
        } else if (pieces.size() == 2) {
            joined = pieces.get(0) + ", and " + pieces.get(1);
        } else {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < pieces.size(); i++) {
                if (i == pieces.size() - 1) {
                    sb.append(", and ").append(pieces.get(i));
                } else if (i == 0) {
                    sb.append(pieces.get(i));
                } else {
                    sb.append(", ").append(pieces.get(i));
                }
            }
            joined = sb.toString();
        }
        String capitalized = Character.toUpperCase(joined.charAt(0)) + joined.substring(1);
        return capitalized + ".";
    }

    private static String spell(int n) {
        return switch (n) {
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
            case 13 -> "thirteen";
            case 14 -> "fourteen";
            case 15 -> "fifteen";
            case 16 -> "sixteen";
            case 17 -> "seventeen";
            case 18 -> "eighteen";
            case 19 -> "nineteen";
            case 20 -> "twenty";
            default -> Integer.toString(n);
        };
    }
}
