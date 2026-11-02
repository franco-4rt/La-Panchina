package group3.la_panchina.controllers;

import group3.la_panchina.models.Booking;
import group3.la_panchina.models.User;
import group3.la_panchina.repositories.BookingRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import jakarta.servlet.http.HttpSession;
import jakarta.servlet.http.HttpServletResponse;

import java.util.List;

@Controller
@RequestMapping("/admin")
public class AdminBookingController {

    private final BookingRepository bookingRepository;

    public AdminBookingController(BookingRepository bookingRepository) {
        this.bookingRepository = bookingRepository;
    }

    // When admin visits /admin/bookings 
    @GetMapping("/bookings")
    public String viewAllBookings(HttpSession session, HttpServletResponse response, Model model) {
        // 1. Ensure only admin can access
        User user = (User) session.getAttribute("session_user");
        if (user == null || !"ADMIN".equalsIgnoreCase(user.getRole())) {
            return "redirect:/login";
        }
        // 2. Prevent caching of authenticated pages (matching teammate's pattern)
        response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
        response.setHeader("Pragma", "no-cache");
        response.setHeader("Expires", "0");

        // 3. Fetch and display bookings
        List<Booking> bookings = bookingRepository.findAll();
        
        // Pass the list to the HTML template
        model.addAttribute("bookings", bookings);
        
        return "admin-bookings";
    }
}