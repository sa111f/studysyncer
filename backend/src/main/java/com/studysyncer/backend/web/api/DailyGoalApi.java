package com.studysyncer.backend.web.api;

import com.studysyncer.backend.domain.DailyGoal;
import com.studysyncer.backend.domain.User;
import com.studysyncer.backend.repository.UserRepository;
import com.studysyncer.backend.service.DailyGoalService;
import com.studysyncer.backend.web.api.dto.DailyGoalRequest;
import com.studysyncer.backend.web.api.dto.DailyGoalResponse;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/daily-goal")
public class DailyGoalApi {

    private final DailyGoalService goalService;
    private final UserRepository userRepository;

    public DailyGoalApi(DailyGoalService goalService, UserRepository userRepository) {
        this.goalService = goalService;
        this.userRepository = userRepository;
    }

    @GetMapping
    public Map<String, Object> get(Authentication auth) {
        User user = userRepository.findByEmail(auth.getName()).orElseThrow();
        DailyGoal g = goalService.getOrCreate(user);
        return Map.of("ok", true, "goal", new DailyGoalResponse(g.getMinutesPerDay()));
    }

    @PutMapping
    public Map<String, Object> update(@Valid @ModelAttribute DailyGoalRequest req,
                                      Authentication auth) {
        User user = userRepository.findByEmail(auth.getName()).orElseThrow();
        DailyGoal g = goalService.update(user, req.getMinutesPerDay());
        return Map.of("ok", true, "goal", new DailyGoalResponse(g.getMinutesPerDay()));
    }
}
