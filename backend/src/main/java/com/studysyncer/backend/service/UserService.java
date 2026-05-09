package com.studysyncer.backend.service;

import com.studysyncer.backend.domain.DailyGoal;
import com.studysyncer.backend.domain.User;
import com.studysyncer.backend.repository.DailyGoalRepository;
import com.studysyncer.backend.repository.UserRepository;
import com.studysyncer.backend.web.dto.SignupForm;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final DailyGoalRepository dailyGoalRepository;
    private final PasswordEncoder passwordEncoder;
    private final DemoSeeder demoSeeder;

    public UserService(UserRepository userRepository,
                       DailyGoalRepository dailyGoalRepository,
                       PasswordEncoder passwordEncoder,
                       DemoSeeder demoSeeder) {
        this.userRepository = userRepository;
        this.dailyGoalRepository = dailyGoalRepository;
        this.passwordEncoder = passwordEncoder;
        this.demoSeeder = demoSeeder;
    }

    @Transactional
    public User register(SignupForm form) {
        String email = form.getEmail().trim().toLowerCase();
        if (userRepository.findByEmail(email).isPresent()) {
            throw new DuplicateEmailException(email);
        }

        Instant now = Instant.now();

        User user = new User();
        user.setFirstName(form.getFirstName().trim());
        user.setLastName(form.getLastName().trim());
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(form.getPassword()));
        user.setAvatarInitial(initialOf(form.getFirstName()));
        user.setCreatedAt(now);
        User saved = userRepository.save(user);

        DailyGoal goal = new DailyGoal();
        goal.setUser(saved);
        goal.setMinutesPerDay(150);
        goal.setCreatedAt(now);
        goal.setUpdatedAt(now);
        dailyGoalRepository.save(goal);

        demoSeeder.seedIfEmpty(saved);

        return saved;
    }

    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email.trim().toLowerCase());
    }

    private static String initialOf(String firstName) {
        String trimmed = firstName.trim();
        if (trimmed.isEmpty()) {
            return "?";
        }
        return trimmed.substring(0, 1).toUpperCase();
    }
}
