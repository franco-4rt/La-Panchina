package group3.la_panchina.controllers;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.servlet.view.InternalResourceViewResolver;

import group3.la_panchina.repositories.UserRepository;

public class GoogleOAuthControllerTest {

    private MockMvc mockMvc;
    private GoogleOAuthController googleOAuthController;

    @Mock
    private UserRepository userRepository;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        googleOAuthController = new GoogleOAuthController(userRepository);

        ReflectionTestUtils.setField(googleOAuthController, "clientId", "test-client-id");
        ReflectionTestUtils.setField(googleOAuthController, "redirectUri", "http://localhost:8080/login/oauth2/code/google");

        InternalResourceViewResolver viewResolver = new InternalResourceViewResolver();
        viewResolver.setPrefix("/templates/");
        viewResolver.setSuffix(".html");

        mockMvc = MockMvcBuilders.standaloneSetup(googleOAuthController)
                .setViewResolvers(viewResolver)
                .build();
    }

    // Ensures that clicking the Google button initiates an HTTP redirect to Google
    @Test
    void RedirectToGoogleBuildsCorrectAuthUrl() throws Exception {
        mockMvc.perform(get("/oauth2/authorization/google"))
                .andExpect(status().is3xxRedirection());
    }
}