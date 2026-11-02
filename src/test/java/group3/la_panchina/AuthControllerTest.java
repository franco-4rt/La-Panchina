package group3.la_panchina;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
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
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.servlet.view.InternalResourceViewResolver;

import group3.la_panchina.controllers.AuthController;
import group3.la_panchina.models.User;
import group3.la_panchina.repositories.UserRepository;

public class AuthControllerTest {
    private MockMvc mockMvc;
    private AuthController authController;

    @Mock
    private UserRepository userRepository;

    @BeforeEach
    void setUp(){
        MockitoAnnotations.openMocks(this);
        authController = new AuthController(userRepository);
        InternalResourceViewResolver viewResolver = new InternalResourceViewResolver();
        viewResolver.setPrefix("/templates/");
        viewResolver.setSuffix(".html");
        mockMvc = MockMvcBuilders.standaloneSetup(authController)
                .setViewResolvers(viewResolver)
                .build();
    }

    // Make sure that the signup page loads up for guests and an empty User object from GET /signup
    @Test
    void ReturnSignupView() throws Exception{
        mockMvc.perform(get("/signup"))
                .andExpect(status().isOk())
                .andExpect(view().name("signup"))
                .andExpect(model().attributeExists("user"));
    }

    // Make sure that /signup redirects already logged-in users to the homepage
    @Test 
    void RedirectHome() throws Exception{
        User user = new User("Alice", "alice@example.com", "secret", "CUSTOMER");
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("session_user", user);

        mockMvc.perform(get("/signup").session(session))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/"));
    }

    // Ensure valid registration hashes password with BCrypt, sets role to CUSTOMER, saves users, and logs the user in
    @Test 
    void Signup_Success()throws Exception{
        when(userRepository.existsByEmailIgnoreCase("alice@example.com")).thenReturn(false);
            when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

            mockMvc.perform(post("/signup")
                    .param("name", "Alice")
                    .param("email", "alice@example.com")
                    .param("password", "secret123")
                    .param("phone", "555-123-4567"))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/"))
                    .andExpect(flash().attributeExists("successMessage"));

            // Use ArgumentCaptor to inspeact the saved User entity (from professor's example)
            ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
            verify(userRepository).save(userCaptor.capture());

            User savedUser = userCaptor.getValue();
            assertEquals("Alice", savedUser.getName());
            assertEquals("alice@example.com", savedUser.getEmail());
            assertEquals("CUSTOMER", savedUser.getRole());
            assertNotEquals("secret123", savedUser.getPassword()); // To make sure the password is not saved as plain text
            assertTrue(new BCryptPasswordEncoder().matches("secret123", savedUser.getPassword()));
    }

    // Ensure signing up with an already exist email fails and does not save to the DB
    @Test 
    void DuplicateEmail_Fails() throws Exception{
        when(userRepository.existsByEmailIgnoreCase("taken@example.com")).thenReturn(true);
            mockMvc.perform(post("/signup")
            .param("name", "Bob")
            .param("email", "taken@example.com")
            .param("password", "secret123"))
            .andExpect(status().isOk())
            .andExpect(view().name("signup"))
            .andExpect(model().hasErrors());

            verify(userRepository, never()).save(any());
    }

    // Ensure submitting invalid/blank fields triggers validation erorrs and does not save
    @Test 
    void signupBlankFields() throws Exception{
        mockMvc.perform(post("/signup")
                .param("name", "")
                .param("email", "invalid-email")
                .param("password", ""))
                .andExpect(status().isOk())
                .andExpect(view().name("signup"))
                .andExpect(model().hasErrors());

        verify(userRepository, never()).save(any());
    }

    // Ensure that Get /login gives the login form for not-logged in users
    @Test
    void loggedOutReturnsLoginView() throws Exception {
        mockMvc.perform(get("/login"))
                .andExpect(status().isOk())
                .andExpect(view().name("login"));
    }

    // Make sure the GET /login redirects already logged-in users to the homepage
    @Test
    void alreadyLoggedInRedirectsHome() throws Exception {
        User user = new User("Alice", "alice@example.com", "secret", "CUSTOMER");
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("session_user", user);
        mockMvc.perform(get("/login").session(session))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/"));
    }

    // Make sure customer login with valid credentials sets session and redirect to homepage
    @Test
    void customerLoggedInSuccessRedirectsHome() throws Exception {
        String hashedPassword = new BCryptPasswordEncoder().encode("secret123");
        User customer = new User("Alice", "alice@example.com", hashedPassword, "CUSTOMER");
        when(userRepository.findByEmailIgnoreCase("alice@example.com")).thenReturn(Optional.of(customer));
        mockMvc.perform(post("/login")
                .param("email", "alice@example.com")
                .param("password", "secret123"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/"))
                .andExpect(request().sessionAttribute("session_user", customer))
                .andExpect(header().string("Cache-Control", "no-cache, no-store, must-revalidate"))
                .andExpect(flash().attributeExists("successMessage"));
    }

    // Ensure admin login with valid credentials sets session and redirects to Admin Dashboard (/admin)
    @Test
    void adminLoggedInSuccessRedirectsToAdmin() throws Exception {
        String hashedPassword = new BCryptPasswordEncoder().encode("admin123");
        User admin = new User("Admin", "admin@lapanchina.com", hashedPassword, "ADMIN");
        when(userRepository.findByEmailIgnoreCase("admin@lapanchina.com")).thenReturn(Optional.of(admin));
        mockMvc.perform(post("/login")
                .param("email", "admin@lapanchina.com")
                .param("password", "admin123"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin"))
                .andExpect(request().sessionAttribute("session_user", admin))
                .andExpect(header().string("Cache-Control", "no-cache, no-store, must-revalidate"))
                .andExpect(flash().attributeExists("successMessage"));
    }

    // Ensure entering an incorrect password displays an error and does not create a session
    @Test
    void loginWrongPasswordShowsError() throws Exception {
        String hashedPassword = new BCryptPasswordEncoder().encode("correctPassword");
        User user = new User("Alice", "alice@example.com", hashedPassword, "CUSTOMER");
        when(userRepository.findByEmailIgnoreCase("alice@example.com")).thenReturn(Optional.of(user));
        mockMvc.perform(post("/login")
                .param("email", "alice@example.com")
                .param("password", "wrongPassword"))
                .andExpect(status().isOk())
                .andExpect(view().name("login"))
                .andExpect(model().attributeExists("errorMessage"))
                .andExpect(request().sessionAttributeDoesNotExist("session_user"));
    }

    // Ensure entering an unregistered email shows an error and dose not log in
    @Test
    void loginUnknownEmailShowsError() throws Exception {
        when(userRepository.findByEmailIgnoreCase("nobody@example.com")).thenReturn(Optional.empty());
        mockMvc.perform(post("/login")
                .param("email", "nobody@example.com")
                .param("password", "anyPassword"))
                .andExpect(status().isOk())
                .andExpect(view().name("login"))
                .andExpect(model().attributeExists("errorMessage"))
                .andExpect(request().sessionAttributeDoesNotExist("session_user"));
    }

    // Ensure logout invalidates the session, sets no-cache headers, and redirects to homepage.
    @Test
    void logoutInvalidatesSessionAndRedirectsHome() throws Exception {
        User user = new User("Alice", "alice@example.com", "secret", "CUSTOMER");
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("session_user", user);
        mockMvc.perform(get("/logout").session(session))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/"))
                .andExpect(header().string("Cache-Control", "no-cache, no-store, must-revalidate"))
                .andExpect(flash().attributeExists("successMessage"));
        assertTrue(session.isInvalid());
    }
}
