package com.studysyncer.backend.service;

import com.studysyncer.backend.domain.Course;
import com.studysyncer.backend.domain.Exam;
import com.studysyncer.backend.domain.ExamStatus;
import com.studysyncer.backend.domain.ExamTopic;
import com.studysyncer.backend.domain.StudySession;
import com.studysyncer.backend.domain.TopicStatus;
import com.studysyncer.backend.domain.User;
import com.studysyncer.backend.repository.CourseRepository;
import com.studysyncer.backend.repository.ExamRepository;
import com.studysyncer.backend.repository.ExamTopicRepository;
import com.studysyncer.backend.repository.StudySessionRepository;
import com.studysyncer.backend.web.view.ExamsView;
import com.studysyncer.backend.web.view.Formatters;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.Month;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.time.temporal.WeekFields;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

@Service
public class ExamsService {

    private static final DateTimeFormatter WEEKDAY_LONG = DateTimeFormatter.ofPattern("EEEE", Locale.ENGLISH);
    private static final DateTimeFormatter EEE_MMM_D = DateTimeFormatter.ofPattern("EEE MMM d", Locale.ENGLISH);
    private static final DateTimeFormatter MMM_D = DateTimeFormatter.ofPattern("MMM d", Locale.ENGLISH);
    private static final DateTimeFormatter H_MM_A = DateTimeFormatter.ofPattern("h:mm a", Locale.ENGLISH);
    private static final DateTimeFormatter MONTH_SHORT = DateTimeFormatter.ofPattern("MMM", Locale.ENGLISH);
    private static final int RING_CIRCUMFERENCE = 264;

    private final ExamRepository examRepository;
    private final ExamTopicRepository examTopicRepository;
    private final CourseRepository courseRepository;
    private final StudySessionRepository studySessionRepository;
    private final Formatters formatters;

    public ExamsService(ExamRepository examRepository,
                        ExamTopicRepository examTopicRepository,
                        CourseRepository courseRepository,
                        StudySessionRepository studySessionRepository,
                        Formatters formatters) {
        this.examRepository = examRepository;
        this.examTopicRepository = examTopicRepository;
        this.courseRepository = courseRepository;
        this.studySessionRepository = studySessionRepository;
        this.formatters = formatters;
    }

    @Transactional(readOnly = true)
    public ExamsView buildFor(User user, String filter) {
        ZoneId tz = ZoneId.systemDefault();
        ZonedDateTime now = ZonedDateTime.now(tz);
        LocalDate today = now.toLocalDate();

        List<Exam> upcoming = examRepository.findByUserAndStatusOrderByStartsAtAsc(user, ExamStatus.UPCOMING);
        List<Exam> pastExams = examRepository.findByUserAndStatusOrderByStartsAtDesc(user, ExamStatus.PAST);

        long countUpcoming = upcoming.size();
        long countPast = pastExams.size();
        long countAll = countUpcoming + countPast;

        // Build all exam-card data first (for hero + cards)
        Map<Long, List<ExamTopic>> topicsByExam = new HashMap<>();
        for (Exam ex : upcoming) {
            topicsByExam.put(ex.getId(), examTopicRepository.findByExamOrderByDisplayOrderAsc(ex));
        }

        ExamsView.HeroExam hero = null;
        List<ExamsView.ExamCard> upcomingCards = new ArrayList<>();
        List<ExamsView.PastEntry> pastEntries = new ArrayList<>();

        boolean showUpcoming = "upcoming".equals(filter) || "all".equals(filter);
        boolean showPast = "past".equals(filter) || "all".equals(filter);

        if (showUpcoming && !upcoming.isEmpty()) {
            Exam heroExam = upcoming.get(0);
            hero = buildHero(heroExam, topicsByExam.get(heroExam.getId()), today, tz);
            for (int i = 1; i < upcoming.size(); i++) {
                Exam ex = upcoming.get(i);
                upcomingCards.add(buildCard(ex, topicsByExam.get(ex.getId()), today, tz));
            }
        }

        if (showPast) {
            for (Exam ex : pastExams) {
                pastEntries.add(buildPastEntry(ex, tz));
            }
        }

        ExamsView.Calendar calendar = buildCalendar(upcoming, today, tz);

        String aiSuggestion = composeSuggestion(hero, topicsByExam);

        List<ExamsView.TimeInvested> timeInvested = buildTimeInvested(user, now, tz);

        int weekNum = today.get(WeekFields.ISO.weekOfWeekBasedYear());
        String eyebrow = WEEKDAY_LONG.format(today) + " · Week " + weekNum;
        String subtitle = composeSubtitle(hero, countUpcoming);

        return new ExamsView(
                eyebrow,
                subtitle,
                countUpcoming,
                countPast,
                countAll,
                filter,
                hero,
                upcomingCards,
                pastEntries,
                calendar,
                aiSuggestion,
                timeInvested
        );
    }

