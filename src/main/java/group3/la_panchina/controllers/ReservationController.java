package group3.la_panchina.controllers;

import group3.la_panchina.models.Booking;
import group3.la_panchina.models.User;
import group3.la_panchina.repositories.BookingRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import jakarta.servlet.http.HttpSession;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

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
    public String showBookingForm(HttpSession session, Model model) {
        // 1. Check if user is logged in
        User user = (User) session.getAttribute("session_user");
        if (user == null) {
            return "redirect:/login"; // Redirect to login if not authenticated
        }
        // 2. Pre-fill the booking name with the logged-in user's name
        Booking booking = new Booking();
        booking.setName(user.getName());
        
        // Create an empty Booking object for the HTML form to bind to
        model.addAttribute("booking", booking);
        return "reservations"; // Tells Spring to look for reservations.html
    }

    // 2. HANDLE SUBMISSION: When user submits the form (POST /reservations)
    @PostMapping
    public String submitBooking(@ModelAttribute Booking booking, HttpSession session, Model model) {
        // --- VALIDATION LOGIC ---
        // 0. Double-check session on POST
        User user = (User) session.getAttribute("session_user");
        if (user == null) {
            return "redirect:/login";
        }

        // 1. Check if date is in the past
        if (booking.getDate() == null || booking.getDate().isBefore(LocalDate.now())) {
            model.addAttribute("errorMessage", "Please select a future date.");
            model.addAttribute("booking", booking); // Keeps the user's typed data
            return "reservations";
        }

        // 2. Check if it's a Monday (Closed)
        if (booking.getDate().getDayOfWeek() == DayOfWeek.MONDAY) {
            model.addAttribute("errorMessage", "Sorry, the restaurant is closed on Mondays.");
            model.addAttribute("booking", booking);
            return "reservations";
        }

        // 3. Check Opening Hours (with 1-hour cutoff before closing)
        LocalTime time = booking.getTime();
        DayOfWeek day = booking.getDate().getDayOfWeek();
        boolean validTime = false;

        if (day == DayOfWeek.SUNDAY) {
            // Sunday: Closes at 20:00 (8 PM), so last booking is 19:00 (7 PM)
            validTime = !time.isBefore(LocalTime.of(8, 0)) && !time.isAfter(LocalTime.of(19, 0));
        } else {
            // Tue-Sat: Closes at 22:00 (10 PM), so last booking is 21:00 (9 PM)
            validTime = !time.isBefore(LocalTime.of(8, 0)) && !time.isAfter(LocalTime.of(21, 0));
        }

        if (!validTime) {
            model.addAttribute("errorMessage", "Please select a time within our opening hours (bookings close 1 hour before closing).");
            model.addAttribute("booking", booking);
            return "reservations";
        }

        // If all good, save
        // 4. Link the booking to the logged-in user before saving
        booking.setUser(user);

        bookingRepository.save(booking);
        // Redirect back to the form with a success message in the URL
        return "redirect:/reservations?success=true";
    }

    // 3. SHOW MY BOOKINGS: When logged-in user visits GET /reservations/my-bookings
    @GetMapping("/my-bookings")
    public String showMyBookings(HttpSession session, Model model) {
        // Check if user is logged in
        User user = (User) session.getAttribute("session_user");
        if (user == null) {
            return "redirect:/login";
        }
        
        // Fetch only this user's bookings, sorted by date
        List<Booking> myBookings = bookingRepository.findByUserOrderByDateDesc(user);
        model.addAttribute("bookings", myBookings);
        
        return "my-bookings"; // Tells Spring to look for my-bookings.html
    }

    // 4. CANCEL MY BOOKING: When customer clicks Cancel on their own pending booking
    @PostMapping("/my-bookings/{id}/cancel")
    public String cancelMyBooking(@PathVariable Long id, 
                                  HttpSession session, 
                                  org.springframework.web.servlet.mvc.support.RedirectAttributes redirectAttributes) {
        // 1. Check if user is logged in
        User user = (User) session.getAttribute("session_user");
        if (user == null) {
            return "redirect:/login";
        }

        // 2. Find the booking
        Booking booking = bookingRepository.findById(id).orElse(null);

        // 3. Security & Business Logic Checks
        if (booking != null) {
            // Check if the booking actually belongs to this logged-in user
            if (booking.getUser().getId().equals(user.getId())) {
                // Check if the booking is still pending
                if ("pending".equalsIgnoreCase(booking.getStatus())) {
                    booking.setStatus("cancelled");
                    bookingRepository.save(booking);
                    redirectAttributes.addFlashAttribute("successMessage", "Your booking has been successfully cancelled.");
                } else {
                    redirectAttributes.addFlashAttribute("errorMessage", "Only pending bookings can be cancelled.");
                }
            } else {
                redirectAttributes.addFlashAttribute("errorMessage", "You are not authorized to cancel this booking.");
            }
        } else {
            redirectAttributes.addFlashAttribute("errorMessage", "Booking not found.");
        }

        // 4. Redirect back to the my-bookings page
        return "redirect:/reservations/my-bookings";
    }
}