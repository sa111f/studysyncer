package com.studysyncer.backend.web.view;

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
}
