package com.studysyncer.backend.service;

import com.studysyncer.backend.domain.User;
import com.studysyncer.backend.repository.CourseRepository;
import com.studysyncer.backend.repository.ExamRepository;
import com.studysyncer.backend.repository.StudySessionRepository;
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
    // Tracker page expects per-day session coverage. The richest weekday seed produces ~18,
    // the leanest (Sunday) ~2. 12 catches old prompt-3 6-session seeds on most weekdays.
    private static final long EXPECTED_SESSION_COUNT = 12L;

    private final UserRepository userRepository;
    private final CourseRepository courseRepository;
    private final TaskRepository taskRepository;
    private final ExamRepository examRepository;
    private final StudySessionRepository studySessionRepository;
    private final DemoSeeder demoSeeder;

    public DemoBackseeder(UserRepository userRepository,
                          CourseRepository courseRepository,
                          TaskRepository taskRepository,
                          ExamRepository examRepository,
                          StudySessionRepository studySessionRepository,
                          DemoSeeder demoSeeder) {
        this.userRepository = userRepository;
        this.courseRepository = courseRepository;
        this.taskRepository = taskRepository;
        this.examRepository = examRepository;
        this.studySessionRepository = studySessionRepository;
        this.demoSeeder = demoSeeder;
    }

    @Override
    public void run(String... args) {
        for (User user : userRepository.findAll()) {
            boolean hasCourses = !courseRepository.findByUserOrderByDisplayOrderAsc(user).isEmpty();
            long taskCount = taskRepository.countByUser(user);
            long examCount = examRepository.countByUser(user);
            long sessionCount = studySessionRepository.countByUser(user);
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
            } else if (sessionCount < EXPECTED_SESSION_COUNT) {
                log.info("Reseeding demo data for user {} (sessions: {}, expected {})",
                        user.getEmail(), sessionCount, EXPECTED_SESSION_COUNT);
                demoSeeder.wipeAndReseed(user);
            }
        }
    }
}
