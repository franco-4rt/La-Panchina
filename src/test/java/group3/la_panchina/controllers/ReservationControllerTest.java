package group3.la_panchina.controllers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.ui.Model;
import org.springframework.web.servlet.view.InternalResourceViewResolver;

import group3.la_panchina.models.Booking;
import group3.la_panchina.models.BookingSettings;
import group3.la_panchina.models.User;
import group3.la_panchina.repositories.BookingRepository;
import group3.la_panchina.repositories.BookingSettingsRepository;

public class ReservationControllerTest {

    private MockMvc mockMvc;
    private ReservationController reservationController;

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private BookingSettingsRepository bookingSettingsRepository;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        reservationController = new ReservationController(bookingRepository, bookingSettingsRepository);

        // Default settings row: 60 days ahead
        when(bookingSettingsRepository.findById(1L)).thenReturn(Optional.of(new BookingSettings()));

        // Prevents standalone MockMvc from throwing a "Circular view path" error
        InternalResourceViewResolver viewResolver = new InternalResourceViewResolver();
        viewResolver.setPrefix("/templates/");
        viewResolver.setSuffix(".html");

        mockMvc = MockMvcBuilders.standaloneSetup(reservationController)
                .setViewResolvers(viewResolver)
                .build();
    }

    // ---------- helpers ----------

    private User customer(Long id) {
        User u = new User("Test Customer", "customer@test.com", "secret", "CUSTOMER");
        u.setId(id);
        return u;
    }

    private MockHttpSession sessionFor(User u) {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("session_user", u);
        return session;
    }

    // A date 2 days out, nudged forward if it lands on a Monday (restaurant closed)
    private LocalDate nextOpenDate() {
        LocalDate d = LocalDate.now().plusDays(2);
        if (d.getDayOfWeek() == DayOfWeek.MONDAY) {
            d = d.plusDays(1);
        }
        return d;
    }

    private Booking validBooking() {
        Booking b = new Booking();
        b.setName("Test Customer");
        b.setDate(nextOpenDate());
        b.setTime(LocalTime.of(12, 0));
        b.setPartySize(4);
        return b;
    }

    private Booking bookingOwnedBy(User owner, String status) {
        Booking b = validBooking();
        b.setId(5L);
        b.setUser(owner);
        b.setStatus(status);
        return b;
    }

    // ---------- Story 2.1: booking form access ----------

    // FAILURE: guest cannot open the booking form
    @Test
    void ReservationFormAsGuestRedirectsToLogin() throws Exception {
        mockMvc.perform(get("/reservations"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
    }

    // SUCCESS: customer sees the form, plus the admin's booking window
    @Test
    void ReservationFormAsCustomerReturnsReservationsView() throws Exception {
        mockMvc.perform(get("/reservations").session(sessionFor(customer(1L))))
                .andExpect(status().isOk())
                .andExpect(view().name("reservations"))
                .andExpect(model().attributeExists("booking"))
                .andExpect(model().attribute("maxDaysAhead", 60))
                .andExpect(model().attributeExists("maxDate"));
    }

    // ---------- Story 2.1: my-bookings page ----------

    // FAILURE: guest cannot view booking history
    @Test
    void MyBookingsAsGuestRedirectsToLogin() throws Exception {
        mockMvc.perform(get("/reservations/my-bookings"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));

        verify(bookingRepository, never()).findByUserOrderByDateDesc(any());
    }

    // SUCCESS: history is queried using the SESSION user, so only their rows come back
    @Test
    void MyBookingsAsCustomerQueriesOnlyTheirOwnBookings() throws Exception {
        User customer = customer(1L);
        List<Booking> mine = List.of(bookingOwnedBy(customer, "pending"));
        when(bookingRepository.findByUserOrderByDateDesc(customer)).thenReturn(mine);

        mockMvc.perform(get("/reservations/my-bookings").session(sessionFor(customer)))
                .andExpect(status().isOk())
                .andExpect(view().name("my-bookings"))
                .andExpect(model().attribute("bookings", mine));

        // This is the assertion that proves data scoping: the query used THIS user
        verify(bookingRepository).findByUserOrderByDateDesc(customer);
    }

    // ---------- Story 2.3: customer cancels own pending booking ----------

    // FAILURE: guest cannot cancel anything
    @Test
    void CancelAsGuestRedirectsToLogin() throws Exception {
        mockMvc.perform(post("/reservations/my-bookings/5/cancel"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));

        verify(bookingRepository, never()).save(any());
    }

    // SUCCESS: owner cancels a pending booking
    @Test
    void CancelOwnPendingBookingSucceeds() throws Exception {
        User customer = customer(1L);
        Booking booking = bookingOwnedBy(customer, "pending");
        when(bookingRepository.findById(5L)).thenReturn(Optional.of(booking));

        mockMvc.perform(post("/reservations/my-bookings/5/cancel").session(sessionFor(customer)))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/reservations/my-bookings"));

        assertEquals("cancelled", booking.getStatus());
        verify(bookingRepository).save(booking);
    }

    // FAILURE: an already-confirmed booking cannot be cancelled by the customer
    @Test
    void CancelConfirmedBookingIsRejected() throws Exception {
        User customer = customer(1L);
        Booking booking = bookingOwnedBy(customer, "confirmed");
        when(bookingRepository.findById(5L)).thenReturn(Optional.of(booking));

        mockMvc.perform(post("/reservations/my-bookings/5/cancel").session(sessionFor(customer)))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/reservations/my-bookings"));

        assertEquals("confirmed", booking.getStatus());
        verify(bookingRepository, never()).save(any());
    }

    // FAILURE (security): a customer cannot cancel somebody else's booking
    @Test
    void CancelAnotherUsersBookingIsRejected() throws Exception {
        User owner = customer(2L);      // booking belongs to user 2
        User attacker = customer(1L);   // logged in as user 1
        Booking booking = bookingOwnedBy(owner, "pending");
        when(bookingRepository.findById(5L)).thenReturn(Optional.of(booking));

        mockMvc.perform(post("/reservations/my-bookings/5/cancel").session(sessionFor(attacker)))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/reservations/my-bookings"));

        assertEquals("pending", booking.getStatus());
        verify(bookingRepository, never()).save(any());
    }

    // FAILURE: cancelling a booking id that does not exist
    @Test
    void CancelNonexistentBookingIsRejected() throws Exception {
        when(bookingRepository.findById(99L)).thenReturn(Optional.empty());

        mockMvc.perform(post("/reservations/my-bookings/99/cancel").session(sessionFor(customer(1L))))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/reservations/my-bookings"));

        verify(bookingRepository, never()).save(any());
    }

    // ---------- Story 2.4 + Iteration 1 rules: submission validation ----------
    // These call the controller method directly. Form binding of LocalDate/LocalTime
    // depends on MVC conversion config, so testing the method keeps these assertions
    // focused on the business rules themselves.

    // FAILURE: date beyond the admin-controlled window
    @Test
    void SubmitBookingBeyondAdminWindowIsRejected() {
        BookingSettings settings = new BookingSettings();
        settings.setMaxDaysAhead(7);
        when(bookingSettingsRepository.findById(1L)).thenReturn(Optional.of(settings));

        Booking booking = validBooking();
        booking.setDate(LocalDate.now().plusDays(30)); // outside the 7-day window

        Model model = new ExtendedModelMap();
        String view = reservationController.submitBooking(booking, sessionFor(customer(1L)), model);

        assertEquals("reservations", view);
        assertNotNull(model.getAttribute("errorMessage"));
        verify(bookingRepository, never()).save(any());
    }

    // SUCCESS: date inside the admin-controlled window is saved and linked to the user
    @Test
    void SubmitBookingWithinAdminWindowIsSaved() {
        BookingSettings settings = new BookingSettings();
        settings.setMaxDaysAhead(7);
        when(bookingSettingsRepository.findById(1L)).thenReturn(Optional.of(settings));

        User customer = customer(1L);
        Booking booking = validBooking(); // 2-3 days out, midday, party of 4

        Model model = new ExtendedModelMap();
        String view = reservationController.submitBooking(booking, sessionFor(customer), model);

        assertEquals("redirect:/reservations?success=true", view);
        assertEquals(customer, booking.getUser());
        verify(bookingRepository).save(booking);
    }

    // FAILURE: party size bypassing the HTML min/max is still caught server-side
    @Test
    void SubmitBookingWithInvalidPartySizeIsRejected() {
        Booking booking = validBooking();
        booking.setPartySize(0);

        Model model = new ExtendedModelMap();
        String view = reservationController.submitBooking(booking, sessionFor(customer(1L)), model);

        assertEquals("reservations", view);
        assertNotNull(model.getAttribute("errorMessage"));
        verify(bookingRepository, never()).save(any());
    }

    // FAILURE: Monday is still rejected (Iteration 1 rule, regression guard)
    @Test
    void SubmitBookingOnMondayIsRejected() {
        LocalDate monday = LocalDate.now().with(DayOfWeek.MONDAY).plusWeeks(1);
        Booking booking = validBooking();
        booking.setDate(monday);

        Model model = new ExtendedModelMap();
        String view = reservationController.submitBooking(booking, sessionFor(customer(1L)), model);

        assertEquals("reservations", view);
        assertNotNull(model.getAttribute("errorMessage"));
        verify(bookingRepository, never()).save(any());
    }
}