package com.studysyncer.backend.web.api.dto;

import com.studysyncer.backend.domain.SessionType;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class SessionLogRequest {

    @NotNull(message = "Pick a course")
    private Long courseId;

    /** Optional task this session was for. */
    private Long taskId;

    @NotBlank(message = "Title is required")
    @Size(max = 200)
    private String title;

    @Size(max = 200)
    private String italicSuffix;

    /** ISO local-datetime string from {@code <input type="datetime-local">} */
    @NotBlank(message = "Start time required")
    private String startedAtLocal;

    @NotNull(message = "Duration required")
    @Min(value = 1, message = "At least 1 minute")
    @Max(value = 720, message = "Cap at 12 hours")
    private Integer durationMinutes;

    /** WORK | BREAK — break sessions are unusual to manually log but allowed. */
    private SessionType sessionType = SessionType.WORK;

    /** How many of the session's minutes counted as completed pomodoros. Defaults to durationMinutes / 25 if null. */
    @Min(0)
    @Max(48)
    private Integer completedPomodoros;
}
