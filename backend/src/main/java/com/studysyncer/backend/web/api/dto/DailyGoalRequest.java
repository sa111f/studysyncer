package com.studysyncer.backend.web.api.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class DailyGoalRequest {

    @NotNull(message = "Set a target")
    @Min(value = 5, message = "At least 5 minutes")
    @Max(value = 600, message = "Cap at 10 hours")
    private Integer minutesPerDay;
}
