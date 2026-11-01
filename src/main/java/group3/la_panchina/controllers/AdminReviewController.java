package group3.la_panchina.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.server.ResponseStatusException;

import group3.la_panchina.models.Review;
import group3.la_panchina.models.User;
import group3.la_panchina.repositories.ReviewRepository;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;


@Controller
public class AdminReviewController {

    @Autowired
    private ReviewRepository reviewRepo;

    @GetMapping("/admin/reviews")
    public String showReviewsAdmin(HttpSession session, HttpServletResponse response,
    Model model) {

        User user = (User) session.getAttribute("session_user");

        //if not admin redirect to login
        if(user == null || !"ADMIN".equalsIgnoreCase(user.getRole())){
            return "redirect:/login";
        }

        // Prevent caching of pages (from professor)
        response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate" );
        response.setHeader("Pragma", "no-cache");
        response.setHeader("Expires", "0");

        model.addAttribute("reviews", reviewRepo.findAll());

        return "admin/review/reviews";
    }
    
    @GetMapping("/admin/delete/{rid}")
    public String showDeleteConfirmation(@PathVariable Long rid, HttpSession session, 
    HttpServletResponse response, Model model) {
       //get the role/status of the user   
        User user = (User) session.getAttribute("session_user");

        //if not admin redirect to login
        if(user == null || !"ADMIN".equalsIgnoreCase(user.getRole())){
            return "redirect:/login";
        }

        // Prevent caching of pages (from professor)
        response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate" );
        response.setHeader("Pragma", "no-cache");
        response.setHeader("Expires", "0");

        Review review = reviewRepo.findById(rid)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Review not found"));
        

        model.addAttribute("reviews", review);

        return "admin/review/deleteR";
    }

    @PostMapping("/admin/delete/{rid}")
    public String deleteReview(@PathVariable Long rid, HttpSession session, HttpServletResponse response) {
        
        User user = (User) session.getAttribute("session_user");

        //if not admin redirect to login
        if(user == null || !"ADMIN".equalsIgnoreCase(user.getRole())){
            return "redirect:/login";
        }

        // Prevent caching of pages (from professor)
        response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate" );
        response.setHeader("Pragma", "no-cache");
        response.setHeader("Expires", "0");

        reviewRepo.deleteById(rid);

        return "redirect:/admin/reviews";
    }
    
}
