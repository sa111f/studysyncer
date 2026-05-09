package com.studysyncer.backend.web.advice;

import com.studysyncer.backend.web.view.Formatters;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
public class FormattersAdvice {

    private final Formatters formatters;

    public FormattersAdvice(Formatters formatters) {
        this.formatters = formatters;
    }

    @ModelAttribute("fmt")
    public Formatters fmt() {
        return formatters;
    }
}
