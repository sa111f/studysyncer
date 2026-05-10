package com.studysyncer.backend.web.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class TagCreateRequest {

    @NotBlank(message = "Tag name required")
    @Size(max = 40, message = "Keep it under 40 characters")
    private String name;
}
