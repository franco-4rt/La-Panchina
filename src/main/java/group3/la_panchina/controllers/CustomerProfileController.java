package group3.la_panchina.controllers;

import java.util.Optional;
import java.util.regex.Pattern;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import group3.la_panchina.models.User;
import group3.la_panchina.repositories.UserRepository;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;


@Controller 
@RequestMapping ("/profile")
public class CustomerProfileController {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    private static final Pattern PHONE_PATTERN = Pattern.compile("^(\\+?\\d{1,2}\\s?)?\\(?\\d{3}\\)?[\\s.-]?\\d{3}[\\s.-]?\\d{4}$");
    
    public CustomerProfileController(UserRepository userRepository){
        this.userRepository = userRepository;
    }

    @GetMapping
    public String showProfile(HttpSession session, HttpServletResponse response, Model model){
        User sessionUser = (User) session.getAttribute("session_user");
        if(sessionUser == null){
            return "redirect:/login";
        }

        response.setHeader("Cache-Control", "no-cahce, no-store, must-revalidate");
        response.setHeader("Pragma", "no-cache");
        response.setHeader("Expires", "0");

        Optional<User> freshUserOpt = userRepository.findById(sessionUser.getId());
        User user = freshUserOpt.orElse(sessionUser);

        model.addAttribute("user", user);
        return "CustomerProfile";
    }

    // Update personal information (Name, Phone, Preference (Phone or Email for notification))
    @PostMapping
    public String updateProfile(@RequestParam("name") String name,
                                @RequestParam("phone") String phone,
                                @RequestParam(value = "contactPreference", defaultValue = "Email") String contactPreference,
                                HttpSession session,
                                RedirectAttributes redirectAttributes) {
        User sessionUser = (User) session.getAttribute("session_user");
        if (sessionUser == null) {
            return "redirect:/login";
        }
        if (name == null || name.trim().isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Name cannot be empty.");
            return "redirect:/profile";
        }
        if (phone == null || !PHONE_PATTERN.matcher(phone.trim()).matches()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Please enter a valid 10-digit phone number (e.g. 555-123-4567).");
            return "redirect:/profile";
        }
        Optional<User> userOpt = userRepository.findById(sessionUser.getId());
        if (userOpt.isEmpty()) {
            return "redirect:/login";
        }
        User user = userOpt.get();
        user.setName(name.trim());
        user.setPhone(phone.trim());
        user.setContactPreference(contactPreference);
        User savedUser = userRepository.save(user);
        session.setAttribute("session_user", savedUser);
        redirectAttributes.addFlashAttribute("successMessage", "Profile details updated successfully!");
        return "redirect:/profile";
    }
    // Changing password
    @PostMapping("/password")
    public String changePassword(@RequestParam("currentPassword") String currentPassword,
                                 @RequestParam("newPassword") String newPassword,
                                 @RequestParam("confirmPassword") String confirmPassword,
                                 HttpSession session,
                                 RedirectAttributes redirectAttributes) {
        User sessionUser = (User) session.getAttribute("session_user");
        if (sessionUser == null) {
            return "redirect:/login";
        }
        Optional<User> userOpt = userRepository.findById(sessionUser.getId());
        if (userOpt.isEmpty()) {
            return "redirect:/login";
        }
        User user = userOpt.get();
        // Verify current password
        if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
            redirectAttributes.addFlashAttribute("passwordError", "Current password does not match.");
            return "redirect:/profile";
        }
        // Validate new password length
        if (newPassword == null || newPassword.length() < 6) {
            redirectAttributes.addFlashAttribute("passwordError", "New password must be at least 6 characters long.");
            return "redirect:/profile";
        }
        // Validate confirmation
        if (!newPassword.equals(confirmPassword)) {
            redirectAttributes.addFlashAttribute("passwordError", "New passwords do not match.");
            return "redirect:/profile";
        }
        // Persist new BCrypt hash
        user.setPassword(passwordEncoder.encode(newPassword));
        User savedUser = userRepository.save(user);
        session.setAttribute("session_user", savedUser);
        redirectAttributes.addFlashAttribute("passwordSuccess", "Password updated successfully!");
        return "redirect:/profile";
    }

    @GetMapping("/deactivate")
    public String getDeactivePage(HttpSession session, HttpServletResponse response, Model model) {
        User sessionUser = (User) session.getAttribute("session_user");
        if(sessionUser == null){
            return "redirect:/login";
        }

        response.setHeader("Cache-Control", "no-cahce, no-store, must-revalidate");
        response.setHeader("Pragma", "no-cache");
        response.setHeader("Expires", "0");

        Optional<User> freshUserOpt = userRepository.findById(sessionUser.getId());
        User user = freshUserOpt.orElse(sessionUser);

        model.addAttribute("user", user);
        return "deactivateAccount";
    }
    
}
