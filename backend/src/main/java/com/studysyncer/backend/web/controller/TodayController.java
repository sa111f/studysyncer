package com.studysyncer.backend.web.controller;

import com.studysyncer.backend.domain.User;
import com.studysyncer.backend.repository.UserRepository;
import com.studysyncer.backend.service.TodayService;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class TodayController {

    private final TodayService todayService;
    private final UserRepository userRepository;

    public TodayController(TodayService todayService, UserRepository userRepository) {
        this.todayService = todayService;
        this.userRepository = userRepository;
    }

    @GetMapping("/")
    public String today(Model model, Authentication auth) {
        User user = userRepository.findByEmail(auth.getName())
                .orElseThrow(() -> new IllegalStateException("Authenticated user not found"));
        model.addAttribute("today", todayService.buildFor(user));
        model.addAttribute("pageTitle", "Today · StudySyncer");
        return "today";
    }
}
