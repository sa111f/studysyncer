package com.studysyncer.backend.web.api.dto;

import com.studysyncer.backend.domain.ScheduleItem;

public record ScheduleItemResponse(
        Long id,
        String title,
        String location,
        Short dayOfWeek,
        Integer startMinute,
        String startTimeLocal,
        Integer durationMinutes,
        String itemType
) {
    public static ScheduleItemResponse from(ScheduleItem s) {
        String startLabel = String.format("%02d:%02d", s.getStartMinute() / 60, s.getStartMinute() % 60);
        return new ScheduleItemResponse(
                s.getId(),
                s.getTitle(),
                s.getLocation(),
                s.getDayOfWeek(),
                s.getStartMinute(),
                startLabel,
                s.getDurationMinutes(),
                s.getItemType().name()
        );
    }
}
