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
    public String seeReviews(Model model,  HttpSession session, HttpServletResponse response) {
        User user = (User) session.getAttribute("session_user");
        //check whether the user already has a review(if he has only allow him to edit previous review)
        boolean hasReview = false;
        if(user != null ){
            hasReview = (reviewRepo.findByAppUser(user) != null);
        }

        // Prevent caching of pages (from professor)
        response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate" );
        response.setHeader("Pragma", "no-cache");
        response.setHeader("Expires", "0");

        model.addAttribute("reviews", reviewRepo.findAll());
        model.addAttribute("hasReview", hasReview);
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

        boolean hasReview = (reviewRepo.findByAppUser(user) != null);

        if (hasReview) {
            return "redirect:/reviews";
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

        boolean hasReview = (reviewRepo.findByAppUser(user) != null); //check if a review with that username exist
        if (hasReview) {
            return "redirect:/reviews";
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

    @GetMapping("/reviews/edit")
    public String editReviewForm(Model model, HttpServletResponse response, HttpSession session) {
        //whether the user is logged in as a customer(guests won't be able to add reviews)
        User user = (User) session.getAttribute("session_user");

        //if not logged in redirect to login
        if(user == null){
            return "redirect:/login";
        }

        boolean hasNoReview = (reviewRepo.findByAppUser(user) == null);

        if (hasNoReview) {
            return "redirect:/reviews";
        }

        // Prevent caching of pages (from professor)
        response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate" );
        response.setHeader("Pragma", "no-cache");
        response.setHeader("Expires", "0");

        model.addAttribute("reviewS", new Review());
        return "customers/review/updateR";
    }

    @PostMapping("/reviews/edit")
    public String submiteditForm(@Valid @ModelAttribute("reviewS") Review rev,
    BindingResult bind, HttpServletResponse response, HttpSession session){
        
        User user = (User) session.getAttribute("session_user");

        //if not logged in redirect to login
        if(user == null){
            return "redirect:/login";
        }

        boolean hasNoReview = (reviewRepo.findByAppUser(user) == null); //check if it has a review
        if (hasNoReview) {
            return "redirect:/reviews";
        }
        // Prevent caching of pages (from professor)
        response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate" );
        response.setHeader("Pragma", "no-cache");
        response.setHeader("Expires", "0");
        
        if(bind.hasErrors()){
            return "customers/review/updateR";
        }
        else{
            Review nrev = reviewRepo.findByAppUser(user); //get an already existing review made by the user
            nrev.setRating(rev.getRating());
            nrev.setComment(rev.getComment());
            reviewRepo.save(nrev);
            return "redirect:/reviews";
        }
    }
    



}
