package group3.la_panchina.controllers;
    
import java.util.List;
import java.util.Optional;
    
import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import group3.la_panchina.models.Review;
import group3.la_panchina.models.User;
import group3.la_panchina.repositories.ReviewRepository;


@WebMvcTest(AdminReviewController.class)
public class AdminReviewControllerTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @MockitoBean
    private ReviewRepository reviewRepo;
    
    // method to create an admin user
    private User createAdmin() {
    return new User(
            "Admin",
            "admin@lapanchina.com",
            "password",
            "ADMIN"
        );
    }

    private User createCustomer() {
    return new User(
            "customer",
            "customer@gmail.com",
            "password",
            "CUSTOMER"
        );
    }
    

    private Review createReview() {
        return new Review(createCustomer(), "Great restaurant!", 5);
    }

    @Test //test whether a guest can access a Admin review page, it should redirect to loggin
    void testShowReviewsWithoutLogin() throws Exception {
        mockMvc.perform(get("/admin/reviews"))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/login"));

        verify(reviewRepo, never()).findAll();
    }

    //Test that a customer can't access a Admin review page, it should redirect 
    // to login(which will redirect to the index)
    @Test
    void testShowReviewsAsNonAdmin() throws Exception {
        mockMvc.perform(get("/admin/reviews")
            .sessionAttr("session_user", createCustomer()))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/login"));

        verify(reviewRepo, never()).findAll();
    }

    //Test that the admin can access the review management page
    //we expect the page to load and the model to have all the reviews
    @Test
    void testShowReviewsAsAdmin() throws Exception {
        List<Review> reviews = List.of(createReview());

        when(reviewRepo.findAll()).thenReturn(reviews);

        mockMvc.perform(get("/admin/reviews")
            .sessionAttr("session_user", createAdmin()))
            .andExpect(status().isOk())
            .andExpect(view().name("admin/review/reviews"))
            .andExpect(model().attribute("reviews", reviews));

        verify(reviewRepo).findAll();
    }

    //test whether any user that is not a admin can enter the delete confirmation page
    //expected to be redirected to login
    @Test
    void testDeleteConfirmationWithoutLogin() throws Exception {
        mockMvc.perform(get("/admin/delete/1"))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/login"));

        verify(reviewRepo, never()).findById(anyLong());
    }

    @Test
    void testDeleteConfirmationAsCustomer() throws Exception {
        mockMvc.perform(get("/admin/delete/1")
            .sessionAttr("session_user", createCustomer()))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/login"));

        verify(reviewRepo, never()).findById(anyLong());
    }

    //Test That an Amdincan see Deletion Confirmation
    //we expect the review to be retrieved and added to the model
    @Test
    void testDeleteConfirmationAsAdmin() throws Exception {
        Review review = createReview();

        when(reviewRepo.findById(1L)).thenReturn(Optional.of(review));

        mockMvc.perform(get("/admin/delete/1")
                .sessionAttr("session_user", createAdmin()))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/review/deleteR"))
                .andExpect(model().attribute("review", review));

        verify(reviewRepo).findById(1L);
    }

    //Test that an error happens if the review doesnt exist with that id
    //Should return an error of not found 
    @Test
    void testDeleteConfirmationReviewNotFound() throws Exception {
        when(reviewRepo.findById(1L)).thenReturn(Optional.empty());

        mockMvc.perform(get("/admin/delete/1")
                .sessionAttr("session_user", createAdmin()))
                .andExpect(status().isNotFound());
    }
    
    //Test what happens if you try to use the delete command as a non-admin
    //we expect a redirection to login and no changes in the database
    @Test
    void testDeleteReviewAsNonAdmin() throws Exception {
        mockMvc.perform(post("/admin/delete/1")
                .sessionAttr("session_user", createCustomer()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));

        verify(reviewRepo, never()).deleteById(anyLong());
    }

    //Test what happens if you try to delete a review as an admin
    //deletes the entry from the database
    @Test
    void testDeleteReviewAsAdmin() throws Exception {
        mockMvc.perform(post("/admin/delete/1")
                .sessionAttr("session_user", createAdmin()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/reviews"));

        verify(reviewRepo).deleteById(1L);
    }
}
