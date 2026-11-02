package group3.la_panchina;

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

import group3.la_panchina.controllers.AdminController;
import group3.la_panchina.models.User;
import group3.la_panchina.repositories.UserRepository;

public class AdminControllerTest {

    private MockMvc mockMvc;
    private AdminController adminController;

    @Mock
    private UserRepository userRepository;

    // Set up MockMvc with our controller and mocked repository before each test
    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        adminController = new AdminController(userRepository);

        // Prevents Spring standalone MockMvc from throwing a "Circular view path" error
        InternalResourceViewResolver viewResolver = new InternalResourceViewResolver();
        viewResolver.setPrefix("/templates/");
        viewResolver.setSuffix(".html");

        mockMvc = MockMvcBuilders.standaloneSetup(adminController)
                .setViewResolvers(viewResolver)
                .build();
    }

    // Ensure that an Admin user can view the dashboard with the list of users and no-cache headers
    @Test
    void AdminDashboardAsAdminReturnsAdminViewWithUsers() throws Exception {
        User admin = new User("Admin", "admin@lapanchina.com", "secret", "ADMIN");
        admin.setId(1L);

        User customer = new User("Alice", "alice@example.com", "secret", "CUSTOMER");
        customer.setId(2L);

        List<User> userList = List.of(admin, customer);
        when(userRepository.findAll()).thenReturn(userList);

        MockHttpSession session = new MockHttpSession();
        session.setAttribute("session_user", admin);

        mockMvc.perform(get("/admin").session(session))
                .andExpect(status().isOk())
                .andExpect(view().name("admin"))
                .andExpect(model().attributeExists("users"))
                .andExpect(model().attribute("users", userList))
                .andExpect(model().attributeExists("adminUser"))
                .andExpect(model().attribute("adminUser", admin))
                .andExpect(header().string("Cache-Control", "no-cache, no-store, must-revalidate"));

        verify(userRepository).findAll();
    }

    // Ensure that a logged-in Customer cannot access the admin dashboard and is redirected to /login
    @Test
    void AdminDashboardAsCustomerRedirectsToLogin() throws Exception {
        User customer = new User("Alice", "alice@example.com", "secret", "CUSTOMER");
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("session_user", customer);

        mockMvc.perform(get("/admin").session(session))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));

        // Sensitive user list must never be avaliable for regular customers
        verify(userRepository, never()).findAll();
    }

    // Ensure that an unauthenticated guest cannot access the admin dashboard and is redirected to /login
    @Test
    void AdminDashboardAsGuestRedirectsToLogin() throws Exception {
        mockMvc.perform(get("/admin"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));

        // Sensitive user list must never be avaliable for guests
        verify(userRepository, never()).findAll();
    }
}