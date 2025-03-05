package org.ilghar.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class ReactController {

    @GetMapping("/{path:[^\\.]*}")
    public String serveReactApp() {
        return "forward:/index.html";
    }
}
