package com.studysyncer.backend.web.api.dto;

import com.studysyncer.backend.domain.Exam;
import com.studysyncer.backend.domain.ExamTopic;
import com.studysyncer.backend.web.view.Formatters;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

public record ExamResponse(
        Long id,
        String title,
        Long courseId,
        String courseCode,
        String pillClass,
        String examType,
        String status,
        String startsAtLocal,
        Integer durationMinutes,
        String location,
        String grade,
        int readinessPercent,
        List<TopicResponse> topics
) {
    public static ExamResponse from(Exam ex, List<ExamTopic> topics, Formatters fmt) {
        int score = topics.isEmpty() ? 0
                : (int) Math.round(100.0 * topics.stream()
                .mapToDouble(t -> switch (t.getStatus()) {
                    case DONE -> 1.0;
                    case NEUTRAL -> 0.5;
                    case WEAK -> 0.0;
                }).sum() / topics.size());
        return new ExamResponse(
                ex.getId(),
                ex.getTitle(),
                ex.getCourse() != null ? ex.getCourse().getId() : null,
                ex.getCourse() != null ? ex.getCourse().getCode() : null,
                ex.getCourse() != null ? fmt.pillClass(ex.getCourse()) : null,
                ex.getExamType().name(),
                ex.getStatus().name(),
                ex.getStartsAt() != null
                        ? LocalDateTime.ofInstant(ex.getStartsAt(), ZoneId.systemDefault()).toString()
                        : null,
                ex.getDurationMinutes(),
                ex.getLocation(),
                ex.getGrade(),
                score,
                topics.stream().map(TopicResponse::from).toList()
        );
    }
}
