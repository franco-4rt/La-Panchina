package group3.la_panchina.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

import group3.la_panchina.models.Review;
import group3.la_panchina.models.User;
import group3.la_panchina.repositories.ReviewRepository;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;


@Controller //This controller will be for customer functions
public class ReviewController{
    @Autowired
    private ReviewRepository reviewRepo;

    @GetMapping("/reviews")
    public String seeReviews(Model model) {

        //check whether the user already has a review(if he has only allow him to edit previous review)
        model.addAttribute("reviews", reviewRepo.findAll());
        return "customers/review/reviews";
    }

    @GetMapping("/reviews/create")
    public String createReviewForm(Model model, HttpServletResponse response, HttpSession session) {
        //whether the user is logged in as a customer(guests won't be able to add reviews)
        User user = (User) session.getAttribute("session_user");

        //if not logged in redirect to login
        if(user == null){
            return "redirect:/login";
        }
        // Prevent caching of pages (from professor)
        response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate" );
        response.setHeader("Pragma", "no-cache");
        response.setHeader("Expires", "0");

        model.addAttribute("reviewS", new Review());
        return "customers/review/createR";
    }

    @PostMapping("/reviews/create")
    public String submitcreateForm(@Valid @ModelAttribute("reviewS") Review rev,
    BindingResult bind, HttpServletResponse response, HttpSession session){
        
        User user = (User) session.getAttribute("session_user");

        //if not logged in redirect to login
        if(user == null){
            return "redirect:/login";
        }

        // Prevent caching of pages (from professor)
        response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate" );
        response.setHeader("Pragma", "no-cache");
        response.setHeader("Expires", "0");
        
        if(bind.hasErrors()){
            return "customers/review/createR";
        }
        else{
            rev.setUser(user);
            reviewRepo.save(rev);
            return "redirect:/reviews";
        }
    }
    



}
