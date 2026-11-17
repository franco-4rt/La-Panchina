package group3.la_panchina.controllers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.servlet.view.InternalResourceViewResolver;

import group3.la_panchina.models.Booking;
import group3.la_panchina.models.BookingSettings;
import group3.la_panchina.models.User;
import group3.la_panchina.repositories.BookingRepository;
import group3.la_panchina.repositories.BookingSettingsRepository;

public class AdminBookingControllerTest {

    private MockMvc mockMvc;
    private AdminBookingController adminBookingController;

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private BookingSettingsRepository bookingSettingsRepository;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        adminBookingController = new AdminBookingController(bookingRepository, bookingSettingsRepository);

        when(bookingSettingsRepository.findById(1L)).thenReturn(Optional.of(new BookingSettings()));

        InternalResourceViewResolver viewResolver = new InternalResourceViewResolver();
        viewResolver.setPrefix("/templates/");
        viewResolver.setSuffix(".html");

        mockMvc = MockMvcBuilders.standaloneSetup(adminBookingController)
                .setViewResolvers(viewResolver)
                .build();
    }

    // ---------- helpers ----------

    private User admin() {
        User u = new User("Admin", "admin@lapanchina.com", "secret", "ADMIN");
        u.setId(1L);
        return u;
    }

    private User customer() {
        User u = new User("Test Customer", "customer@test.com", "secret", "CUSTOMER");
        u.setId(2L);
        return u;
    }

    private MockHttpSession sessionFor(User u) {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("session_user", u);
        return session;
    }

    private Booking pendingBooking(Long id) {
        Booking b = new Booking();
        b.setId(id);
        b.setName("Test Customer");
        b.setStatus("pending");
        return b;
    }

    // ---------- Story 2.2: viewing the admin list ----------

    @Test
    void AdminBookingsAsGuestRedirectsToLogin() throws Exception {
        mockMvc.perform(get("/admin/bookings"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));

        verify(bookingRepository, never()).findAll();
    }

    @Test
    void AdminBookingsAsCustomerRedirectsToLogin() throws Exception {
        mockMvc.perform(get("/admin/bookings").session(sessionFor(customer())))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));

        verify(bookingRepository, never()).findAll();
    }

    @Test
    void AdminBookingsAsAdminReturnsAdminBookingsView() throws Exception {
        List<Booking> bookingList = List.of(pendingBooking(1L));
        when(bookingRepository.findAll()).thenReturn(bookingList);

        mockMvc.perform(get("/admin/bookings").session(sessionFor(admin())))
                .andExpect(status().isOk())
                .andExpect(view().name("admin-bookings"))
                .andExpect(model().attribute("bookings", bookingList))
                .andExpect(header().string("Cache-Control", "no-cache, no-store, must-revalidate"));

        verify(bookingRepository).findAll();
    }

    // ---------- Story 2.2: confirm / cancel ----------

    @Test
    void AdminConfirmsPendingBooking() throws Exception {
        Booking booking = pendingBooking(1L);
        when(bookingRepository.findById(1L)).thenReturn(Optional.of(booking));

        mockMvc.perform(post("/admin/bookings/1/status")
                        .param("status", "confirmed")
                        .session(sessionFor(admin())))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/bookings"));

        assertEquals("confirmed", booking.getStatus());
        verify(bookingRepository).save(booking);
    }

    @Test
    void AdminCancelsPendingBooking() throws Exception {
        Booking booking = pendingBooking(1L);
        when(bookingRepository.findById(1L)).thenReturn(Optional.of(booking));

        mockMvc.perform(post("/admin/bookings/1/status")
                        .param("status", "cancelled")
                        .session(sessionFor(admin())))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/bookings"));

        assertEquals("cancelled", booking.getStatus());
        verify(bookingRepository).save(booking);
    }

    // FAILURE: an arbitrary status value must never reach the database
    @Test
    void AdminInvalidStatusIsRejected() throws Exception {
        Booking booking = pendingBooking(1L);
        when(bookingRepository.findById(1L)).thenReturn(Optional.of(booking));

        mockMvc.perform(post("/admin/bookings/1/status")
                        .param("status", "banana")
                        .session(sessionFor(admin())))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/bookings"));

        assertEquals("pending", booking.getStatus());
        verify(bookingRepository, never()).save(any());
    }

    // FAILURE (security): customer cannot change any booking status
    @Test
    void CustomerCannotUpdateBookingStatus() throws Exception {
        mockMvc.perform(post("/admin/bookings/1/status")
                        .param("status", "confirmed")
                        .session(sessionFor(customer())))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));

        verify(bookingRepository, never()).save(any());
    }

    // ---------- Story 2.4: admin-controlled booking window ----------

    @Test
    void AdminViewsBookingSettings() throws Exception {
        mockMvc.perform(get("/admin/bookings/settings").session(sessionFor(admin())))
                .andExpect(status().isOk())
                .andExpect(view().name("admin-booking-settings"))
                .andExpect(model().attributeExists("settings"));
    }

    @Test
    void AdminSavesValidBookingWindow() throws Exception {
        mockMvc.perform(post("/admin/bookings/settings")
                        .param("maxDaysAhead", "30")
                        .session(sessionFor(admin())))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/bookings/settings"));

        // Capture what was actually written to the database
        ArgumentCaptor<BookingSettings> captor = ArgumentCaptor.forClass(BookingSettings.class);
        verify(bookingSettingsRepository).save(captor.capture());
        assertEquals(30, captor.getValue().getMaxDaysAhead());
    }

    @Test
    void AdminBookingWindowBelowMinimumIsRejected() throws Exception {
        mockMvc.perform(post("/admin/bookings/settings")
                        .param("maxDaysAhead", "0")
                        .session(sessionFor(admin())))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/bookings/settings"));

        verify(bookingSettingsRepository, never()).save(any());
    }

    @Test
    void AdminBookingWindowAboveMaximumIsRejected() throws Exception {
        mockMvc.perform(post("/admin/bookings/settings")
                        .param("maxDaysAhead", "400")
                        .session(sessionFor(admin())))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/bookings/settings"));

        verify(bookingSettingsRepository, never()).save(any());
    }

    @Test
    void CustomerCannotViewBookingSettings() throws Exception {
        mockMvc.perform(get("/admin/bookings/settings").session(sessionFor(customer())))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
    }

    @Test
    void CustomerCannotSaveBookingSettings() throws Exception {
        mockMvc.perform(post("/admin/bookings/settings")
                        .param("maxDaysAhead", "30")
                        .session(sessionFor(customer())))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));

        verify(bookingSettingsRepository, never()).save(any());
    }
}