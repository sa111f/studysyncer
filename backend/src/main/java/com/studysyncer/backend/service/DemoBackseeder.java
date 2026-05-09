package com.studysyncer.backend.service;

import com.studysyncer.backend.domain.User;
import com.studysyncer.backend.repository.CourseRepository;
import com.studysyncer.backend.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class DemoBackseeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DemoBackseeder.class);

    private final UserRepository userRepository;
    private final CourseRepository courseRepository;
    private final DemoSeeder demoSeeder;

    public DemoBackseeder(UserRepository userRepository,
                          CourseRepository courseRepository,
                          DemoSeeder demoSeeder) {
        this.userRepository = userRepository;
        this.courseRepository = courseRepository;
        this.demoSeeder = demoSeeder;
    }

    @Override
    public void run(String... args) {
        for (User user : userRepository.findAll()) {
            if (courseRepository.findByUserOrderByDisplayOrderAsc(user).isEmpty()) {
                log.info("Back-seeding demo data for user {}", user.getEmail());
                demoSeeder.seedIfEmpty(user);
            }
        }
    }
}
