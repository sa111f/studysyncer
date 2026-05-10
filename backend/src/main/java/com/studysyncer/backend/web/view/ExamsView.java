package com.studysyncer.backend.web.view;

import com.studysyncer.backend.domain.Course;
import com.studysyncer.backend.domain.Exam;

import java.time.Month;
import java.util.List;

public record ExamsView(
        String pageEyebrow,
        String pageSubtitle,
        long countUpcoming,
        long countPast,
        long countAll,
        String activeFilter,
        HeroExam hero,
        List<ExamCard> upcomingCards,
        List<PastEntry> past,
        Calendar calendar,
        String aiSuggestion,
        List<TimeInvested> timeInvested
) {
    public record HeroExam(
            Exam exam,
            String heroTitle,
            String heroTypeItalic,
            long daysUntil,
            String dateLine,
            String typeLabel,
            String pillClass,
            int readinessPercent,
            int dashOffset
    ) {}

    public record ExamCard(
            Exam exam,
            long daysUntil,
            String pillClass,
            String courseClass,
            String title,
            String typeItalic,
            String dateLine,
            List<String> metaParts,
            List<TopicChip> topics,
            int readinessPercent,
            int dashOffset
    ) {}

    public record TopicChip(Long id, String name, String statusClass, String statusName) {}

    public record PastEntry(
            Exam exam,
            String dateLabel,
            String pillClass,
            String gradeClass,
            String titleHead,
            String titleEm
    ) {}

    public record Calendar(
            int year,
            Month month,
            String monthLabelShort,
            List<CalendarDay> days
    ) {}

    public record CalendarDay(
            int dayNum,
            boolean inMonth,
            boolean isToday,
            String examMarkerClass
    ) {}

    public record TimeInvested(
            Course course,
            long minutes,
            String hoursLabel,
            int percent,
            String pebbleClass,
            String fillGradient
    ) {}
}
