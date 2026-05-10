package com.studysyncer.backend.web.api;

import com.studysyncer.backend.domain.Task;
import com.studysyncer.backend.domain.User;
import com.studysyncer.backend.repository.UserRepository;
import com.studysyncer.backend.service.TasksService;
import com.studysyncer.backend.web.api.dto.TaskCreateRequest;
import com.studysyncer.backend.web.api.dto.TaskResponse;
import com.studysyncer.backend.web.view.Formatters;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/tasks")
public class TaskApi {

    private final TasksService tasksService;
    private final UserRepository userRepository;
    private final Formatters formatters;

    public TaskApi(TasksService tasksService,
                   UserRepository userRepository,
                   Formatters formatters) {
        this.tasksService = tasksService;
        this.userRepository = userRepository;
        this.formatters = formatters;
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> create(@Valid @ModelAttribute TaskCreateRequest req,
                                                      Authentication auth) {
        User user = userRepository.findByEmail(auth.getName()).orElseThrow();
        Task created = tasksService.createTask(user, req);
        return ResponseEntity.status(201).body(Map.of(
                "ok", true,
                "task", TaskResponse.from(created, formatters)));
    }

    @PutMapping("/{id}")
    public Map<String, Object> update(@PathVariable Long id,
                                      @Valid @ModelAttribute TaskCreateRequest req,
                                      Authentication auth) {
        User user = userRepository.findByEmail(auth.getName()).orElseThrow();
        Task updated = tasksService.updateTask(user, id, req);
        return Map.of(
                "ok", true,
                "task", TaskResponse.from(updated, formatters));
    }

    @DeleteMapping("/{id}")
    public Map<String, Object> delete(@PathVariable Long id, Authentication auth) {
        User user = userRepository.findByEmail(auth.getName()).orElseThrow();
        tasksService.deleteTask(user, id);
        return Map.of("ok", true);
    }
}
