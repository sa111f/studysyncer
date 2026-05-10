package com.studysyncer.backend.web.api.dto;

import com.studysyncer.backend.domain.Priority;
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
public class TaskCreateRequest {

    @NotBlank(message = "Title is required")
    @Size(max = 200, message = "Keep it under 200 characters")
    private String title;

    @Size(max = 200)
    private String italicSuffix;

    @NotNull(message = "Pick a course")
    private Long courseId;

    /** ISO local-datetime string from the form's <input type="datetime-local">, e.g. 2026-05-09T18:30 */
    private String dueAtLocal;

    @Min(1)
    @Max(600)
    private Integer estimatedMinutes;

    private Priority priority = Priority.NORMAL;

    /** Comma-separated tag names from the chip input. Empty allowed. */
    private String tags;
}
