package group3.la_panchina.controllers;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.servlet.view.InternalResourceViewResolver;

import group3.la_panchina.models.User;
import group3.la_panchina.repositories.BookingRepository;

public class ReservationControllerTest {

    private MockMvc mockMvc;
    private ReservationController reservationController;

    @Mock
    private BookingRepository bookingRepository;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        reservationController = new ReservationController(bookingRepository);

        // Prevents Spring standalone MockMvc from throwing a "Circular view path" error
        InternalResourceViewResolver viewResolver = new InternalResourceViewResolver();
        viewResolver.setPrefix("/templates/");
        viewResolver.setSuffix(".html");

        mockMvc = MockMvcBuilders.standaloneSetup(reservationController)
                .setViewResolvers(viewResolver)
                .build();
    }

    // FAILURE CASE: Ensure that an unauthenticated guest cannot access the reservation form and is redirected to /login
    @Test
    void ReservationFormAsGuestRedirectsToLogin() throws Exception {
        mockMvc.perform(get("/reservations"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
    }

    // SUCCESS CASE: Ensure that a logged-in Customer can access the reservation form
    @Test
    void ReservationFormAsCustomerReturnsReservationsView() throws Exception {
        User customer = new User("Test Customer", "customer@test.com", "secret", "CUSTOMER");
        customer.setId(1L);
        
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("session_user", customer);

        mockMvc.perform(get("/reservations").session(session))
                .andExpect(status().isOk())
                .andExpect(view().name("reservations"))
                .andExpect(model().attributeExists("booking"));
    }
}