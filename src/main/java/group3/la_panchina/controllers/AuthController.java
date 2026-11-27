package group3.la_panchina.controllers;

import java.util.Optional;
import java.time.Duration;
import java.time.LocalDateTime;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;

import group3.la_panchina.models.User;
import group3.la_panchina.repositories.UserRepository;
import group3.la_panchina.services.PasswordValidator;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.bind.annotation.PostMapping;

@Controller
public class AuthController {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public AuthController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @GetMapping("/signup")
    public String getSignupForm(Model model, HttpSession session) {
        if (session.getAttribute("session_user") != null) {
            return "redirect:/";
        }
        if (!model.containsAttribute("user")) {
            model.addAttribute("user", new User());
        }
        return "signup";
    }

    @GetMapping("/logout")
    public String destroySession(HttpServletRequest request, HttpServletResponse response,
            RedirectAttributes redirectAttributes) {
        // Prevent caching of pages (from professor)
        response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
        response.setHeader("Pragma", "no-cache");
        response.setHeader("Expires", "0");

        // Destroy the session (from professor)
        request.getSession().invalidate();

        redirectAttributes.addFlashAttribute("successMessage", "You have been logged out successfully.");
        return "redirect:/";
    }

    @GetMapping("/login")
    public String getLogin(HttpSession session) {
        if (session.getAttribute("session_user") != null) {
            return "redirect:/";
        }
        return "login";
    }

    @PostMapping("/signup")
    public String processSignup(@Valid @ModelAttribute("user") User user,
            BindingResult result,
            HttpSession session,
            RedirectAttributes redirectAttributes) {
        if (userRepository.existsByEmailIgnoreCase(user.getEmail())) {
            result.rejectValue("email",
                    "duplicate.email",
                    "An account with this email already exists.");
        }

        if (!PasswordValidator.isValid(user.getPassword())) {
            result.rejectValue("password", "invalid.password", PasswordValidator.getErrorMsg());
        }

        if (result.hasErrors()) {
            return "signup";
        }

        user.setPassword(passwordEncoder.encode(user.getPassword()));
        user.setRole("CUSTOMER");
        user.recordLogin();
        User savedUser = userRepository.save(user);

        session.setAttribute("session_user", savedUser);
        redirectAttributes.addFlashAttribute("successMessage", "Welcome to La Panchina, " + savedUser.getName() + "!");

        return "redirect:/";
    }

    @PostMapping("/login")
    public String login(@RequestParam String email,
            @RequestParam String password,
            HttpServletRequest request,
            HttpServletResponse response,
            RedirectAttributes redirectAttributes,
            Model model) {

        // Find user by email in database
        Optional<User> optionalUser = userRepository.findByEmailIgnoreCase(email);
        if (optionalUser.isEmpty()) {
            model.addAttribute("errorMessage", "Account doesn't exist");
            model.addAttribute("email", email);
            return "login";
        }

        User user = optionalUser.get();

        // Block login if account is currently locked (Story 1.11)
        if (user.accountLocked()) {
            long minutesLeft = Duration.between(LocalDateTime.now(), user.getLockUntil()).toMinutes() + 1;
            model.addAttribute("errorMessage",
                    "Account is temporarily locked due to too many failed login attempts. Try again in " + minutesLeft
                            + " minute(s).");
            model.addAttribute("email", email);
            return "login";
        }

        // Check if BCrypt matches the hashed password
        if (!passwordEncoder.matches(password, user.getPassword())) {
            user.registerFailedLogin();
            userRepository.save(user);

            if (user.accountLocked()) {
                model.addAttribute("errorMessage",
                        "Too many failed login attempts. Your account is locked for 15 minutes.");
            } else {
                int remaining = User.MAX_FAILED_ATTEMPTS - user.getFailedLoginAttempts();
                model.addAttribute("errorMessage",
                        "Invalid password. " + remaining + " attempt(s) remaining before temporary lockout.");
            }
            model.addAttribute("email", email);
            return "login";
        }

        // Successful login: reset failed attempts & record login timestamp
        user.resetFailedLogins();
        user.recordLogin();
        user = userRepository.save(user);
        // Prevent caching of authenticated pages (from professor's code)
        response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
        response.setHeader("Pragma", "no-cache");
        response.setHeader("Expires", "0");
        // Save to session using professor's key name
        request.getSession().setAttribute("session_user", user);

        redirectAttributes.addFlashAttribute("successMessage", "Welcome back, " + user.getName() + "!");
        if ("ADMIN".equalsIgnoreCase(user.getRole())) {
            return "redirect:/admin";
        } else {
            return "redirect:/";
        }
    }

}
