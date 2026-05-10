package com.studysyncer.backend.web.api.dto;

import com.studysyncer.backend.domain.ExamStatus;
import com.studysyncer.backend.domain.ExamType;
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
public class ExamCreateRequest {

    @NotBlank(message = "Title is required")
    @Size(max = 200, message = "Keep it under 200 characters")
    private String title;

    @NotNull(message = "Pick a course")
    private Long courseId;

    @NotNull(message = "Pick exam type")
    private ExamType examType = ExamType.MIDTERM;

    /** ISO local-datetime string from {@code <input type="datetime-local">} */
    @NotBlank(message = "Set a date and time")
    private String startsAtLocal;

    @Min(0)
    @Max(600)
    private Integer durationMinutes;

    @Size(max = 200)
    private String location;

    /** UPCOMING | PAST */
    private ExamStatus status = ExamStatus.UPCOMING;

    /** Optional grade label, e.g. "B+" or "A-". Only meaningful for PAST. */
    @Size(max = 16)
    private String grade;

    /** Comma-separated topic names. Empty allowed. New topics start with NEUTRAL status. */
    private String topics;
}
