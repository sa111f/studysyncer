package com.studysyncer.backend.web.api.dto;

import com.studysyncer.backend.domain.StudySession;
import com.studysyncer.backend.web.view.Formatters;

import java.time.LocalDateTime;
import java.time.ZoneId;

public record SessionResponse(
        Long id,
        Long courseId,
        String courseCode,
        String pillClass,
        Long taskId,
        String title,
        String italicSuffix,
        String startedAtLocal,
        String endedAtLocal,
        Integer durationMinutes,
        String sessionType,
        int pomodorosTotal,
        int pomodorosCompleted
) {
    public static SessionResponse from(StudySession s, int pomosTotal, int pomosCompleted, Formatters fmt) {
        ZoneId tz = ZoneId.systemDefault();
        return new SessionResponse(
                s.getId(),
                s.getCourse() != null ? s.getCourse().getId() : null,
                s.getCourse() != null ? s.getCourse().getCode() : null,
                s.getCourse() != null ? fmt.pillClass(s.getCourse()) : null,
                s.getTask() != null ? s.getTask().getId() : null,
                s.getTitle(),
                s.getItalicSuffix(),
                s.getStartedAt() != null ? LocalDateTime.ofInstant(s.getStartedAt(), tz).toString() : null,
                s.getEndedAt() != null ? LocalDateTime.ofInstant(s.getEndedAt(), tz).toString() : null,
                s.getDurationSeconds() != null ? (s.getDurationSeconds() / 60) : null,
                s.getSessionType().name(),
                pomosTotal,
                pomosCompleted
        );
    }
}
