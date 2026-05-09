package com.studysyncer.backend.repository;

import com.studysyncer.backend.domain.DailyGoal;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DailyGoalRepository extends JpaRepository<DailyGoal, Long> {
}
