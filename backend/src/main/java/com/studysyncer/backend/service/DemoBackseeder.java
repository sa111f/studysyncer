package com.studysyncer.backend.service;

import com.studysyncer.backend.domain.User;
import com.studysyncer.backend.repository.CourseRepository;
import com.studysyncer.backend.repository.ExamRepository;
import com.studysyncer.backend.repository.TaskRepository;
import com.studysyncer.backend.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class DemoBackseeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DemoBackseeder.class);
    private static final long EXPECTED_TASK_COUNT = 9L;
    private static final long EXPECTED_EXAM_COUNT = 8L;

    private final UserRepository userRepository;
    private final CourseRepository courseRepository;
    private final TaskRepository taskRepository;
    private final ExamRepository examRepository;
    private final DemoSeeder demoSeeder;

    public DemoBackseeder(UserRepository userRepository,
                          CourseRepository courseRepository,
                          TaskRepository taskRepository,
                          ExamRepository examRepository,
                          DemoSeeder demoSeeder) {
        this.userRepository = userRepository;
        this.courseRepository = courseRepository;
        this.taskRepository = taskRepository;
        this.examRepository = examRepository;
        this.demoSeeder = demoSeeder;
    }

    @Override
    public void run(String... args) {
        for (User user : userRepository.findAll()) {
            boolean hasCourses = !courseRepository.findByUserOrderByDisplayOrderAsc(user).isEmpty();
            long taskCount = taskRepository.countByUser(user);
            long examCount = examRepository.countByUser(user);
            if (!hasCourses) {
                log.info("Seeding fresh demo data for user {}", user.getEmail());
                demoSeeder.seedIfEmpty(user);
            } else if (taskCount < EXPECTED_TASK_COUNT) {
                log.info("Reseeding demo data for user {} (tasks: {}, expected {})",
                        user.getEmail(), taskCount, EXPECTED_TASK_COUNT);
                demoSeeder.wipeAndReseed(user);
            } else if (examCount < EXPECTED_EXAM_COUNT) {
                log.info("Reseeding demo data for user {} (exams: {}, expected {})",
                        user.getEmail(), examCount, EXPECTED_EXAM_COUNT);
                demoSeeder.wipeAndReseed(user);
            }
        }
    }
}
