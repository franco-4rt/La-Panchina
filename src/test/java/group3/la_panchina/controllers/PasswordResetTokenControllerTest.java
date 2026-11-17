package group3.la_panchina.controllers;

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
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.servlet.view.InternalResourceViewResolver;

import group3.la_panchina.models.PasswordResetToken;
import group3.la_panchina.models.User;
import group3.la_panchina.repositories.PasswordResetTokenRepository;
import group3.la_panchina.repositories.UserRepository;

public class PasswordResetTokenControllerTest {

    private MockMvc mockMvc;
    private PasswordResetTokenController passwordResetTokenController;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordResetTokenRepository tokenRepository;

    private User testUser;
    private BCryptPasswordEncoder passwordEncoder;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        passwordResetTokenController = new PasswordResetTokenController(userRepository, tokenRepository);

        InternalResourceViewResolver viewResolver = new InternalResourceViewResolver();
        viewResolver.setPrefix("/templates/");
        viewResolver.setSuffix(".html");

        mockMvc = MockMvcBuilders.standaloneSetup(passwordResetTokenController)
                .setViewResolvers(viewResolver)
                .build();

        passwordEncoder = new BCryptPasswordEncoder();
        testUser = new User("John Doe", "john@example.com", passwordEncoder.encode("oldPassword123"), "CUSTOMER");
        testUser.setId(1L);
        testUser.setPhone("604-555-0123");
    }

    // Test for /forgot-password displays forgot password form
    @Test
    void ShowForgotPasswordFormReturnsView() throws Exception {
        mockMvc.perform(get("/forgot-password"))
                .andExpect(status().isOk())
                .andExpect(view().name("forgotPassword"));
    }

    // Test for /forgot-password with existing user creates token and demo reset link
    @Test
    void ForgotPasswordFromExistingUserCreatesToken() throws Exception {
        when(userRepository.findByEmailIgnoreCase("john@example.com")).thenReturn(Optional.of(testUser));

        mockMvc.perform(post("/forgot-password")
                .param("email", "john@example.com"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/forgot-password"))
                .andExpect(flash().attributeExists("demoResetLink"))
                .andExpect(flash().attributeExists("successMessage"));

        verify(tokenRepository).deleteByUser(testUser);
        verify(tokenRepository).save(any(PasswordResetToken.class));
    }

        // Test for /forgot-password with unknown email shows generic success message and creates no token
    @Test
    void ForgotPasswordFromNonExistentUserNoTokenCreated() throws Exception {
        when(userRepository.findByEmailIgnoreCase("unknown@example.com")).thenReturn(Optional.empty());

        mockMvc.perform(post("/forgot-password")
                .param("email", "unknown@example.com"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/forgot-password"))
                .andExpect(flash().attributeExists("successMessage"));

        verify(tokenRepository, never()).save(any(PasswordResetToken.class));
    }

    // Test for /reset-password with missing token redirects to /login
    @Test
    void ResetPasswordFormMissingTokenRedirectsToLogin() throws Exception {
        mockMvc.perform(get("/reset-password"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"))
                .andExpect(flash().attribute("errorMessage", "Invalid password reset token."));
    }

    // Test for /reset-password with expired or invalid token redirects to /forgot-password
    @Test
    void ResetPasswordFormExpiredTokenRedirectsToForgotPassword() throws Exception {
        PasswordResetToken expiredToken = new PasswordResetToken("expired-token-123", testUser, -10);
        when(tokenRepository.findByToken("expired-token-123")).thenReturn(Optional.of(expiredToken));

        mockMvc.perform(get("/reset-password").param("token", "expired-token-123"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/forgot-password"))
                .andExpect(flash().attribute("errorMessage",
                        "Your reset link has expired or is invalid. Please request a new one."));
    }

    // Test for /reset-password with valid token gives reset password form
    @Test
    void ResetPasswordFormValidTokenReturnsView() throws Exception {
        PasswordResetToken validToken = new PasswordResetToken("valid-token-123", testUser, 30);
        when(tokenRepository.findByToken("valid-token-123")).thenReturn(Optional.of(validToken));

        mockMvc.perform(get("/reset-password").param("token", "valid-token-123"))
                .andExpect(status().isOk())
                .andExpect(view().name("resetPassword"))
                .andExpect(model().attribute("token", "valid-token-123"));
    }

    // Test for /reset-password with password shorter than 6 characters fails
    @Test
    void ResetPasswordTooShortFails() throws Exception {
        PasswordResetToken validToken = new PasswordResetToken("valid-token-123", testUser, 30);
        when(tokenRepository.findByToken("valid-token-123")).thenReturn(Optional.of(validToken));

        mockMvc.perform(post("/reset-password")
                .param("token", "valid-token-123")
                .param("newPassword", "123")
                .param("confirmPassword", "123"))
                .andExpect(status().isOk())
                .andExpect(view().name("resetPassword"))
                .andExpect(model().attribute("errorMessage", "Password must be at least 6 characters long."));

        verify(userRepository, never()).save(any(User.class));
    }

    // Test for /reset-password with password mismatch fails
    @Test
    void ResetPasswordMismatchFails() throws Exception {
        PasswordResetToken validToken = new PasswordResetToken("valid-token-123", testUser, 30);
        when(tokenRepository.findByToken("valid-token-123")).thenReturn(Optional.of(validToken));

        mockMvc.perform(post("/reset-password")
                .param("token", "valid-token-123")
                .param("newPassword", "newPassword123")
                .param("confirmPassword", "differentPassword123"))
                .andExpect(status().isOk())
                .andExpect(view().name("resetPassword"))
                .andExpect(model().attribute("errorMessage", "Passwords do not match."));

        verify(userRepository, never()).save(any(User.class));
    }

    // Test for /reset-password with valid parameters updates password and deletes single-use token
    @Test
    void ResetPasswordValidSucceeds() throws Exception {
        PasswordResetToken validToken = new PasswordResetToken("valid-token-123", testUser, 30);
        when(tokenRepository.findByToken("valid-token-123")).thenReturn(Optional.of(validToken));

        mockMvc.perform(post("/reset-password")
                .param("token", "valid-token-123")
                .param("newPassword", "newSecretPassword123")
                .param("confirmPassword", "newSecretPassword123"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"))
                .andExpect(flash().attribute("successMessage",
                        "Your password has been reset successfully! Please log in with your new password."));

        verify(userRepository).save(testUser);
        verify(tokenRepository).delete(validToken);
        assertTrue(passwordEncoder.matches("newSecretPassword123", testUser.getPassword()));
    }
}