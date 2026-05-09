package com.studysyncer.backend.web.view;

import com.studysyncer.backend.domain.Course;
import com.studysyncer.backend.domain.Exam;
import com.studysyncer.backend.domain.ScheduleItem;
import com.studysyncer.backend.domain.Task;

import java.util.List;

public record SpacesView(
        Course course,
        String courseInitial,
        String courseClass,
        String pillClass,
        String nameHead,
        String nameEm,
        String scheduleSummary,
        Stats stats,
        NextExam nextExam,
        List<OpenTask> openTasks,
        long openTasksTotal,
        TimeInvested timeInvested,
        List<UpcomingScheduleEntry> schedule
) {

    public static record Stats(
            long openTasksOpen,
            long openTasksTotal,
            long openTasksDueToday,
            long openTasksOverdue,
            int weekMinutes,
            String weekHoursLabel,
            int weekMinutesDelta,
            String weekDeltaLabel,
            Long nextExamDays
    ) {}

    public static record NextExam(
            Exam exam,
            long daysUntil,
            String dateLine,
            String typeItalic,
            String titleHead,
            String titleEm
    ) {}

    public static record OpenTask(
            Task task,
            String estLabel,
            String dueLabel,
            String dueClass
    ) {}

    public static record TimeInvested(
            int weekMinutes,
            String hoursLabel,
            List<MiniBar> bars
    ) {}

    public static record MiniBar(
            int minutes,
            double heightPercent,
            boolean filled,
            boolean isToday,
            String dayLetter
    ) {}

    public static record UpcomingScheduleEntry(
            ScheduleItem item,
            String dayLabel,
            String timeLabel,
            boolean isNext,
            String typeBadge,
            String location,
            Integer durationMinutes
    ) {}
}
