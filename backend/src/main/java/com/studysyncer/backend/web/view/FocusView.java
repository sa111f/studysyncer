package com.studysyncer.backend.web.view;

import com.studysyncer.backend.domain.Course;
import com.studysyncer.backend.domain.Task;

import java.util.List;

public sealed interface FocusView permits FocusView.SetupView, FocusView.RunningView {

    String pillClass();

    record SetupView(
            List<Course> courses,
            List<Task> selectableTasks,
            Long defaultCourseId,
            Long defaultTaskId,
            String defaultTitle,
            String defaultItalicSuffix,
            String pillClass,
            String courseCode
    ) implements FocusView {}

    record RunningView(
            Long sessionId,
            String sessionType,
            String title,
            String italicSuffix,
            String courseCode,
            String pillClass,
            long startedAtEpochMs,
            long serverNowEpochMs,
            int plannedDurationSeconds,
            int pomodorosCompleted,
            int cycleLength,
            String upNextTitle,
            String upNextItalic,
            String upNextPillClass,
            String upNextCourseCode
    ) implements FocusView {}
}
