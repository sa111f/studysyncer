package com.studysyncer.backend.web.controller;

import com.studysyncer.backend.service.DuplicateEmailException;
import com.studysyncer.backend.service.UserService;
import com.studysyncer.backend.web.dto.LoginForm;
import com.studysyncer.backend.web.dto.SignupForm;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@Controller
public class AuthController {

    private final UserService userService;

    public AuthController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/login")
    public String loginPage(@ModelAttribute("loginForm") LoginForm loginForm,
                            @RequestParam(value = "error", required = false) String error,
                            @RequestParam(value = "logout", required = false) String logout,
                            @RequestParam(value = "registered", required = false) String registered,
                            Model model) {
        if (error != null) {
            model.addAttribute("errorMessage", "Invalid email or password.");
        }
        if (logout != null) {
            model.addAttribute("infoMessage", "You have been logged out.");
        }
        if (registered != null) {
            model.addAttribute("infoMessage", "Account created — please log in.");
        }
        model.addAttribute("pageTitle", "Log in · StudySyncer");
        model.addAttribute("extraCss", List.of("/css/auth.css"));
        return "auth/login";
    }

    @GetMapping("/signup")
    public String signupPage(@ModelAttribute("signupForm") SignupForm signupForm, Model model) {
        model.addAttribute("pageTitle", "Create your account · StudySyncer");
        model.addAttribute("extraCss", List.of("/css/auth.css"));
        return "auth/signup";
    }

    @PostMapping("/signup")
    public String signupSubmit(@Valid @ModelAttribute("signupForm") SignupForm signupForm,
                               BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            return "auth/signup";
        }
        try {
            userService.register(signupForm);
        } catch (DuplicateEmailException ex) {
            bindingResult.rejectValue("email", "duplicate", "An account with that email already exists.");
            return "auth/signup";
        }
        return "redirect:/login?registered";
    }
}
