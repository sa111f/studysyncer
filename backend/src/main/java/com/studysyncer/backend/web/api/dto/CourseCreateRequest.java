package com.studysyncer.backend.web.api.dto;

import com.studysyncer.backend.domain.ColorKey;
import com.studysyncer.backend.domain.ColorVariant;
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
public class CourseCreateRequest {

    @NotBlank(message = "Course code is required")
    @Size(max = 32)
    private String code;

    @NotBlank(message = "Course name is required")
    @Size(max = 200)
    private String name;

    @NotNull(message = "Pick a color")
    private ColorKey colorKey = ColorKey.OTHER;

    private ColorVariant colorVariant = ColorVariant.DEFAULT;

    @Size(max = 64)
    private String section;

    @Size(max = 64)
    private String term;

    @Size(max = 200)
    private String professor;

    @Size(max = 200)
    private String room;

    @Min(0)
    @Max(20)
    private Integer credits;

    @Size(max = 2000)
    private String description;
}
