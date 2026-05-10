package com.studysyncer.backend.web.api.dto;

import com.studysyncer.backend.domain.ExamTopic;

public record TopicResponse(Long id, String name, String status) {
    public static TopicResponse from(ExamTopic t) {
        return new TopicResponse(t.getId(), t.getName(), t.getStatus().name());
    }
}
