package group3.la_panchina.controllers;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;

import group3.la_panchina.models.User;
import group3.la_panchina.repositories.UserRepository;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;



@Controller 
public class AuthController {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder()
    
    public AuthController(UserRepository userRepository2){
        this.userRepository = userRepository2;
    }

    @GetMapping("/signup")
    public String getSignupForm(Model model, HttpSession session) {
        if(session.getAttribute("session_user") !=null){
            return "redirect:/";
        }
        if(!model.containsAttribute("user")){
            model.addAttribute("user", new User());
        }
        return "signup";
    }

    @PostMapping("/signup")
    public String processSignup(@Valid @ModelAttribute ("user") User user, 
                                BindingResult result, 
                                HttpSession session, 
                                RedirectAttributes redirectAttributes){
        if(userRepository.existsByEmailIgnoreCase(user.getEmail()))
            result.rejectValue("email", "duplicate.email", "An account with this email already exists.");
        
        if(result.hasErrors()){
            return "signup";
        }

        user.setPassword(passwordEncoder.encode(user.getPassword()));
        user.setRole("CUSTOMER");
        User savedUser = userRepository.save(user);

        session.setAttribute("session_user", savedUser);
        redirectAttributes.addFlashAttribute("successMessage", "Welcome to La Panchina, " + savedUser.getName() + "!");

        return "redirect:/";
    }
    
    
}
