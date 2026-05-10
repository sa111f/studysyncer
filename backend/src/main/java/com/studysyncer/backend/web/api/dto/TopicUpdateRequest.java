package com.studysyncer.backend.web.api.dto;

import com.studysyncer.backend.domain.TopicStatus;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class TopicUpdateRequest {

    /** New status, or null to keep existing. */
    private TopicStatus status;

    /** New name, or null to keep existing. */
    @Size(max = 100)
    private String name;
}
