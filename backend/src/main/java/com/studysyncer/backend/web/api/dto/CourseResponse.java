package com.studysyncer.backend.web.api.dto;

import com.studysyncer.backend.domain.Course;
import com.studysyncer.backend.domain.ScheduleItem;
import com.studysyncer.backend.web.view.Formatters;

import java.util.List;

public record CourseResponse(
        Long id,
        String code,
        String slug,
        String name,
        String colorKey,
        String colorVariant,
        String pillClass,
        String section,
        String term,
        String professor,
        String room,
        Integer credits,
        String description,
        List<ScheduleItemResponse> schedule
) {
    public static CourseResponse from(Course c, List<ScheduleItem> items, Formatters fmt) {
        return new CourseResponse(
                c.getId(),
                c.getCode(),
                c.getSlug(),
                c.getName(),
                c.getColorKey() != null ? c.getColorKey().name() : null,
                c.getColorVariant() != null ? c.getColorVariant().name() : null,
                fmt.pillClass(c),
                c.getSection(),
                c.getTerm(),
                c.getProfessor(),
                c.getRoom(),
                c.getCredits(),
                c.getDescription(),
                items.stream().map(ScheduleItemResponse::from).toList()
        );
    }
}
