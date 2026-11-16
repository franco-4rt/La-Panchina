package group3.la_panchina.controllers;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;


@Controller 
public class AboutController {
    
    @GetMapping("/about-us")
    public String showAboutUs() {
        return "about-us";
    }
    
}