    private ExamsView.HeroExam buildHero(Exam exam, List<ExamTopic> topics, LocalDate today, ZoneId tz) {
        long days = ChronoUnit.DAYS.between(today, exam.getStartsAt().atZone(tz).toLocalDate());
        int percent = readinessPercent(topics);
        int offset = RING_CIRCUMFERENCE - (RING_CIRCUMFERENCE * percent / 100);
        String[] split = splitTitle(exam.getTitle(), exam.getExamType().name().toLowerCase(Locale.ROOT));
        String dateLine = EEE_MMM_D.format(exam.getStartsAt().atZone(tz))
                + " · " + H_MM_A.format(exam.getStartsAt().atZone(tz))
                + " · " + exam.getLocation();
        return new ExamsView.HeroExam(
                exam,
                split[0],
                "— " + split[1],
                days,
                dateLine,
                split[1],
                formatters.pillClass(exam.getCourse()),
                percent,
                offset
        );
    }

    private ExamsView.ExamCard buildCard(Exam exam, List<ExamTopic> topics, LocalDate today, ZoneId tz) {
        long days = ChronoUnit.DAYS.between(today, exam.getStartsAt().atZone(tz).toLocalDate());
        int percent = readinessPercent(topics);
        int offset = RING_CIRCUMFERENCE - (RING_CIRCUMFERENCE * percent / 100);
        String[] split = splitTitle(exam.getTitle(), exam.getExamType().name().toLowerCase(Locale.ROOT));

        String dateShort = EEE_MMM_D.format(exam.getStartsAt().atZone(tz));
        boolean isTakehome = exam.getTitle().toLowerCase(Locale.ROOT).contains("take-home")
                || exam.getLocation().toLowerCase(Locale.ROOT).contains("take-home");

        List<String> meta = new ArrayList<>();
        meta.add(dateShort);
        if (!isTakehome) {
            meta.add(H_MM_A.format(exam.getStartsAt().atZone(tz)));
        }
        if (exam.getLocation() != null && !exam.getLocation().isBlank()) {
            meta.add(exam.getLocation());
        }
        String dur = formatDuration(exam.getDurationMinutes());
        if (dur != null) {
            meta.add(dur);
        }

        List<ExamsView.TopicChip> chips = new ArrayList<>();
        if (topics != null) {
            for (ExamTopic t : topics) {
                String klass = switch (t.getStatus()) {
                    case DONE    -> "done";
                    case WEAK    -> "weak";
                    case NEUTRAL -> "";
                };
                chips.add(new ExamsView.TopicChip(t.getName(), klass));
            }
        }

        return new ExamsView.ExamCard(
                exam,
                days,
                formatters.pillClass(exam.getCourse()),
                formatters.pillClass(exam.getCourse()),
                split[0],
                "— " + split[1],
                dateShort,
                meta,
                chips,
                percent,
                offset
        );
    }

    private ExamsView.PastEntry buildPastEntry(Exam exam, ZoneId tz) {
        String dateLabel = MMM_D.format(exam.getStartsAt().atZone(tz));
        String[] split = splitTitle(exam.getTitle(), exam.getExamType().name().toLowerCase(Locale.ROOT));
        String gradeClass = "";
        if (exam.getGrade() != null && !exam.getGrade().isBlank()
                && Character.toUpperCase(exam.getGrade().charAt(0)) == 'B') {
            gradeClass = "b";
        }
        return new ExamsView.PastEntry(
                exam,
                dateLabel,
                formatters.pillClass(exam.getCourse()),
                gradeClass,
                split[0],
                split[1]
        );
    }

