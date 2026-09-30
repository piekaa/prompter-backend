package com.prompter.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class PageController {

    @GetMapping("/")
    public String index() {
        return "forward:/index.html";
    }

    /** Backoffice shell — requires authentication. */
    @GetMapping({"/backoffice", "/backoffice/"})
    public String backoffice() {
        return "forward:/backoffice.html";
    }
}
