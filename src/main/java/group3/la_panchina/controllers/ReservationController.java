package group3.la_panchina.controllers;

import group3.la_panchina.models.Booking;
import group3.la_panchina.models.BookingSettings;
import group3.la_panchina.models.User;
import group3.la_panchina.repositories.BookingRepository;
import group3.la_panchina.repositories.BookingSettingsRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import jakarta.servlet.http.HttpSession;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Controller
@RequestMapping("/reservations")
public class ReservationController {

    private final BookingRepository bookingRepository;
    private final BookingSettingsRepository bookingSettingsRepository;

    // Constructor injection: Spring provides both repositories here
    public ReservationController(BookingRepository bookingRepository,
                                 BookingSettingsRepository bookingSettingsRepository) {
        this.bookingRepository = bookingRepository;
        this.bookingSettingsRepository = bookingSettingsRepository;
    }

    // Helper: read the admin's settings, or fall back to defaults if no row exists yet.
    // Note: this never writes to the database, so it is safe to call on every request.
    private BookingSettings currentSettings() {
        return bookingSettingsRepository.findById(1L).orElse(new BookingSettings());
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

        // Tell the page what the admin's current booking window is
        BookingSettings settings = currentSettings();
        model.addAttribute("maxDaysAhead", settings.getMaxDaysAhead());
        model.addAttribute("maxDate", LocalDate.now().plusDays(settings.getMaxDaysAhead()).toString());

        return "reservations"; // Tells Spring to look for reservations.html
    }

    // 2. HANDLE SUBMISSION: When user submits the form (POST /reservations)
    @PostMapping
    public String submitBooking(@ModelAttribute Booking booking, HttpSession session, Model model) {
        // 0. Double-check session on POST
        User user = (User) session.getAttribute("session_user");
        if (user == null) {
            return "redirect:/login";
        }

        // Make the booking window available to the page on every return path below
        BookingSettings settings = currentSettings();
        model.addAttribute("maxDaysAhead", settings.getMaxDaysAhead());
        model.addAttribute("maxDate", LocalDate.now().plusDays(settings.getMaxDaysAhead()).toString());

        // --- VALIDATION LOGIC ---

        // 1. Check if date is missing or in the past
        if (booking.getDate() == null || booking.getDate().isBefore(LocalDate.now())) {
            model.addAttribute("errorMessage", "Please select a future date.");
            model.addAttribute("booking", booking);
            return "reservations";
        }

        // 2. NEW: Check the admin-controlled future booking limit
        LocalDate latestAllowed = LocalDate.now().plusDays(settings.getMaxDaysAhead());
        if (booking.getDate().isAfter(latestAllowed)) {
            model.addAttribute("errorMessage",
                    "Bookings can only be made up to " + settings.getMaxDaysAhead() + " days in advance.");
            model.addAttribute("booking", booking);
            return "reservations";
        }

        // 3. Check if it's a Monday (Closed)
        if (booking.getDate().getDayOfWeek() == DayOfWeek.MONDAY) {
            model.addAttribute("errorMessage", "Sorry, the restaurant is closed on Mondays.");
            model.addAttribute("booking", booking);
            return "reservations";
        }

        // 4. Check a time was actually provided
        if (booking.getTime() == null) {
            model.addAttribute("errorMessage", "Please select a time.");
            model.addAttribute("booking", booking);
            return "reservations";
        }

        // 5. Check Opening Hours (with 1-hour cutoff before closing)
        LocalTime time = booking.getTime();
        DayOfWeek day = booking.getDate().getDayOfWeek();
        boolean validTime;

        if (day == DayOfWeek.SUNDAY) {
            // Sunday: closes 20:00, last booking 19:00
            validTime = !time.isBefore(LocalTime.of(8, 0)) && !time.isAfter(LocalTime.of(19, 0));
        } else {
            // Tue-Sat: closes 22:00, last booking 21:00
            validTime = !time.isBefore(LocalTime.of(8, 0)) && !time.isAfter(LocalTime.of(21, 0));
        }

        if (!validTime) {
            model.addAttribute("errorMessage", "Please select a time within our opening hours (bookings close 1 hour before closing).");
            model.addAttribute("booking", booking);
            return "reservations";
        }

        // 6. NEW: Validate party size on the server too (never trust the browser alone)
        if (booking.getPartySize() < 1 || booking.getPartySize() > 20) {
            model.addAttribute("errorMessage", "Party size must be between 1 and 20 guests.");
            model.addAttribute("booking", booking);
            return "reservations";
        }

        // --- IF ALL GOOD, SAVE ---
        booking.setUser(user);
        bookingRepository.save(booking);
        return "redirect:/reservations?success=true";
    }

    // 3. SHOW MY BOOKINGS: When logged-in user visits GET /reservations/my-bookings
    @GetMapping("/my-bookings")
    public String showMyBookings(HttpSession session, Model model) {
        User user = (User) session.getAttribute("session_user");
        if (user == null) {
            return "redirect:/login";
        }

        List<Booking> myBookings = bookingRepository.findByUserOrderByDateDesc(user);
        model.addAttribute("bookings", myBookings);

        return "my-bookings";
    }

    // 4. CANCEL MY BOOKING: Customer cancels their own pending booking
    @PostMapping("/my-bookings/{id}/cancel")
    public String cancelMyBooking(@PathVariable Long id,
                                  HttpSession session,
                                  RedirectAttributes redirectAttributes) {
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