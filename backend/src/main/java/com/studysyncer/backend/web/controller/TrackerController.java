package com.studysyncer.backend.web.controller;

import com.studysyncer.backend.domain.User;
import com.studysyncer.backend.repository.UserRepository;
import com.studysyncer.backend.service.TrackerService;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@Controller
public class TrackerController {

    private final TrackerService trackerService;
    private final UserRepository userRepository;

    public TrackerController(TrackerService trackerService, UserRepository userRepository) {
        this.trackerService = trackerService;
        this.userRepository = userRepository;
    }

    @GetMapping("/tracker")
    public String tracker(@RequestParam(defaultValue = "week") String range,
                          Model model, Authentication auth) {
        User user = userRepository.findByEmail(auth.getName())
                .orElseThrow(() -> new IllegalStateException("Authenticated user not found"));
        String safe = "week"; // only week supported in this prompt
        model.addAttribute("tracker", trackerService.buildFor(user, safe));
        model.addAttribute("pageTitle", "Tracker · StudySyncer");
        model.addAttribute("extraJs", List.of("/js/today.js", "/js/tracker.js"));
        return "tracker";
    }
}
