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
import group3.la_panchina.repositories.ReviewRepository;



@Controller
public class AdminReviewController {

    @Autowired
    private ReviewRepository reviewRepo;

    @GetMapping("/admin/review/reviews")
    public String showReviewsAdmin(Model model) {
        //add a checker for the user status(admin or customer/not logged in)
            //if not admin redirect to login

        model.addAttribute("reviews", reviewRepo.findAll());

        return "admin/reviews";
    }
    
    @GetMapping("/admin/review/deleteR/{id}")
    public String showDeleteConfirmation(@PathVariable Long id, Model model) {
        Review review = reviewRepo.findById(id)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,"review not found"));
        //add a checker for the user status(admin or customer/not logged in)
            //if not admin redirect to login

        model.addAttribute("reviews", review);

        return "admin/reviews/delete";
    }

    @PostMapping("/admin/review/deleteR/{id}")
    public String deleteReview(@PathVariable Long id) {
        
        //add a checker for the user status(admin or customer/ not logged in)
            //if not admin redirect to login

        reviewRepo.deleteById(id);

        return "redirect:/admin/review/reviews";
    }
    
}
