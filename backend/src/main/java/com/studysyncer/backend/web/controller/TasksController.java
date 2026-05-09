package com.studysyncer.backend.web.controller;

import com.studysyncer.backend.domain.User;
import com.studysyncer.backend.repository.UserRepository;
import com.studysyncer.backend.service.TasksService;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Set;

@Controller
public class TasksController {

    private static final Set<String> VALID_FILTERS = Set.of("all", "active", "completed", "overdue");

    private final TasksService tasksService;
    private final UserRepository userRepository;

    public TasksController(TasksService tasksService, UserRepository userRepository) {
        this.tasksService = tasksService;
        this.userRepository = userRepository;
    }

    @GetMapping("/tasks")
    public String tasks(@RequestParam(defaultValue = "all") String filter,
                        Model model, Authentication auth) {
        User user = userRepository.findByEmail(auth.getName())
                .orElseThrow(() -> new IllegalStateException("Authenticated user not found"));
        String safe = VALID_FILTERS.contains(filter) ? filter : "all";
        model.addAttribute("tasks", tasksService.buildFor(user, safe));
        model.addAttribute("pageTitle", "Tasks · StudySyncer");
        return "tasks";
    }

    @PostMapping("/tasks/{id}/toggle")
    public String toggle(@PathVariable Long id,
                         @RequestParam(required = false, defaultValue = "all") String filter,
                         Authentication auth) {
        User user = userRepository.findByEmail(auth.getName())
                .orElseThrow(() -> new IllegalStateException("Authenticated user not found"));
        tasksService.toggleTaskCompletion(user, id);
        String safe = VALID_FILTERS.contains(filter) ? filter : "all";
        return "redirect:/tasks?filter=" + safe;
    }
}
