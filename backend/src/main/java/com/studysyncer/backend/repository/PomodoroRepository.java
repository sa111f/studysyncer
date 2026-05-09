package com.studysyncer.backend.repository;

import com.studysyncer.backend.domain.Pomodoro;
import com.studysyncer.backend.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PomodoroRepository extends JpaRepository<Pomodoro, Long> {

    long countBySession_User(User user);

    long countBySession_UserAndCompletedTrue(User user);
}
