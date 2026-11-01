package group3.la_panchina.controllers;


import group3.la_panchina.models.User;
import group3.la_panchina.repositories.UserRepository;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.util.List;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.GetMapping;


@Controller 
@RequestMapping ("/admin")
public class AdminController {
    private final UserRepository userRepository;

    public AdminController(UserRepository userRepository){
        this.userRepository = userRepository;
    }

    @GetMapping
    public String showAdminDashboard(HttpSession session, HttpServletResponse response, Model model) {
        User user = (User) session.getAttribute("session_user");
        
        if(user == null || !"ADMIN".equalsIgnoreCase(user.getRole())){
            return "redirect:/login";
        }

        // Prevent caching of pages (from professor)
        response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate" );
        response.setHeader("Pragma", "no-cache");
        response.setHeader("Expires", "0");

        List<User> allUsers = userRepository.findAll();
        model.addAttribute("users", allUsers);
        model.addAttribute("adminUser", user);
        
        return "admin";
    }
    
}
