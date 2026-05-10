package com.studysyncer.backend.web.api.dto;

import com.studysyncer.backend.domain.Tag;
import com.studysyncer.backend.domain.Task;
import com.studysyncer.backend.web.view.Formatters;

import java.util.List;

public record TaskResponse(
        Long id,
        String title,
        String italicSuffix,
        Long courseId,
        String courseCode,
        String pillClass,
        String status,
        String dueLabel,
        String dueClass,
        String estLabel,
        String priority,
        List<String> tags
) {
    public static TaskResponse from(Task t, Formatters fmt) {
        return new TaskResponse(
                t.getId(),
                t.getTitle(),
                t.getItalicSuffix(),
                t.getCourse() != null ? t.getCourse().getId() : null,
                t.getCourse() != null ? t.getCourse().getCode() : null,
                t.getCourse() != null ? fmt.pillClass(t.getCourse()) : null,
                t.getStatus().name(),
                t.getDueAt() != null ? fmt.fmtTaskDueLabel(t.getDueAt(), false, t.getCompletedAt()) : null,
                "",
                t.getEstimatedMinutes() != null ? fmt.fmtEstimate(t.getEstimatedMinutes()) : null,
                t.getPriority() != null ? t.getPriority().name() : null,
                t.getTags() == null ? List.of() : t.getTags().stream().map(Tag::getName).sorted().toList()
        );
    }
}
