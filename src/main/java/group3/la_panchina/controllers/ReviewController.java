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

    @GetMapping("/customers/review/reviews")
    public String seeReviews(Model model) {

        //check whether the user already has a review(if he has only allow him to edit previous review)
        model.addAttribute("review", reviewRepo.findAll());
        return "review"; //dont put customers/ as to avoid giving xustomers the information 
        // that there also exists a Administrator part.
    }

    @GetMapping("/customers/review/createR")
    public String createReviewForm(Model model) {
        //check whether the user is logged in as a customer(admins and guest won't be able to add reviews)
        model.addAttribute("reviewS", new Review());
        return "review/create";
    }

    @PostMapping("/customer/review/createR")
    public String submitcreateForm(@Valid @ModelAttribute("review") Review rev , BindingResult result){
        if(result.hasErrors()){
            return "review/create";
        }
        else{
            reviewRepo.save(rev);
            return "reviews";
        }
    }
    



}
