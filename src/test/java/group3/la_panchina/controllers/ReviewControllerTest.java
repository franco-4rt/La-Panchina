package group3.la_panchina.controllers;

import java.util.List;

import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
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

@WebMvcTest(ReviewController.class)
class ReviewControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ReviewRepository reviewRepo;

    // Creates a customer to simulate a logged-in user.
    private User createCustomer() {
        return new User("Test Customer", "customer@email.com", "password", "CUSTOMER");
    }

    // Creates a sample review associated with a given customer.
    private Review createReview(User user) {
        return new Review(user, "Great food!", 5);
    }

    //Tests whether guests can view the public reviews page.
    // we expect that the page loads, reviews are displayed, and hasReview is false.
    @Test
    void testSeeReviewsAsGuest() throws Exception {
        List<Review> reviews = List.of();

        when(reviewRepo.findAll()).thenReturn(reviews);
        mockMvc.perform(get("/reviews"))
            .andExpect(status().isOk())
            .andExpect(view().name("customers/review/reviews"))
            .andExpect(model().attribute("reviews", reviews))
            .andExpect(model().attribute("hasReview", false));

        verify(reviewRepo).findAll();
        verify(reviewRepo, never()).findByAppUser(any());
    }

    // Checks that a logged-in customer with an existing review is recognized.
    // we expect the reviews page to load and hasReview is true.
    @Test
    void testSeeReviewsWithExistingReview() throws Exception {
        User customer = createCustomer();
        Review review = createReview(customer);

        when(reviewRepo.findAll()).thenReturn(List.of(review));
        when(reviewRepo.findByAppUser(customer)).thenReturn(review);
        mockMvc.perform(get("/reviews")
        .sessionAttr("session_user", customer))
        .andExpect(status().isOk())
        .andExpect(view().name("customers/review/reviews"))
        .andExpect(model().attribute("hasReview", true));

        verify(reviewRepo).findByAppUser(customer);
    }

    // Checks that guests cannot access the create-review form
    // we expect the guest to be redirected to the login page.
    @Test
    void testCreateReviewFormWithoutLogin() throws Exception {
        mockMvc.perform(get("/reviews/create"))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/login"));
    }

    // Checks that a logged-in customer without a review can open the create form.
    // We expect the create-review page loads with a new Review object.
    @Test
    void testCreateReviewFormAsCustomer() throws Exception {
        User customer = createCustomer();

        when(reviewRepo.findByAppUser(customer)).thenReturn(null);
        mockMvc.perform(get("/reviews/create")
            .sessionAttr("session_user", customer))
            .andExpect(status().isOk())
            .andExpect(view().name("customers/review/createR"))
            .andExpect(model().attributeExists("reviewS"));

        verify(reviewRepo).findByAppUser(customer);
    }

    // Checks that customers who already submitted a review cannot create another.
    // We expect the customer to be redirected to the reviews page if they try.
    @Test
    void testCreateReviewFormWithExistingReview() throws Exception {
        User customer = createCustomer();
        Review review = createReview(customer);

        when(reviewRepo.findByAppUser(customer)).thenReturn(review);
        mockMvc.perform(get("/reviews/create")
            .sessionAttr("session_user", customer))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/reviews"));
    }

    // Checks that guests cannot submit a review.
    // We expect that the guest is redirected to the login page and nothing is saved in the db.
    @Test
    void testSubmitReviewWithoutLogin() throws Exception {
        mockMvc.perform(post("/reviews/create")
            .param("rating", "5")
            .param("comment", "Great food!"))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/login"));

        verify(reviewRepo, never()).save(any());
    }

    // Checks that a logged-in customer can successfully submit a valid review.
    // We expect the review is associated with the customer, saved, and 
    // they are redirected to reviews page.
    @Test
    void testSubmitReviewSuccessfully() throws Exception {
        User customer = createCustomer();

        when(reviewRepo.findByAppUser(customer)).thenReturn(null);
        mockMvc.perform(post("/reviews/create")
            .sessionAttr("session_user", customer)
            .param("rating", "5")
            .param("comment", "Great food!"))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/reviews"));

        verify(reviewRepo).save(argThat(review ->
            review.getAppUser() == customer
            && Integer.valueOf(5).equals(review.getRating())
            && "Great food!".equals(review.getComment())
        ));
    }

    // Checks that submitting a rating above the allowed maximum is rejected.
    // We expect validation errors are returned and the review is not saved.
    @Test
    void testSubmitReviewWithInvalidRating() throws Exception {
        User customer = createCustomer();

        when(reviewRepo.findByAppUser(customer)).thenReturn(null);
        mockMvc.perform(post("/reviews/create")
            .sessionAttr("session_user", customer)
            .param("rating", "6")
            .param("comment", "Great food!"))
            .andExpect(status().isOk())
            .andExpect(view().name("customers/review/createR"))
            .andExpect(model().attributeHasFieldErrors("reviewS", "rating"));

        verify(reviewRepo, never()).save(any());
    }

    // Checks that guests cannot access the edit-review form.
    // We expect the guest to be redirected to login.
    @Test
    void testEditReviewFormWithoutLogin() throws Exception {
        mockMvc.perform(get("/reviews/edit"))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/login"));
    }

    // Checks that customers without an existing review cannot access the edit form.
    // We expect the customer to be redirected to the reviews page.
    @Test
    void testEditReviewFormWithoutExistingReview() throws Exception {
        User customer = createCustomer();

        when(reviewRepo.findByAppUser(customer)).thenReturn(null);
        mockMvc.perform(get("/reviews/edit")
            .sessionAttr("session_user", customer))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/reviews"));
    }

    // Checks that a customer with an existing review can access the edit form.
    // We expect the edit-review page to load with the review model attribute.
    @Test
    void testEditReviewFormSuccessfully() throws Exception {
        User customer = createCustomer();
        Review review = createReview(customer);

        when(reviewRepo.findByAppUser(customer)).thenReturn(review);

        mockMvc.perform(get("/reviews/edit")
            .sessionAttr("session_user", customer))
            .andExpect(status().isOk())
            .andExpect(view().name("customers/review/updateR"))
            .andExpect(model().attributeExists("reviewS"));
    }

    // Checks that guests cannot submit an edit to a review.
    // We expect the guest is redirected to login and nothing is saved in the db.
    @Test
    void testSubmitEditWithoutLogin() throws Exception {
        mockMvc.perform(post("/reviews/edit")
            .param("rating", "4")
            .param("comment", "Updated comment"))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/login"));

        verify(reviewRepo, never()).save(any());
    }

    // Checks that customers without an existing review cannot submit an edit.
    // We expect the customer to be redirected to the reviews page and nothing is saved in the db.
    @Test
    void testSubmitEditWithoutExistingReview() throws Exception {
        User customer = createCustomer();

        when(reviewRepo.findByAppUser(customer)).thenReturn(null);
        mockMvc.perform(post("/reviews/edit")
            .sessionAttr("session_user", customer)
            .param("rating", "4")
            .param("comment", "Updated comment"))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/reviews"));
        verify(reviewRepo, never()).save(any());
    }

    // Checks that a customer can successfully update their existing review.
    //We expect the existing review is updated and saved instead of creating a new one.
    @Test
    void testSubmitEditSuccessfully() throws Exception {
        User customer = createCustomer();
        Review existingReview = createReview(customer);

        when(reviewRepo.findByAppUser(customer)).thenReturn(existingReview);
        mockMvc.perform(post("/reviews/edit")
            .sessionAttr("session_user", customer)
            .param("rating", "4")
            .param("comment", "Updated comment"))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/reviews"));
        verify(reviewRepo).save(argThat(review ->
            review == existingReview
            && Integer.valueOf(4).equals(review.getRating())
            && "Updated comment".equals(review.getComment())
        ));
    }

    // Checks that submitting an invalid rating while editing is rejected.
    // We expect the validation errors are returned and the existing review is not saved in the db.
    @Test
    void testSubmitEditWithInvalidRating() throws Exception {
        User customer = createCustomer();
        Review existingReview = createReview(customer);

        when(reviewRepo.findByAppUser(customer)).thenReturn(existingReview);
        mockMvc.perform(post("/reviews/edit")
            .sessionAttr("session_user", customer)
            .param("rating", "0")
            .param("comment", "Updated comment"))
            .andExpect(status().isOk())
            .andExpect(view().name("customers/review/updateR"))
            .andExpect(model().attributeHasFieldErrors("reviewS", "rating"));
        verify(reviewRepo, never()).save(any());
    }
}
