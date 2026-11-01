package group3.la_panchina.controllers;

import group3.la_panchina.models.Booking;
import group3.la_panchina.repositories.BookingRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/reservations")
public class ReservationController {

    private final BookingRepository bookingRepository;

    // Constructor injection: Spring automatically provides the repository here
    public ReservationController(BookingRepository bookingRepository) {
        this.bookingRepository = bookingRepository;
    }

    // 1. SHOW THE FORM: When user visits GET /reservations
    @GetMapping
    public String showBookingForm(Model model) {
        // Create an empty Booking object for the HTML form to bind to
        model.addAttribute("booking", new Booking());
        return "reservations"; // Tells Spring to look for reservations.html
    }

    // 2. HANDLE SUBMISSION: When user submits the form (POST /reservations)
    @PostMapping
    public String submitBooking(@ModelAttribute Booking booking) {
        // Save the booking to the database
        bookingRepository.save(booking);
        
        // Redirect back to the form with a success message in the URL
        return "redirect:/reservations?success=true";
    }
}