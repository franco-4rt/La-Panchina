package group3.la_panchina.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

import group3.la_panchina.models.Review;
import group3.la_panchina.repositories.ReviewRepository;
import jakarta.validation.Valid;


@Controller //This controller will be for customer functions
public class ReviewController {
    @Autowired
    private ReviewRepository reviewRepo;

    @GetMapping("/reviews")
    public String seeReviews(Model model) {

        //check whether the user already has a review(if he has only allow him to edit previous review)
        model.addAttribute("reviews", reviewRepo.findAll());
        return "customers/review/reviews";
    }

    @GetMapping("/reviews/create")
    public String createReviewForm(Model model) {
        //check whether the user is logged in as a customer(admins and guest won't be able to add reviews)
        model.addAttribute("reviewS", new Review());
        return "customers/review/createR";
    }

    @PostMapping("/review/create")
    public String submitcreateForm(@Valid @ModelAttribute("review") Review rev , BindingResult result){
        if(result.hasErrors()){
            return "customers/review/createR";
        }
        else{
            reviewRepo.save(rev);
            return "redirect:/reviews";
        }
    }
    



}
