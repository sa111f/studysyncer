package com.studysyncer.backend.web.view;

import com.studysyncer.backend.domain.Course;

import java.util.List;

public record TrackerView(
        String pageEyebrow,
        String pageSubtitle,
        String activeRange,
        String weekRangeLabel,
        Summary summary,
        Chart chart,
        List<RecentSession> recentSessions,
        Streak streak,
        List<CourseBreakdown> byCourse,
        Goal goal,
        String bestTimeInsight
) {
    public record Summary(
            int totalMinutes,
            String totalHoursLabel,
            String deltaLabel,
            int dailyAvgMinutes,
            String dailyAvgLabel,
            String dailyAvgTarget,
            int sessionsCount,
            String sessionsAvgLabel,
            int pomodorosCount,
            String pomodorosCompletionLabel,
            int streakDays,
            String streakBestLabel
    ) {}

    public record Chart(
            int yAxisMaxMinutes,
            List<String> yAxisLabels,
            List<DayColumn> days
    ) {}

    public record DayColumn(
            String shortDay,
            String dateNum,
            boolean isToday,
            int totalMinutes,
            String totalLabel,
            String tooltipLabel,
            List<BarSeg> segments
    ) {}

    public record BarSeg(
            String courseClass,
            int minutes,
            double heightPercent
    ) {}

    public record RecentSession(
            String whenWeekday,
            String whenTime,
            String title,
            String italicSuffix,
            String pillClass,
            String courseCode,
            int pomoCount,
            String durationLabel
    ) {}

    public record Streak(
            int days,
            String currentLabel,
            String bestLabel,
            List<StreakDot> dots
    ) {}

    public record StreakDot(String stateClass, String label) {}

    public record CourseBreakdown(
            Course course,
            int weekMinutes,
            String hoursLabel,
            String pebbleClass,
            String fillGradient,
            int percent
    ) {}

    public record Goal(
            int minutesStudiedToday,
            int dailyGoalMinutes,
            int percent,
            int dashOffset,
            int remainingMinutes,
            String hoursLabel,
            String stateLine
    ) {}
}
