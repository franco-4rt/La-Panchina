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

    @GetMapping("/admin/reviews")
    public String showReviewsAdmin(Model model) {
        //add a checker for the user status(admin or customer/not logged in)
            //if not admin redirect to login

        model.addAttribute("reviews", reviewRepo.findAll());

        return "admin/review/reviews";
    }
    
    @GetMapping("/admin/delete/{rid}")
    public String showDeleteConfirmation(@PathVariable Long rid, Model model) {
        //add a checker for the user status(admin or customer/not logged in)
            //if not admin redirect to login

        Review review = reviewRepo.findById(rid)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Review not found"));
        

        model.addAttribute("reviews", review);

        return "admin/review/deleteR";
    }

    @PostMapping("/admin/delete/{rid}")
    public String deleteReview(@PathVariable Long rid) {
        
        //add a checker for the user status(admin or customer/ not logged in)
            //if not admin redirect to login

        reviewRepo.deleteById(rid);

        return "redirect:/admin/reviews";
    }
    
}
