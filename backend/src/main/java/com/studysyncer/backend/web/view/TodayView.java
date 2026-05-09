package com.studysyncer.backend.web.view;

import com.studysyncer.backend.domain.Exam;
import com.studysyncer.backend.domain.StudySession;
import com.studysyncer.backend.domain.Task;

import java.util.List;

public record TodayView(
        String greetingTimeOfDay,
        String greetingFirstName,
        String greetingDateLine,
        String greetingEyebrow,
        String greetingSubtitle,
        StudySession activeSession,
        Long activeSessionElapsedSeconds,
        List<Task> overdueTasks,
        List<Task> todayTasks,
        List<Task> weekTasks,
        int dailyGoalMinutes,
        int minutesStudiedToday,
        int dailyGoalPercent,
        int dailyGoalRemainingMinutes,
        int currentStreakDays,
        List<Exam> upcomingExams,
        int weekTasksCompleted,
        int weekTasksTotal,
        int weekMinutesStudied
) {
}
