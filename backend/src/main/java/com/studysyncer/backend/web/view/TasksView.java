package com.studysyncer.backend.web.view;

import com.studysyncer.backend.domain.Course;
import com.studysyncer.backend.domain.Task;

import java.util.List;

public record TasksView(
        String pageEyebrow,
        String pageSubtitle,
        long countAll,
        long countActive,
        long countCompleted,
        long countOverdue,
        String activeFilter,
        List<TaskGroup> groups,
        int weekDoneCount,
        int weekTotalCount,
        int weekRemainingHoursEstimate,
        long weekOverdue,
        long weekActive,
        long weekDone,
        List<CourseProgress> byCourse,
        String aiSuggestion
) {
    public record TaskGroup(String heading, String headingItalic, String cssExtra, List<Task> tasks) {}

    public record CourseProgress(Course course, long done, long total, int percent,
                                 String pebbleClass, String fillGradient) {}
}
