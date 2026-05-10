package com.studysyncer.backend.web.api.dto;

import com.studysyncer.backend.domain.ScheduleItemType;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class ScheduleItemRequest {

    /** null for new, existing id for keep/update. */
    private Long id;

    private String title;

    private String location;

    /** ISO 1=Mon..7=Sun */
    private Short dayOfWeek;

    /** "HH:mm" from {@code <input type="time">} */
    private String startTimeLocal;

    private Integer durationMinutes;

    private ScheduleItemType itemType = ScheduleItemType.LECTURE;
}
