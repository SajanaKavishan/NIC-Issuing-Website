package com.project.nic.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Simple route alias to serve the static confirmation page via 
 * a clean URL without the .html suffix.
 *
 * This maps GET /confirmation -> forward:/confirmation.html
 */
@Controller
public class ConfirmationController {

    @GetMapping("/confirmation")
    public String confirmation() {
        // Forward to the static resource under src/main/resources/static
        return "forward:/confirmation.html";
    }
}