    private ExamsView.Calendar buildCalendar(List<Exam> upcoming, LocalDate today, ZoneId tz) {
        Month month = today.getMonth();
        int year = today.getYear();
        LocalDate firstOfMonth = today.withDayOfMonth(1);
        // Sunday-first grid: SUN=0..SAT=6
        int leadingMuted = firstOfMonth.getDayOfWeek().getValue() % 7;
        int daysInMonth = firstOfMonth.lengthOfMonth();
        int totalNeeded = leadingMuted + daysInMonth;
        int totalCells = totalNeeded <= 35 ? 35 : 42;
        int trailingMuted = totalCells - totalNeeded;

        // Map of day-of-month → marker class for in-month exams
        Map<Integer, String> markers = new HashMap<>();
        int markerIndex = 0;
        List<Exam> sortedInMonth = new ArrayList<>();
        for (Exam ex : upcoming) {
            LocalDate d = ex.getStartsAt().atZone(tz).toLocalDate();
            if (d.getYear() == year && d.getMonth() == month) {
                sortedInMonth.add(ex);
            }
        }
        Collections.sort(sortedInMonth,
                (a, b) -> a.getStartsAt().compareTo(b.getStartsAt()));
        for (Exam ex : sortedInMonth) {
            int day = ex.getStartsAt().atZone(tz).getDayOfMonth();
            if (markers.containsKey(day)) {
                continue;
            }
            String klass = switch (markerIndex) {
                case 0 -> "exam";
                case 1 -> "exam2";
                default -> "exam3";
            };
            markers.put(day, klass);
            markerIndex++;
        }

        List<ExamsView.CalendarDay> days = new ArrayList<>(totalCells);
        // Leading muted days from previous month
        LocalDate prev = firstOfMonth.minusDays(leadingMuted);
        for (int i = 0; i < leadingMuted; i++) {
            days.add(new ExamsView.CalendarDay(prev.plusDays(i).getDayOfMonth(), false, false, ""));
        }
        // In-month days
        for (int d = 1; d <= daysInMonth; d++) {
            boolean isToday = d == today.getDayOfMonth();
            String marker = markers.getOrDefault(d, "");
            days.add(new ExamsView.CalendarDay(d, true, isToday, marker));
        }
        // Trailing muted days from next month
        for (int d = 1; d <= trailingMuted; d++) {
            days.add(new ExamsView.CalendarDay(d, false, false, ""));
        }

        return new ExamsView.Calendar(year, month, MONTH_SHORT.format(today), days);
    }

    private List<ExamsView.TimeInvested> buildTimeInvested(User user, ZonedDateTime now, ZoneId tz) {
        ZonedDateTime startOfWeek = now.toLocalDate().atStartOfDay(tz).with(DayOfWeek.MONDAY);
        if (startOfWeek.isAfter(now)) {
            startOfWeek = startOfWeek.minusWeeks(1);
        }
        ZonedDateTime endOfWeek = startOfWeek.plusWeeks(1);
        List<StudySession> sessions = studySessionRepository.findByUserAndStartedAtBetween(
                user, startOfWeek.toInstant(), endOfWeek.toInstant());

        Map<Long, Long> minutesByCourse = new LinkedHashMap<>();
        Map<Long, Course> courseById = new HashMap<>();
        for (StudySession s : sessions) {
            if (s.getCourse() == null) continue;
            long mins = s.getDurationSeconds() == null ? 0L : (s.getDurationSeconds() / 60);
            courseById.put(s.getCourse().getId(), s.getCourse());
            minutesByCourse.merge(s.getCourse().getId(), mins, Long::sum);
        }

        // Include all user courses with 0 if missing (so the section never goes empty when other sections do)
        for (Course c : courseRepository.findByUserOrderByDisplayOrderAsc(user)) {
            minutesByCourse.putIfAbsent(c.getId(), 0L);
            courseById.putIfAbsent(c.getId(), c);
        }

        List<Map.Entry<Long, Long>> sorted = new ArrayList<>(minutesByCourse.entrySet());
        sorted.sort((a, b) -> Long.compare(b.getValue(), a.getValue()));

        long max = sorted.isEmpty() ? 0L : sorted.get(0).getValue();
        List<ExamsView.TimeInvested> result = new ArrayList<>();
        for (int i = 0; i < Math.min(4, sorted.size()); i++) {
            Map.Entry<Long, Long> e = sorted.get(i);
            Course course = courseById.get(e.getKey());
            long minutes = e.getValue();
            int percent = max == 0 ? 0 : (int) Math.round(100.0 * minutes / max);
            String label = formatHours(minutes);
            result.add(new ExamsView.TimeInvested(
                    course, minutes, label, percent,
                    formatters.pebbleClass(course),
                    formatters.fillGradient(course)
            ));
        }
        return result;
    }

