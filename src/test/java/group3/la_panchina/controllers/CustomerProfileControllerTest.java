package group3.la_panchina.controllers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.servlet.view.InternalResourceViewResolver;

import group3.la_panchina.models.User;
import group3.la_panchina.repositories.UserRepository;

public class CustomerProfileControllerTest {

    private MockMvc mockMvc;
    private CustomerProfileController customerProfileController;

    @Mock
    private UserRepository userRepository;

    private User testUser;
    private BCryptPasswordEncoder passwordEncoder;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        customerProfileController = new CustomerProfileController(userRepository);

        InternalResourceViewResolver viewResolver = new InternalResourceViewResolver();
        viewResolver.setPrefix("/templates/");
        viewResolver.setSuffix(".html");

        mockMvc = MockMvcBuilders.standaloneSetup(customerProfileController)
                .setViewResolvers(viewResolver)
                .build();

        passwordEncoder = new BCryptPasswordEncoder();
        testUser = new User("John Doe", "john@example.com", passwordEncoder.encode("oldPassword123"), "CUSTOMER");
        testUser.setId(1L);
        testUser.setPhone("604-555-0123");
        testUser.setContactPreference("Email");
    }

    // Unauthenticated guest visiting /profile redirects to /login
    @Test
    void ProfileAsGuestRedirectsToLogin() throws Exception {
        mockMvc.perform(get("/profile"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
    }

    // Authenticated customer visiting /profile shows CustomerProfile page
    @Test
    void ProfileAsCustomerReturnsProfileView() throws Exception {
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

        MockHttpSession session = new MockHttpSession();
        session.setAttribute("session_user", testUser);

        mockMvc.perform(get("/profile").session(session))
                .andExpect(status().isOk())
                .andExpect(view().name("CustomerProfile"))
                .andExpect(model().attributeExists("user"));
    }

    // Updating profile with valid name, phone, and preference succeeds
    @Test
    void UpdateProfileValidDataSucceeds() throws Exception {
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        MockHttpSession session = new MockHttpSession();
        session.setAttribute("session_user", testUser);

        mockMvc.perform(post("/profile")
                .session(session)
                .param("name", "Jane Doe")
                .param("phone", "604-555-9876")
                .param("contactPreference", "Phone"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/profile"))
                .andExpect(flash().attribute("successMessage", "Profile details updated successfully!"));

        verify(userRepository).save(any(User.class));
        User sessionUser = (User) session.getAttribute("session_user");
        assertEquals("Jane Doe", sessionUser.getName());
        assertEquals("604-555-9876", sessionUser.getPhone());
        assertEquals("Phone", sessionUser.getContactPreference());
    }

    // Updating profile with empty name fails
    @Test
    void UpdateProfileEmptyNameFails() throws Exception {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("session_user", testUser);

        mockMvc.perform(post("/profile")
                .session(session)
                .param("name", "   ")
                .param("phone", "604-555-9876")
                .param("contactPreference", "Email"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/profile"))
                .andExpect(flash().attribute("errorMessage", "Name cannot be empty."));

        verify(userRepository, never()).save(any(User.class));
    }

    // Updating profile with invalid phone fails
    @Test
    void UpdateProfileInvalidPhoneFails() throws Exception {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("session_user", testUser);

        mockMvc.perform(post("/profile")
                .session(session)
                .param("name", "Jane Doe")
                .param("phone", "invalid-phone")
                .param("contactPreference", "Email"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/profile"))
                .andExpect(flash().attribute("errorMessage",
                        "Please enter a valid 10-digit phone number (e.g. 555-123-4567)."));

        verify(userRepository, never()).save(any(User.class));
    }

    // Changing password with incorrect current password fails
    @Test
    void ChangePasswordIncorrectCurrentPasswordFails() throws Exception {
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

        MockHttpSession session = new MockHttpSession();
        session.setAttribute("session_user", testUser);

        mockMvc.perform(post("/profile/password")
                .session(session)
                .param("currentPassword", "wrongPassword")
                .param("newPassword", "newSecret123")
                .param("confirmPassword", "newSecret123"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/profile"))
                .andExpect(flash().attribute("passwordError", "Current password does not match."));

        verify(userRepository, never()).save(any(User.class));
    }

    // Changing password with new password shorter than 6 characters fails
    @Test
    void ChangePasswordTooShortFails() throws Exception {
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

        MockHttpSession session = new MockHttpSession();
        session.setAttribute("session_user", testUser);

        mockMvc.perform(post("/profile/password")
                .session(session)
                .param("currentPassword", "oldPassword123")
                .param("newPassword", "12345")
                .param("confirmPassword", "12345"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/profile"))
                .andExpect(flash().attribute("passwordError", "New password must be at least 6 characters long."));

        verify(userRepository, never()).save(any(User.class));
    }

    // Changing password with mismatched confirmation fails
    @Test
    void ChangePasswordMismatchFails() throws Exception {
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

        MockHttpSession session = new MockHttpSession();
        session.setAttribute("session_user", testUser);

        mockMvc.perform(post("/profile/password")
                .session(session)
                .param("currentPassword", "oldPassword123")
                .param("newPassword", "newSecret123")
                .param("confirmPassword", "differentSecret123"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/profile"))
                .andExpect(flash().attribute("passwordError", "New passwords do not match."));

        verify(userRepository, never()).save(any(User.class));
    }

    // Changing password with valid parameters succeeds and persists BCrypt hash
    @Test
    void ChangePasswordValidSucceeds() throws Exception {
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        MockHttpSession session = new MockHttpSession();
        session.setAttribute("session_user", testUser);

        mockMvc.perform(post("/profile/password")
                .session(session)
                .param("currentPassword", "oldPassword123")
                .param("newPassword", "brandNewPassword123")
                .param("confirmPassword", "brandNewPassword123"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/profile"))
                .andExpect(flash().attribute("passwordSuccess", "Password updated successfully!"));

        verify(userRepository).save(any(User.class));
        assertTrue(passwordEncoder.matches("brandNewPassword123", testUser.getPassword()));
    }
}