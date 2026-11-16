package group3.la_panchina.controllers;

import static org.hamcrest.Matchers.containsString;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import group3.la_panchina.models.User;


@WebMvcTest(AboutController.class)
public class AboutControllerTest {

    @Autowired
    private MockMvc mockMvc;


    // Test that a guest can indeed access to the Find Us page 
    @Test
    void testAboutPageAccessibleByGuest() throws Exception {

        mockMvc.perform(get("/about-us"))
            .andExpect(status().isOk())
            .andExpect(view().name("about-us"));
    }


    // Test that a Logged in user can access the Find Us page.
    @Test
    void testAboutPageAccessibleByCustomer() throws Exception {

        MockHttpSession session = new MockHttpSession();

        User customer = new User(
            "customer",
            "customer@gmail.com",
            "password",
            "CUSTOMER"
        );

        session.setAttribute("session_user", customer);

        mockMvc.perform(get("/about-us").session(session))
            .andExpect(status().isOk())
            .andExpect(view().name("about-us"));
    }


    // Test that an admin can access the Find Us page.
    @Test
    void testAboutPageAccessibleByAdmin() throws Exception {

        MockHttpSession session = new MockHttpSession();

        User admin = new User(
            "Admin",
            "admin@lapanchina.com",
            "password",
            "ADMIN"
        );

        session.setAttribute("session_user", admin);

        mockMvc.perform(get("/about-us").session(session))
            .andExpect(status().isOk())
            .andExpect(view().name("about-us"));
    }


    // Test that the About Us page contains the restaurant's social media links.
    //A manual test has been done to verify that they do indeed send you to the correct page.
    @Test
    void testAboutPageContainsSocialMediaLinks() throws Exception {

        mockMvc.perform(get("/about-us"))
            .andExpect(status().isOk())
            .andExpect(content().string(
                    containsString("instagram.com/lapanchina_cucina")
            ))
            .andExpect(content().string(
                containsString("facebook.com/p/La-Panchina-Cucina-Italiana")
            ))
            .andExpect(content().string(
                    containsString("mail.google.com")
            ))
            .andExpect(content().string(
                    containsString("tel:+529516353912")
            ));
    }


    // Test that the About Us page contains an embedded Google Maps iframe.
    @Test
    void testAboutPageContainsGoogleMapsIframe() throws Exception {

        mockMvc.perform(get("/about-us"))
            .andExpect(status().isOk())
            .andExpect(content().string(
                    containsString("<iframe")
            ))
            .andExpect(content().string(
                    containsString("google.com/maps/embed")
            ))
            .andExpect(content().string(
                    containsString("La%20Panchina%20Cucina")
            ));
    }

}