    private static int readinessPercent(List<ExamTopic> topics) {
        if (topics == null || topics.isEmpty()) {
            return 0;
        }
        double total = 0.0;
        for (ExamTopic t : topics) {
            total += switch (t.getStatus()) {
                case DONE    -> 1.0;
                case NEUTRAL -> 0.5;
                case WEAK    -> 0.0;
            };
        }
        int p = (int) Math.round(100.0 * total / topics.size());
        return Math.max(0, Math.min(100, p));
    }

    private static String[] splitTitle(String title, String fallbackType) {
        int sep = title.indexOf(" — ");
        if (sep > 0) {
            return new String[] { title.substring(0, sep), title.substring(sep + 3) };
        }
        return new String[] { title, fallbackType };
    }

    private static String formatDuration(Integer minutes) {
        if (minutes == null || minutes <= 0) {
            return null;
        }
        if (minutes >= 60 && minutes % 60 == 0) {
            int h = minutes / 60;
            return h == 1 ? "1 hour" : h + " hours";
        }
        return minutes + " min";
    }

    private static String formatHours(long minutes) {
        if (minutes <= 0) return "0m";
        if (minutes < 60) return minutes + "m";
        long h = minutes / 60;
        long m = minutes % 60;
        if (m == 0) return h + "h";
        return h + "h " + m + "m";
    }

    private static String composeSubtitle(ExamsView.HeroExam hero, long countUpcoming) {
        if (hero == null) {
            return "Nothing scheduled. Catch your breath.";
        }
        String code = hero.exam().getCourse().getCode();
        String first = capitalize(spell(countUpcoming));
        return first + " " + (countUpcoming == 1 ? "exam" : "exams")
                + " on the horizon. " + code + " is " + spell(hero.daysUntil())
                + " " + (hero.daysUntil() == 1 ? "day" : "days") + " out and you're "
                + hero.readinessPercent() + "% ready.";
    }

    private static String capitalize(String s) {
        if (s == null || s.isEmpty()) return s;
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    private static String composeSuggestion(ExamsView.HeroExam hero, Map<Long, List<ExamTopic>> topicsByExam) {
        if (hero == null) {
            return "No upcoming exams. Take a breath.";
        }
        List<ExamTopic> topics = topicsByExam.get(hero.exam().getId());
        Optional<ExamTopic> weak = Optional.ofNullable(topics)
                .orElse(List.of())
                .stream()
                .filter(t -> t.getStatus() == TopicStatus.WEAK)
                .findFirst();
        if (weak.isPresent()) {
            return "Spend tonight on " + hero.exam().getCourse().getCode() + " — "
                    + weak.get().getName() + ". It's your weakest area for the upcoming exam in "
                    + hero.daysUntil() + " days.";
        }
        return "You're on track for " + hero.exam().getTitle() + ". Keep the pace.";
    }

    private static String spell(long n) {
        return switch ((int) n) {
            case 0  -> "zero";
            case 1  -> "one";
            case 2  -> "two";
            case 3  -> "three";
            case 4  -> "four";
            case 5  -> "five";
            case 6  -> "six";
            case 7  -> "seven";
            case 8  -> "eight";
            case 9  -> "nine";
            case 10 -> "ten";
            case 11 -> "eleven";
            case 12 -> "twelve";
            default -> Long.toString(n);
        };
    }
}
