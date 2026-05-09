package com.studysyncer.backend.web.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class TodayController {

    @GetMapping("/")
    public String today() {
        return "today";
    }
}
