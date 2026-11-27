package group3.la_panchina.controllers;

import group3.la_panchina.models.Booking;
import group3.la_panchina.models.BookingSettings;
import group3.la_panchina.models.User;
import group3.la_panchina.repositories.BookingRepository;
import group3.la_panchina.repositories.BookingSettingsRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import jakarta.servlet.http.HttpSession;
import jakarta.servlet.http.HttpServletResponse;

import java.util.List;

@Controller
@RequestMapping("/admin")
public class AdminBookingController {

    private final BookingRepository bookingRepository;
    private final BookingSettingsRepository bookingSettingsRepository;

    public AdminBookingController(BookingRepository bookingRepository,
                                  BookingSettingsRepository bookingSettingsRepository) {
        this.bookingRepository = bookingRepository;
        this.bookingSettingsRepository = bookingSettingsRepository;
    }

    // Small helper so we don't repeat the admin role check four times
    private boolean isNotAdmin(HttpSession session) {
        User user = (User) session.getAttribute("session_user");
        return user == null || !"ADMIN".equalsIgnoreCase(user.getRole());
    }

    // Prevent caching of authenticated pages (matching teammate's pattern)
    private void noCache(HttpServletResponse response) {
        response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
        response.setHeader("Pragma", "no-cache");
        response.setHeader("Expires", "0");
    }

    // When admin visits /admin/bookings
    @GetMapping("/bookings")
    public String viewAllBookings(HttpSession session, HttpServletResponse response, Model model) {
        if (isNotAdmin(session)) {
            return "redirect:/login";
        }
        noCache(response);

        List<Booking> bookings = bookingRepository.findAll();
        model.addAttribute("bookings", bookings);

        return "admin-bookings";
    }

    // UPDATE STATUS: When admin clicks Confirm or Cancel
    @PostMapping("/bookings/{id}/status")
    public String updateBookingStatus(@PathVariable Long id,
                                      @RequestParam String status,
                                      HttpSession session,
                                      HttpServletResponse response,
                                      RedirectAttributes redirectAttributes) {
        if (isNotAdmin(session)) {
            return "redirect:/login";
        }
        noCache(response);

        Booking booking = bookingRepository.findById(id).orElse(null);
        if (booking == null) {
            redirectAttributes.addFlashAttribute("errorMessage", "Booking not found.");
        } else if ("confirmed".equalsIgnoreCase(status) || "cancelled".equalsIgnoreCase(status)) {
            booking.setStatus(status.toLowerCase());
            bookingRepository.save(booking);
            redirectAttributes.addFlashAttribute("successMessage", "Booking #" + id + " is now " + status.toLowerCase() + ".");
        } else {
            redirectAttributes.addFlashAttribute("errorMessage", "Invalid status value.");
        }

        return "redirect:/admin/bookings";
    }

    // SHOW SETTINGS: admin views the booking rules page
    @GetMapping("/bookings/settings")
    public String showBookingSettings(HttpSession session, HttpServletResponse response, Model model) {
        if (isNotAdmin(session)) {
            return "redirect:/login";
        }
        noCache(response);

        BookingSettings settings = bookingSettingsRepository.findById(1L).orElse(new BookingSettings());
        model.addAttribute("settings", settings);

        return "admin-booking-settings";
    }

    // SAVE SETTINGS: admin updates the booking window and the per-slot capacity
    @PostMapping("/bookings/settings")
    public String saveBookingSettings(@RequestParam int maxDaysAhead,
                                      @RequestParam(defaultValue = "40") int maxCoversPerSlot,
                                      HttpSession session,
                                      HttpServletResponse response,
                                      RedirectAttributes redirectAttributes) {
        if (isNotAdmin(session)) {
            return "redirect:/login";
        }
        noCache(response);

        // Business rule: keep the booking window sane (1 day to 1 year)
        if (maxDaysAhead < 1 || maxDaysAhead > 365) {
            redirectAttributes.addFlashAttribute("errorMessage", "The booking window must be between 1 and 365 days.");
            return "redirect:/admin/bookings/settings";
        }

        // Business rule: capacity must be a realistic positive number
        if (maxCoversPerSlot < 1 || maxCoversPerSlot > 500) {
            redirectAttributes.addFlashAttribute("errorMessage", "The capacity per time slot must be between 1 and 500 guests.");
            return "redirect:/admin/bookings/settings";
        }

        BookingSettings settings = bookingSettingsRepository.findById(1L).orElse(new BookingSettings());
        settings.setMaxDaysAhead(maxDaysAhead);
        settings.setMaxCoversPerSlot(maxCoversPerSlot);
        bookingSettingsRepository.save(settings);

        redirectAttributes.addFlashAttribute("successMessage",
                "Settings saved: bookings up to " + maxDaysAhead + " days ahead, "
                + maxCoversPerSlot + " guests per time slot.");

        return "redirect:/admin/bookings/settings";
    }
}