package group3.la_panchina.controllers;

import group3.la_panchina.models.PasswordResetToken;
import group3.la_panchina.models.User;
import group3.la_panchina.repositories.PasswordResetTokenRepository;
import group3.la_panchina.repositories.UserRepository;
import jakarta.servlet.http.HttpServletRequest;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.util.Optional;
import java.util.UUID;

@Controller
public class PasswordResetTokenController {

    private final UserRepository userRepository;
    private final PasswordResetTokenRepository tokenRepository;
    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public PasswordResetTokenController(UserRepository userRepository,
            PasswordResetTokenRepository tokenRepository) {
        this.userRepository = userRepository;
        this.tokenRepository = tokenRepository;
    }

    @GetMapping("/forgot-password")
    public String showForgotPasswordForm() {
        return "forgotPassword";
    }

    @PostMapping("/forgot-password")
    public String handleForgotPassword(@RequestParam("email") String email,
            HttpServletRequest request,
            RedirectAttributes redirectAttributes) {
        Optional<User> userOpt = userRepository.findByEmailIgnoreCase(email != null ? email.trim() : "");

        if (userOpt.isPresent()) {
            User user = userOpt.get();

            // Clear any previous token for this user
            tokenRepository.deleteByUser(user);

            // Generate a secure 30-minute token
            String token = UUID.randomUUID().toString();
            PasswordResetToken resetToken = new PasswordResetToken(token, user, 30);
            tokenRepository.save(resetToken);

            // Construct the reset URL
            String resetUrl = ServletUriComponentsBuilder.fromRequestUri(request)
                    .replacePath("/reset-password")
                    .replaceQueryParam("token", token)
                    .toUriString();

            // Log to server console for testing & TA grading convenience
            System.out.println("=================================================");
            System.out.println("[PASSWORD RESET LINK GENERATED]");
            System.out.println("User: " + user.getEmail());
            System.out.println("Reset URL: " + resetUrl);
            System.out.println("=================================================");

            // Pass the reset link in flash attributes for easy local demonstration
            redirectAttributes.addFlashAttribute("demoResetLink", resetUrl);
        }

        // display success regardless of whether email exists
        redirectAttributes.addFlashAttribute("successMessage",
                "If an account with that email exists, password reset instructions have been generated.");
        return "redirect:/forgot-password";
    }

    // Validate token and show password reset page
    @GetMapping("/reset-password")
    public String showResetPasswordForm(@RequestParam(value = "token", required = false) String token,
            Model model,
            RedirectAttributes redirectAttributes) {
        if (token == null || token.trim().isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Invalid password reset token.");
            return "redirect:/login";
        }

        Optional<PasswordResetToken> tokenOpt = tokenRepository.findByToken(token);
        if (tokenOpt.isEmpty() || tokenOpt.get().isExpired()) {
            redirectAttributes.addFlashAttribute("errorMessage",
                    "Your reset link has expired or is invalid. Please request a new one.");
            return "redirect:/forgot-password";
        }

        model.addAttribute("token", token);
        return "resetPassword";
    }

    // Update password and invalidate token
    @PostMapping("/reset-password")
    @Transactional
    public String handleResetPassword(@RequestParam("token") String token,
            @RequestParam("newPassword") String newPassword,
            @RequestParam("confirmPassword") String confirmPassword,
            Model model,
            RedirectAttributes redirectAttributes) {
        Optional<PasswordResetToken> tokenOpt = tokenRepository.findByToken(token);
        if (tokenOpt.isEmpty() || tokenOpt.get().isExpired()) {
            redirectAttributes.addFlashAttribute("errorMessage",
                    "Your reset link has expired or is invalid. Please request a new one.");
            return "redirect:/forgot-password";
        }

        if (newPassword == null || newPassword.length() < 6) {
            model.addAttribute("token", token);
            model.addAttribute("errorMessage", "Password must be at least 6 characters long.");
            return "resetPassword";
        }

        if (!newPassword.equals(confirmPassword)) {
            model.addAttribute("token", token);
            model.addAttribute("errorMessage", "Passwords do not match.");
            return "resetPassword";
        }

        // Update password with BCrypt hash
        PasswordResetToken resetToken = tokenOpt.get();
        User user = resetToken.getUser();
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        // Delete token so it cannot be used a second time
        tokenRepository.delete(resetToken);

        redirectAttributes.addFlashAttribute("successMessage",
                "Your password has been reset successfully! Please log in with your new password.");
        return "redirect:/login";
    }
}