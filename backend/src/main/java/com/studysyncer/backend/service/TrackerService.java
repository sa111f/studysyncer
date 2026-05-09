package com.studysyncer.backend.service;

import com.studysyncer.backend.domain.Course;
import com.studysyncer.backend.domain.DailyGoal;
import com.studysyncer.backend.domain.StudySession;
import com.studysyncer.backend.domain.User;
import com.studysyncer.backend.repository.CourseRepository;
import com.studysyncer.backend.repository.DailyGoalRepository;
import com.studysyncer.backend.repository.StudySessionRepository;
import com.studysyncer.backend.web.view.Formatters;
import com.studysyncer.backend.web.view.TrackerView;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.WeekFields;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class TrackerService {

    private static final DateTimeFormatter SHORT_DAY = DateTimeFormatter.ofPattern("EEE", Locale.ENGLISH);
    private static final DateTimeFormatter LONG_WEEKDAY = DateTimeFormatter.ofPattern("EEEE", Locale.ENGLISH);
    private static final DateTimeFormatter MONTH_D = DateTimeFormatter.ofPattern("MMM d", Locale.ENGLISH);
    private static final DateTimeFormatter D = DateTimeFormatter.ofPattern("d", Locale.ENGLISH);
    private static final DateTimeFormatter D_YYYY = DateTimeFormatter.ofPattern("d, yyyy", Locale.ENGLISH);
    private static final DateTimeFormatter H_MM_A = DateTimeFormatter.ofPattern("h:mm a", Locale.ENGLISH);
    private static final String[] WEEKDAY_LETTERS = { "S", "M", "T", "W", "T", "F", "S" };
    private static final int RING_CIRCUMFERENCE = 264;

    private final StudySessionRepository sessionRepo;
    private final CourseRepository courseRepo;
    private final DailyGoalRepository goalRepo;
    private final Formatters formatters;

    public TrackerService(StudySessionRepository sessionRepo,
                          CourseRepository courseRepo,
                          DailyGoalRepository goalRepo,
                          Formatters formatters) {
        this.sessionRepo = sessionRepo;
        this.courseRepo = courseRepo;
        this.goalRepo = goalRepo;
        this.formatters = formatters;
    }

    @Transactional(readOnly = true)
    public TrackerView buildFor(User user, String range) {
        ZoneId tz = ZoneId.systemDefault();
        ZonedDateTime now = ZonedDateTime.now(tz);
        ZonedDateTime startOfToday = now.toLocalDate().atStartOfDay(tz);

        // Sunday-first grid: Sunday at or before today
        int dow = now.getDayOfWeek().getValue(); // Mon=1..Sun=7
        int daysBackToSunday = (dow == 7) ? 0 : dow;
        ZonedDateTime startOfGrid = startOfToday.minusDays(daysBackToSunday);
        ZonedDateTime endOfGrid = startOfGrid.plusDays(7);

        List<StudySession> weekSessions = sessionRepo.findByUserAndStartedAtBetween(
                user, startOfGrid.toInstant(), endOfGrid.toInstant());

        // ─── Per-day chart data ───
        int[] dayMinutes = new int[7];
        // dayCourseMinutes[i] = LinkedHashMap of courseId -> minutes (preserves first-seen order)
        @SuppressWarnings("unchecked")
        Map<Long, Integer>[] dayCourseMinutes = new LinkedHashMap[7];
        Map<Long, Course> courseById = new HashMap<>();
        for (int i = 0; i < 7; i++) dayCourseMinutes[i] = new LinkedHashMap<>();

        for (StudySession s : weekSessions) {
            ZonedDateTime startedAtTz = s.getStartedAt().atZone(tz);
            int idx = (int) java.time.temporal.ChronoUnit.DAYS.between(
                    startOfGrid.toLocalDate(), startedAtTz.toLocalDate());
            if (idx < 0 || idx >= 7) continue;
            int mins = s.getDurationSeconds() == null ? 0 : (s.getDurationSeconds() / 60);
            dayMinutes[idx] += mins;
            if (s.getCourse() != null) {
                courseById.put(s.getCourse().getId(), s.getCourse());
                dayCourseMinutes[idx].merge(s.getCourse().getId(), mins, Integer::sum);
            }
        }

        int maxDay = 0;
        for (int v : dayMinutes) if (v > maxDay) maxDay = v;
        int yAxisMaxMinutes = Math.max(240, ceilToNearest(maxDay, 60));
        List<String> yAxisLabels = buildYAxisLabels(yAxisMaxMinutes);

        // Build day columns
        List<TrackerView.DayColumn> dayColumns = new ArrayList<>(7);
        for (int i = 0; i < 7; i++) {
            ZonedDateTime day = startOfGrid.plusDays(i);
            boolean isToday = day.toLocalDate().isEqual(now.toLocalDate());
            int total = dayMinutes[i];
            String shortDay = SHORT_DAY.format(day.toLocalDate());
            String dateNum = MONTH_D.format(day.toLocalDate());
            String totalLabel = total == 0 ? "—" : fmtHrsMins(total);
            String tooltip;
            if (isToday) {
                tooltip = total == 0
                        ? shortDay + " · today · no sessions yet"
                        : shortDay + " · today · " + fmtHrsMins(total) + " so far";
            } else if (total == 0) {
                tooltip = shortDay + " · no sessions";
            } else {
                tooltip = shortDay + " · " + fmtHrsMins(total);
            }

            // Build segments sorted by course displayOrder
            List<Map.Entry<Long, Integer>> entries = new ArrayList<>(dayCourseMinutes[i].entrySet());
            entries.sort(Comparator.comparingInt(e -> {
                Course c = courseById.get(e.getKey());
                return c == null || c.getDisplayOrder() == null ? Integer.MAX_VALUE : c.getDisplayOrder();
            }));
            List<TrackerView.BarSeg> segs = new ArrayList<>();
            for (Map.Entry<Long, Integer> e : entries) {
                Course c = courseById.get(e.getKey());
                if (e.getValue() <= 0) continue;
                double pct = 100.0 * e.getValue() / yAxisMaxMinutes;
                segs.add(new TrackerView.BarSeg(formatters.pillClass(c), e.getValue(), pct));
            }

            dayColumns.add(new TrackerView.DayColumn(
                    shortDay, dateNum, isToday, total, totalLabel, tooltip, segs));
        }

        TrackerView.Chart chart = new TrackerView.Chart(yAxisMaxMinutes, yAxisLabels, dayColumns);

        // ─── Summary ───
        int totalMinutes = 0;
        for (int v : dayMinutes) totalMinutes += v;

        // Last week
        ZonedDateTime startOfLastWeek = startOfGrid.minusDays(7);
        List<StudySession> lastWeek = sessionRepo.findByUserAndStartedAtBetween(
                user, startOfLastWeek.toInstant(), startOfGrid.toInstant());
        int lastWeekMinutes = lastWeek.stream()
                .mapToInt(s -> s.getDurationSeconds() == null ? 0 : (s.getDurationSeconds() / 60))
                .sum();
        int delta = totalMinutes - lastWeekMinutes;
        String deltaLabel;
        if (lastWeekMinutes == 0 && totalMinutes > 0) {
            deltaLabel = "First week tracking — well begun.";
        } else if (Math.abs(delta) < 5) {
            deltaLabel = "Steady pace this week.";
        } else if (delta > 0) {
            deltaLabel = "+ " + fmtHrsMins(Math.abs(delta)) + " over last week's pace.";
        } else {
            deltaLabel = "- " + fmtHrsMins(Math.abs(delta)) + " under last week's pace.";
        }

        int dailyGoalMinutes = goalRepo.findById(user.getId())
                .map(g -> g.getMinutesPerDay() == null ? 150 : g.getMinutesPerDay())
                .orElse(150);

        int dailyAvgMinutes = totalMinutes / 7;
        String dailyAvgLabel = fmtHrsMins(dailyAvgMinutes);
        String dailyAvgTarget = "target " + fmtHrsMins(dailyGoalMinutes);

        int sessionsCount = weekSessions.size();
        int avgSessionMins = sessionsCount == 0 ? 0 : totalMinutes / sessionsCount;
        String sessionsAvgLabel = "avg " + avgSessionMins + " min";

        int pomodorosCount = totalMinutes / 25;
        String pomodorosCompletionLabel = "~93% completion";

        int streakDays = computeStreak(user, startOfToday, tz);
        String streakBestLabel = "best · 12 days";

        TrackerView.Summary summary = new TrackerView.Summary(
                totalMinutes,
                fmtHrsMins(totalMinutes),
                deltaLabel,
                dailyAvgMinutes, dailyAvgLabel, dailyAvgTarget,
                sessionsCount, sessionsAvgLabel,
                pomodorosCount, pomodorosCompletionLabel,
                streakDays, streakBestLabel
        );

        // ─── Recent sessions ───
        List<StudySession> recentDesc = sessionRepo.findByUserAndStartedAtBetweenOrderByStartedAtDesc(
                user, startOfGrid.toInstant(), endOfGrid.toInstant());
        List<TrackerView.RecentSession> recents = new ArrayList<>();
        int cap = Math.min(8, recentDesc.size());
        for (int i = 0; i < cap; i++) {
            StudySession s = recentDesc.get(i);
            ZonedDateTime startedAtTz = s.getStartedAt().atZone(tz);
            String when;
            if (startedAtTz.toLocalDate().isEqual(now.toLocalDate())) {
                when = "Today";
            } else {
                when = SHORT_DAY.format(startedAtTz.toLocalDate());
            }
            int durMin = s.getDurationSeconds() == null ? 0 : (s.getDurationSeconds() / 60);
            int pomos = Math.max(1, durMin / 25);
            recents.add(new TrackerView.RecentSession(
                    when,
                    H_MM_A.format(startedAtTz),
                    s.getTitle(),
                    s.getItalicSuffix(),
                    s.getCourse() == null ? "" : formatters.pillClass(s.getCourse()),
                    s.getCourse() == null ? "" : s.getCourse().getCode(),
                    pomos,
                    fmtHrsMins(durMin)
            ));
        }

        // ─── Streak dots — aligned with the grid (Sun..Sat) ───
        List<TrackerView.StreakDot> dots = new ArrayList<>(7);
        for (int i = 0; i < 7; i++) {
            ZonedDateTime day = startOfGrid.plusDays(i);
            boolean isToday = day.toLocalDate().isEqual(now.toLocalDate());
            int mins = dayMinutes[i];
            String klass;
            if (mins >= dailyGoalMinutes) klass = "f";
            else if (mins > 0) klass = "partial";
            else klass = "";
            if (isToday) klass = klass.isEmpty() ? "today" : klass + " today";
            dots.add(new TrackerView.StreakDot(klass, WEEKDAY_LETTERS[i]));
        }
        TrackerView.Streak streak = new TrackerView.Streak(
                streakDays,
                streakDays + " day" + (streakDays == 1 ? "" : "s") + " strong",
                streakBestLabel,
                dots
        );

        // ─── Course breakdown ───
        Map<Long, Integer> minutesByCourse = new LinkedHashMap<>();
        for (Course c : courseRepo.findByUserOrderByDisplayOrderAsc(user)) {
            minutesByCourse.put(c.getId(), 0);
            courseById.putIfAbsent(c.getId(), c);
        }
        for (StudySession s : weekSessions) {
            if (s.getCourse() == null) continue;
            int mins = s.getDurationSeconds() == null ? 0 : (s.getDurationSeconds() / 60);
            minutesByCourse.merge(s.getCourse().getId(), mins, Integer::sum);
        }
        int maxCourseMinutes = 0;
        for (int v : minutesByCourse.values()) if (v > maxCourseMinutes) maxCourseMinutes = v;

        List<TrackerView.CourseBreakdown> byCourse = new ArrayList<>();
        // Sort by minutes desc for nicer visual ordering
        List<Map.Entry<Long, Integer>> sortedCourse = new ArrayList<>(minutesByCourse.entrySet());
        sortedCourse.sort((a, b) -> Integer.compare(b.getValue(), a.getValue()));
        for (Map.Entry<Long, Integer> e : sortedCourse) {
            Course c = courseById.get(e.getKey());
            int mins = e.getValue();
            int pct = maxCourseMinutes == 0 ? 0 : (int) Math.round(100.0 * mins / maxCourseMinutes);
            byCourse.add(new TrackerView.CourseBreakdown(
                    c, mins,
                    mins == 0 ? "0m" : fmtHrsMins(mins),
                    formatters.pebbleClass(c),
                    formatters.fillGradient(c),
                    pct
            ));
        }

        // ─── Goal (today) ───
        ZonedDateTime endOfToday = startOfToday.plusDays(1);
        List<StudySession> todaysSessions = sessionRepo.findByUserAndStartedAtBetween(
                user, startOfToday.toInstant(), endOfToday.toInstant());
        int minutesToday = todaysSessions.stream()
                .mapToInt(s -> s.getDurationSeconds() == null ? 0 : (s.getDurationSeconds() / 60))
                .sum();
        int goalPercent = Math.min(100, (int) Math.round(100.0 * minutesToday / Math.max(1, dailyGoalMinutes)));
        int dashOffset = RING_CIRCUMFERENCE - (RING_CIRCUMFERENCE * goalPercent / 100);
        int remaining = Math.max(0, dailyGoalMinutes - minutesToday);
        String hoursLabel = minutesToday + " of " + dailyGoalMinutes + " min";
        String stateLine = remaining > 0
                ? "~" + remaining + "m to go · " + streakDays + "d streak"
                : "Done for today · " + streakDays + "d streak";
        TrackerView.Goal goal = new TrackerView.Goal(
                minutesToday, dailyGoalMinutes, goalPercent, dashOffset,
                remaining, hoursLabel, stateLine
        );

        // ─── Best time insight ───
        String bestInsight = computePeakInsight(weekSessions, tz);

        // ─── Page header strings ───
        int weekNum = now.toLocalDate().get(WeekFields.ISO.weekOfWeekBasedYear());
        String eyebrow = LONG_WEEKDAY.format(now.toLocalDate()) + " · Week " + weekNum;

        String weekRangeLabel = MONTH_D.format(startOfGrid.toLocalDate())
                + " – " + D_YYYY.format(endOfGrid.minusDays(1).toLocalDate());

        String subtitle = composeSubtitle(totalMinutes, byCourse, streakDays);

        return new TrackerView(
                eyebrow,
                subtitle,
                "week",
                weekRangeLabel,
                summary,
                chart,
                recents,
                streak,
                byCourse,
                goal,
                bestInsight
        );
    }

    private int computeStreak(User user, ZonedDateTime startOfToday, ZoneId tz) {
        int streak = 0;
        ZonedDateTime cursor = startOfToday;
        for (int i = 0; i < 366; i++) {
            ZonedDateTime dayStart = cursor;
            ZonedDateTime dayEnd = cursor.plusDays(1);
            List<StudySession> sessions = sessionRepo.findByUserAndStartedAtBetween(
                    user, dayStart.toInstant(), dayEnd.toInstant());
            if (sessions.isEmpty()) break;
            streak++;
            cursor = cursor.minusDays(1);
        }
        return streak;
    }

    private String computePeakInsight(List<StudySession> sessions, ZoneId tz) {
        if (sessions.isEmpty()) {
            return "Log a few sessions and we'll spot your peak hours.";
        }
        int[] minutesByHour = new int[24];
        for (StudySession s : sessions) {
            int hour = s.getStartedAt().atZone(tz).getHour();
            int mins = s.getDurationSeconds() == null ? 0 : (s.getDurationSeconds() / 60);
            minutesByHour[hour] += mins;
        }
        int peakHour = 0;
        int peakMins = 0;
        for (int h = 0; h < 24; h++) {
            if (minutesByHour[h] > peakMins) {
                peakMins = minutesByHour[h];
                peakHour = h;
            }
        }
        if (peakMins == 0) {
            return "Log a few sessions and we'll spot your peak hours.";
        }
        return "You're most focused between <b>"
                + formatHourRange(peakHour, peakHour + 1)
                + "</b>. Schedule deep work then.";
    }

    private static String formatHourRange(int from, int to) {
        // 13–14 → "1 – 2 PM", 9–10 → "9 – 10 AM", 23–24 → "11 PM – 12 AM"
        int toHour = to % 24;
        return formatHour(from) + " – " + formatHour(toHour);
    }

    private static String formatHour(int hour) {
        if (hour == 0) return "12 AM";
        if (hour == 12) return "12 PM";
        if (hour < 12) return hour + " AM";
        return (hour - 12) + " PM";
    }

    private static int ceilToNearest(int value, int step) {
        if (value <= 0) return 0;
        int rem = value % step;
        return rem == 0 ? value : value + (step - rem);
    }

    private static List<String> buildYAxisLabels(int yAxisMaxMinutes) {
        // 5 evenly-spaced labels top-to-bottom (max → 0)
        int hours = yAxisMaxMinutes / 60;
        // Quartile labels: max, 3*max/4 ... actually for design feel use whole-hour steps.
        // We always have 5 labels; spread evenly in hours.
        List<String> out = new ArrayList<>(5);
        for (int i = 4; i >= 0; i--) {
            int hStep = (int) Math.round(hours * (i / 4.0));
            out.add(hStep + "h");
        }
        return out;
    }

    private static String fmtHrsMins(int totalMinutes) {
        if (totalMinutes <= 0) return "0m";
        int h = totalMinutes / 60;
        int m = totalMinutes % 60;
        if (h == 0) return m + "m";
        if (m == 0) return h + "h";
        return h + "h " + (m < 10 ? "0" + m : Integer.toString(m)) + "m";
    }

    private static String composeSubtitle(int totalMinutes,
                                          List<TrackerView.CourseBreakdown> byCourse,
                                          int streakDays) {
        if (totalMinutes == 0) {
            return "No sessions logged this week. Today's a good day to start.";
        }
        int hours = (int) Math.round(totalMinutes / 60.0);
        StringBuilder sb = new StringBuilder();
        sb.append(hours).append(hours == 1 ? " hour" : " hours").append(" this week");
        // Top 1-2 courses by minutes
        List<String> topCourses = new ArrayList<>();
        for (TrackerView.CourseBreakdown cb : byCourse) {
            if (cb.weekMinutes() <= 0) continue;
            topCourses.add(cb.course().getCode());
            if (topCourses.size() == 2) break;
        }
        if (!topCourses.isEmpty()) {
            sb.append(", mostly ").append(String.join(" and ", topCourses));
        }
        sb.append(". ");
        if (streakDays > 0) {
            sb.append(streakDays).append(streakDays == 1 ? "-day streak running." : "-day streak running.");
        } else {
            sb.append("Time to start a new streak.");
        }
        return sb.toString();
    }
}
