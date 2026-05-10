package com.studysyncer.backend.web.controller;

import com.studysyncer.backend.domain.User;
import com.studysyncer.backend.repository.UserRepository;
import com.studysyncer.backend.service.ExamsService;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.Set;

@Controller
public class ExamsController {

    private static final Set<String> VALID_FILTERS = Set.of("upcoming", "past", "all");

    private final ExamsService examsService;
    private final UserRepository userRepository;

    public ExamsController(ExamsService examsService, UserRepository userRepository) {
        this.examsService = examsService;
        this.userRepository = userRepository;
    }

    @GetMapping("/exams")
    public String exams(@RequestParam(defaultValue = "upcoming") String filter,
                        Model model, Authentication auth) {
        User user = userRepository.findByEmail(auth.getName())
                .orElseThrow(() -> new IllegalStateException("Authenticated user not found"));
        String safe = VALID_FILTERS.contains(filter) ? filter : "upcoming";
        model.addAttribute("exams", examsService.buildFor(user, safe));
        model.addAttribute("pageTitle", "Exams · StudySyncer");
        model.addAttribute("extraCss", List.of("/css/modal.css"));
        model.addAttribute("extraJs", List.of("/js/modal.js", "/js/exams.js"));
        return "exams";
    }
}
