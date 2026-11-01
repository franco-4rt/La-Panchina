package group3.la_panchina.controllers;

import group3.la_panchina.models.Booking;
import group3.la_panchina.repositories.BookingRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;

@Controller
@RequestMapping("/admin")
public class AdminController {

    private final BookingRepository bookingRepository;

    public AdminController(BookingRepository bookingRepository) {
        this.bookingRepository = bookingRepository;
    }

    // When admin visits /admin/bookings
    @GetMapping("/bookings")
    public String viewAllBookings(Model model) {
        // Fetch all bookings from the database
        List<Booking> bookings = bookingRepository.findAll();
        
        // Pass the list to the HTML template
        model.addAttribute("bookings", bookings);
        
        return "admin-bookings";
    }
}