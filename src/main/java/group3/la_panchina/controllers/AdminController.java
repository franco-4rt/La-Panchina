package group3.la_panchina.controllers;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import group3.la_panchina.models.User;
import group3.la_panchina.repositories.UserRepository;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

@Controller
@RequestMapping("/admin")
public class AdminController {
    private final UserRepository userRepository;

    public AdminController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @GetMapping
    public String showAdminDashboard(HttpSession session, HttpServletResponse response, Model model) {
        User user = (User) session.getAttribute("session_user");

        if (user == null || !"ADMIN".equalsIgnoreCase(user.getRole())) {
            return "redirect:/login";
        }

        // Prevent caching of pages (from professor)
        response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
        response.setHeader("Pragma", "no-cache");
        response.setHeader("Expires", "0");

        List<User> allUsers = userRepository.findAll();
        model.addAttribute("users", allUsers);
        model.addAttribute("adminUser", user);

        return "admin";
    }

    @PostMapping("/users/{id}/toggle-suspend")
    public String toggleUserSuspension(@PathVariable Long id,
            HttpSession session,
            RedirectAttributes redirectAttributes) {
        User sessionUser = (User) session.getAttribute("session_user");
        if (sessionUser == null || !"ADMIN".equalsIgnoreCase(sessionUser.getRole())) {
            return "redirect:/login";
        }

        Optional<User> targetOpt = userRepository.findById(id);
        if (targetOpt.isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage", "User not found.");
            return "redirect:/admin";
        }

        User target = targetOpt.get();
        if ("ADMIN".equalsIgnoreCase(target.getRole())) {
            redirectAttributes.addFlashAttribute("errorMessage", "Administrator accounts cannot be suspended.");
            return "redirect:/admin";
        }

        boolean nextState = !target.isSuspended();
        target.setSuspended(nextState);
        userRepository.save(target);

        String statusLabel = nextState ? "suspended" : "reactivated";
        redirectAttributes.addFlashAttribute("successMessage",
                "Account for " + target.getName() + " has been " + statusLabel + ".");
        return "redirect:/admin";
    }
}
