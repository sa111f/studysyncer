package com.studysyncer.backend.service;

import com.studysyncer.backend.domain.DailyGoal;
import com.studysyncer.backend.domain.User;
import com.studysyncer.backend.repository.DailyGoalRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
public class DailyGoalService {

    private final DailyGoalRepository goalRepository;

    public DailyGoalService(DailyGoalRepository goalRepository) {
        this.goalRepository = goalRepository;
    }

    @Transactional
    public DailyGoal getOrCreate(User user) {
        return goalRepository.findById(user.getId()).orElseGet(() -> {
            DailyGoal g = new DailyGoal();
            g.setUser(user);
            g.setMinutesPerDay(150);
            g.setCreatedAt(Instant.now());
            g.setUpdatedAt(Instant.now());
            return goalRepository.save(g);
        });
    }

    @Transactional
    public DailyGoal update(User user, int minutesPerDay) {
        if (minutesPerDay < 5 || minutesPerDay > 600) {
            throw new IllegalArgumentException("Goal must be 5–600 minutes");
        }
        DailyGoal g = getOrCreate(user);
        g.setMinutesPerDay(minutesPerDay);
        g.setUpdatedAt(Instant.now());
        return goalRepository.save(g);
    }
}
