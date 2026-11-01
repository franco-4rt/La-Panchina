package group3.la_panchina.controllers;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

import group3.la_panchina.models.User;
import group3.la_panchina.repositories.MenuItemRepository;
import jakarta.servlet.http.HttpSession;

@Controller 
@RequestMapping ("/admin/menu")
public class AdminMenuController {
    
    private final MenuItemRepository repo;

    public AdminMenuController(MenuItemRepository repo){
        this.repo = repo;
    }

    private boolean isAdmin(HttpSession session){
        return session.getAttribute("session_user") instanceof User u && "ADMIN".equals(u.getRole());
    }

    private String deny(HttpSession session){
        return session.getAttribute("session_user") == null ? "redirect:/login" : "redirect:/";
    }

    
    
}
