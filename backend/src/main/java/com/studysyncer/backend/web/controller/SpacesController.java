package com.studysyncer.backend.web.controller;

import com.studysyncer.backend.domain.User;
import com.studysyncer.backend.repository.UserRepository;
import com.studysyncer.backend.service.SpacesService;
import com.studysyncer.backend.web.view.Formatters;
import com.studysyncer.backend.web.view.SpacesView;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.server.ResponseStatusException;

@Controller
public class SpacesController {

    private final SpacesService spacesService;
    private final UserRepository userRepository;
    private final Formatters formatters;

    public SpacesController(SpacesService spacesService,
                            UserRepository userRepository,
                            Formatters formatters) {
        this.spacesService = spacesService;
        this.userRepository = userRepository;
        this.formatters = formatters;
    }

    @GetMapping("/spaces/{slug}")
    public String space(@PathVariable String slug, Model model, Authentication auth) {
        User user = userRepository.findByEmail(auth.getName())
                .orElseThrow(() -> new IllegalStateException("Authenticated user not found"));
        SpacesView view = spacesService.buildFor(user, slug)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Course not found"));
        model.addAttribute("space", view);
        model.addAttribute("fmt", formatters);
        model.addAttribute("pageTitle", view.course().getCode() + " · StudySyncer");
        return "spaces";
    }
}
