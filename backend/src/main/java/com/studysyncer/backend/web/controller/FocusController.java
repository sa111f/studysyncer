package com.studysyncer.backend.web.controller;

import com.studysyncer.backend.domain.Course;
import com.studysyncer.backend.domain.Pomodoro;
import com.studysyncer.backend.domain.StudySession;
import com.studysyncer.backend.domain.Task;
import com.studysyncer.backend.domain.TaskStatus;
import com.studysyncer.backend.domain.User;
import com.studysyncer.backend.repository.CourseRepository;
import com.studysyncer.backend.repository.TaskRepository;
import com.studysyncer.backend.repository.UserRepository;
import com.studysyncer.backend.service.FocusService;
import com.studysyncer.backend.web.view.FocusView;
import com.studysyncer.backend.web.view.Formatters;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Controller
public class FocusController {

    private final FocusService focusService;
    private final TaskRepository taskRepo;
    private final CourseRepository courseRepo;
    private final UserRepository userRepo;
    private final Formatters formatters;

    public FocusController(FocusService focusService,
                           TaskRepository taskRepo,
                           CourseRepository courseRepo,
                           UserRepository userRepo,
                           Formatters formatters) {
        this.focusService = focusService;
        this.taskRepo = taskRepo;
        this.courseRepo = courseRepo;
        this.userRepo = userRepo;
        this.formatters = formatters;
    }

    @GetMapping("/focus")
    @Transactional(readOnly = true)
    public String focus(@RequestParam(required = false) Long taskId,
                        @RequestParam(required = false) Long courseId,
                        Model model, Authentication auth) {
        User user = userRepo.findByEmail(auth.getName())
                .orElseThrow(() -> new IllegalStateException("Authenticated user not found"));

        Optional<StudySession> active = focusService.findActiveBreakSession(user)
                .or(() -> focusService.findActiveWorkSession(user));

        FocusView view;
        if (active.isPresent()) {
            view = buildRunningView(active.get(), user);
        } else {
            view = buildSetupView(user, taskId, courseId);
        }

        model.addAttribute("focusView", view);
        model.addAttribute("pageTitle", "Focus · StudySyncer");
        model.addAttribute("extraCss", List.of("/css/focus.css"));
        return "focus";
    }

    @PostMapping("/focus/start")
    public String start(@RequestParam Long courseId,
                        @RequestParam(required = false) Long taskId,
                        @RequestParam String title,
                        @RequestParam(required = false) String italicSuffix,
                        Authentication auth) {
        User user = userRepo.findByEmail(auth.getName()).orElseThrow();
        focusService.startWorkSession(user, courseId, taskId, title, italicSuffix);
        return "redirect:/focus";
    }

    @PostMapping(value = "/focus/heartbeat", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public Map<String, Object> heartbeat(@RequestParam Long sessionId, Authentication auth) {
        User user = userRepo.findByEmail(auth.getName()).orElseThrow();
        boolean ok = focusService.heartbeat(user, sessionId);
        return Map.of("ok", ok, "serverTime", Instant.now().toString());
    }

    @PostMapping(value = "/focus/pomodoro/complete", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public Map<String, Object> completePomo(@RequestParam Long sessionId, Authentication auth) {
        User user = userRepo.findByEmail(auth.getName()).orElseThrow();
        Pomodoro p = focusService.completePomodoro(user, sessionId);
        return Map.of("ok", true, "pomodoroId", p.getId());
    }

    @PostMapping("/focus/break/start")
    public String startBreak(Authentication auth) {
        User user = userRepo.findByEmail(auth.getName()).orElseThrow();
        focusService.startBreak(user);
        return "redirect:/focus";
    }

    @PostMapping("/focus/end")
    public String end(@RequestParam(required = false, defaultValue = "/") String next,
                      Authentication auth) {
        User user = userRepo.findByEmail(auth.getName()).orElseThrow();
        focusService.endActive(user);
        return "redirect:" + ("/focus".equals(next) ? "/focus" : "/");
    }

    @PostMapping("/focus/skip")
    public String skip(@RequestParam Long sessionId, Authentication auth) {
        User user = userRepo.findByEmail(auth.getName()).orElseThrow();
        focusService.skipAndEnd(user, sessionId);
        return "redirect:/";
    }

    private FocusView.SetupView buildSetupView(User user, Long taskIdParam, Long courseIdParam) {
        List<Course> courses = courseRepo.findByUserOrderByDisplayOrderAsc(user);
        List<Task> tasks = taskRepo.findByUserAndStatusOrderByDueAtAsc(user, TaskStatus.PENDING);

        Task defaultTask = null;
        if (taskIdParam != null) {
            defaultTask = tasks.stream().filter(t -> t.getId().equals(taskIdParam)).findFirst().orElse(null);
        }
        if (defaultTask == null && !tasks.isEmpty()) defaultTask = tasks.get(0);

        Course defaultCourse = null;
        if (defaultTask != null) {
            defaultCourse = defaultTask.getCourse();
        } else if (courseIdParam != null) {
            defaultCourse = courses.stream().filter(c -> c.getId().equals(courseIdParam)).findFirst().orElse(null);
        }
        if (defaultCourse == null && !courses.isEmpty()) defaultCourse = courses.get(0);

        String title = defaultTask != null ? defaultTask.getTitle() : "Focus session";
        String italic = defaultTask != null ? defaultTask.getItalicSuffix() : null;
        String pillClass = defaultCourse != null ? formatters.pillClass(defaultCourse) : "cs";
        String courseCode = defaultCourse != null ? defaultCourse.getCode() : "";

        return new FocusView.SetupView(
                courses, tasks,
                defaultCourse != null ? defaultCourse.getId() : null,
                defaultTask != null ? defaultTask.getId() : null,
                title, italic, pillClass, courseCode
        );
    }

    private FocusView.RunningView buildRunningView(StudySession s, User user) {
        int pomos = focusService.pomodorosInSession(s);
        int planned = s.getSessionType().name().equals("BREAK")
                ? FocusService.BREAK_SECONDS
                : FocusService.POMODORO_SECONDS;

        Task next = taskRepo.findByUserAndStatusOrderByDueAtAsc(user, TaskStatus.PENDING).stream()
                .filter(t -> s.getTask() == null || !t.getId().equals(s.getTask().getId()))
                .findFirst()
                .orElse(null);

        return new FocusView.RunningView(
                s.getId(),
                s.getSessionType().name(),
                s.getTitle(),
                s.getItalicSuffix(),
                s.getCourse().getCode(),
                formatters.pillClass(s.getCourse()),
                s.getStartedAt().toEpochMilli(),
                Instant.now().toEpochMilli(),
                planned,
                pomos,
                FocusService.CYCLE_LENGTH,
                next != null ? next.getTitle() : null,
                next != null ? next.getItalicSuffix() : null,
                next != null ? formatters.pillClass(next.getCourse()) : null,
                next != null ? next.getCourse().getCode() : null
        );
    }
}
