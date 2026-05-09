package com.studysyncer.backend.web.view;

import com.studysyncer.backend.domain.ColorKey;
import com.studysyncer.backend.domain.ColorVariant;
import com.studysyncer.backend.domain.Course;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Locale;

@Component
public class Formatters {

    private static final ZoneId TZ = ZoneId.systemDefault();
    private static final DateTimeFormatter HOUR_AM_PM = DateTimeFormatter.ofPattern("h a", Locale.ENGLISH);
    private static final DateTimeFormatter HOUR_MIN_AM_PM = DateTimeFormatter.ofPattern("h:mm a", Locale.ENGLISH);
    private static final DateTimeFormatter WEEKDAY = DateTimeFormatter.ofPattern("EEE", Locale.ENGLISH);
    private static final DateTimeFormatter MONTH_DAY = DateTimeFormatter.ofPattern("MMM d", Locale.ENGLISH);

    public String fmtClock(Long seconds) {
        if (seconds == null) {
            return "0:00";
        }
        long s = Math.max(0, seconds);
        long h = s / 3600;
        long m = (s % 3600) / 60;
        long sec = s % 60;
        if (h > 0) {
            return String.format("%d:%02d:%02d", h, m, sec);
        }
        return String.format("%d:%02d", m, sec);
    }

    public String fmtOverdue(Instant dueAt) {
        if (dueAt == null) {
            return "";
        }
        long days = ChronoUnit.DAYS.between(dueAt.atZone(TZ).toLocalDate(), LocalDate.now(TZ));
        if (days <= 0) {
            return "late";
        }
        return days + "d late";
    }

    public String fmtToday(Instant dueAt) {
        if (dueAt == null) {
            return "";
        }
        ZonedDateTime z = dueAt.atZone(TZ);
        if (z.getHour() == 23 && z.getMinute() >= 59) {
            return "EOD";
        }
        return HOUR_AM_PM.format(z);
    }

    public String fmtWeekday(Instant dueAt) {
        if (dueAt == null) {
            return "";
        }
        return WEEKDAY.format(dueAt.atZone(TZ));
    }

    public String fmtMonthDay(Instant when) {
        if (when == null) {
            return "";
        }
        return MONTH_DAY.format(when.atZone(TZ));
    }

    public long daysUntil(Instant when) {
        if (when == null) {
            return 0L;
        }
        return ChronoUnit.DAYS.between(LocalDate.now(TZ), when.atZone(TZ).toLocalDate());
    }

    public boolean isSoon(Instant dueAt) {
        if (dueAt == null) {
            return false;
        }
        Instant now = Instant.now();
        return dueAt.isAfter(now) && Duration.between(now, dueAt).toHours() <= 2;
    }

    public int dailyGoalDashOffset(int percent) {
        int p = Math.max(0, Math.min(100, percent));
        return 264 - (264 * p / 100);
    }

    public int hoursPart(int totalMinutes) {
        return Math.max(0, totalMinutes) / 60;
    }

    public int minutesPart(int totalMinutes) {
        return Math.max(0, totalMinutes) % 60;
    }

    public String fmtEstimate(Integer minutes) {
        if (minutes == null || minutes <= 0) {
            return "";
        }
        if (minutes < 60) {
            return minutes + "m";
        }
        int h = minutes / 60;
        int m = minutes % 60;
        if (m == 0) {
            return h + "h";
        }
        return h + "h " + m + "m";
    }

    public String fmtTaskDueLabel(Instant dueAt, boolean overdue, Instant completedAt) {
        if (completedAt != null) {
            return HOUR_MIN_AM_PM.format(completedAt.atZone(TZ));
        }
        if (dueAt == null) {
            return "";
        }
        if (overdue) {
            return fmtOverdue(dueAt);
        }
        LocalDate today = LocalDate.now(TZ);
        LocalDate due = dueAt.atZone(TZ).toLocalDate();
        if (due.isEqual(today)) {
            return fmtToday(dueAt);
        }
        long days = ChronoUnit.DAYS.between(today, due);
        if (days >= 0 && days <= 6) {
            return fmtWeekday(dueAt);
        }
        return fmtMonthDay(dueAt);
    }

    public String pillClass(Course course) {
        if (course == null || course.getColorKey() == null) {
            return "";
        }
        ColorKey key = course.getColorKey();
        ColorVariant variant = course.getColorVariant();
        if (key == ColorKey.PHYSICS && variant == ColorVariant.DEEP) {
            return "physics2";
        }
        return switch (key) {
            case PHYSICS -> "physics";
            case CS      -> "cs";
            case ENG     -> "eng";
            case PHIL    -> "phil";
            default      -> "";
        };
    }

    public String pebbleClass(Course course) {
        if (course == null || course.getColorKey() == null) {
            return "";
        }
        ColorKey key = course.getColorKey();
        ColorVariant variant = course.getColorVariant();
        if (key == ColorKey.PHYSICS && variant == ColorVariant.DEEP) {
            return "pebble-phys2";
        }
        return switch (key) {
            case PHYSICS -> "pebble-phys";
            case CS      -> "pebble-cs";
            case ENG     -> "pebble-eng";
            case PHIL    -> "pebble-phil";
            default      -> "";
        };
    }

    public String fillGradient(Course course) {
        if (course == null || course.getColorKey() == null) {
            return "linear-gradient(90deg, var(--ink-faint), var(--ink-3))";
        }
        ColorKey key = course.getColorKey();
        ColorVariant variant = course.getColorVariant();
        if (key == ColorKey.PHYSICS && variant == ColorVariant.DEEP) {
            return "linear-gradient(90deg, #c46442, #e89070)";
        }
        return switch (key) {
            case PHYSICS -> "linear-gradient(90deg, var(--phys-a), var(--phys-b))";
            case CS      -> "linear-gradient(90deg, var(--cs-a), var(--cs-b))";
            case ENG     -> "linear-gradient(90deg, var(--eng-a), var(--eng-b))";
            case PHIL    -> "linear-gradient(90deg, var(--phil-a), var(--phil-b))";
            default      -> "linear-gradient(90deg, var(--ink-faint), var(--ink-3))";
        };
    }
}
