package group3.la_panchina.controllers;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.servlet.view.InternalResourceViewResolver;

import group3.la_panchina.models.Booking;
import group3.la_panchina.models.User;
import group3.la_panchina.repositories.BookingRepository;

public class AdminBookingControllerTest {

    private MockMvc mockMvc;
    private AdminBookingController adminBookingController;

    @Mock
    private BookingRepository bookingRepository;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        adminBookingController = new AdminBookingController(bookingRepository);

        // Prevents Spring standalone MockMvc from throwing a "Circular view path" error
        InternalResourceViewResolver viewResolver = new InternalResourceViewResolver();
        viewResolver.setPrefix("/templates/");
        viewResolver.setSuffix(".html");

        mockMvc = MockMvcBuilders.standaloneSetup(adminBookingController)
                .setViewResolvers(viewResolver)
                .build();
    }

    // FAILURE CASE: Ensure that an unauthenticated guest cannot access the admin bookings page
    @Test
    void AdminBookingsAsGuestRedirectsToLogin() throws Exception {
        mockMvc.perform(get("/admin/bookings"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));

        verify(bookingRepository, never()).findAll();
    }

    // FAILURE CASE: Ensure that a logged-in Customer cannot access the admin bookings page
    @Test
    void AdminBookingsAsCustomerRedirectsToLogin() throws Exception {
        User customer = new User("Test Customer", "customer@test.com", "secret", "CUSTOMER");
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("session_user", customer);

        mockMvc.perform(get("/admin/bookings").session(session))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));

        verify(bookingRepository, never()).findAll();
    }

    // SUCCESS CASE: Ensure that an Admin user can view the bookings list
    @Test
    void AdminBookingsAsAdminReturnsAdminBookingsView() throws Exception {
        User admin = new User("Admin", "admin@lapanchina.com", "secret", "ADMIN");
        admin.setId(1L);

        List<Booking> bookingList = List.of(new Booking());
        when(bookingRepository.findAll()).thenReturn(bookingList);

        MockHttpSession session = new MockHttpSession();
        session.setAttribute("session_user", admin);

        mockMvc.perform(get("/admin/bookings").session(session))
                .andExpect(status().isOk())
                .andExpect(view().name("admin-bookings"))
                .andExpect(model().attributeExists("bookings"))
                .andExpect(model().attribute("bookings", bookingList));

        verify(bookingRepository).findAll();
    }
}