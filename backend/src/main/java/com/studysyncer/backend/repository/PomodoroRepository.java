package com.studysyncer.backend.repository;

import com.studysyncer.backend.domain.Pomodoro;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PomodoroRepository extends JpaRepository<Pomodoro, Long> {
}
