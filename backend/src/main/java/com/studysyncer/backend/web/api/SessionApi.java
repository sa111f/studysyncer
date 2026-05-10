package com.studysyncer.backend.web.api;

import com.studysyncer.backend.domain.Pomodoro;
import com.studysyncer.backend.domain.StudySession;
import com.studysyncer.backend.domain.User;
import com.studysyncer.backend.repository.PomodoroRepository;
import com.studysyncer.backend.repository.UserRepository;
import com.studysyncer.backend.service.SessionsService;
import com.studysyncer.backend.web.api.dto.SessionLogRequest;
import com.studysyncer.backend.web.api.dto.SessionResponse;
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

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/sessions")
public class SessionApi {

    private final SessionsService sessionsService;
    private final PomodoroRepository pomodoroRepository;
    private final UserRepository userRepository;
    private final Formatters formatters;

    public SessionApi(SessionsService sessionsService,
                      PomodoroRepository pomodoroRepository,
                      UserRepository userRepository,
                      Formatters formatters) {
        this.sessionsService = sessionsService;
        this.pomodoroRepository = pomodoroRepository;
        this.userRepository = userRepository;
        this.formatters = formatters;
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> log(@Valid @ModelAttribute SessionLogRequest req,
                                                   Authentication auth) {
        User user = userRepository.findByEmail(auth.getName()).orElseThrow();
        StudySession s = sessionsService.logManual(user, req);
        List<Pomodoro> pomos = pomodoroRepository.findBySession(s);
        int total = pomos.size();
        int completed = (int) pomos.stream().filter(p -> Boolean.TRUE.equals(p.getCompleted())).count();
        return ResponseEntity.status(201).body(Map.of(
                "ok", true,
                "session", SessionResponse.from(s, total, completed, formatters)));
    }

    @PutMapping("/{id}")
    public Map<String, Object> update(@PathVariable Long id,
                                      @Valid @ModelAttribute SessionLogRequest req,
                                      Authentication auth) {
        User user = userRepository.findByEmail(auth.getName()).orElseThrow();
        StudySession s = sessionsService.update(user, id, req);
        List<Pomodoro> pomos = pomodoroRepository.findBySession(s);
        int total = pomos.size();
        int completed = (int) pomos.stream().filter(p -> Boolean.TRUE.equals(p.getCompleted())).count();
        return Map.of(
                "ok", true,
                "session", SessionResponse.from(s, total, completed, formatters));
    }

    @DeleteMapping("/{id}")
    public Map<String, Object> delete(@PathVariable Long id, Authentication auth) {
        User user = userRepository.findByEmail(auth.getName()).orElseThrow();
        sessionsService.delete(user, id);
        return Map.of("ok", true);
    }
}
