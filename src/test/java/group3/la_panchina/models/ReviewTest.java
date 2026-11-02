package group3.la_panchina.models;

import java.time.LocalDateTime;
import java.util.Set;

import org.junit.jupiter.api.AfterAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;


class ReviewTest {

    private static ValidatorFactory factory;
    private static Validator validator;

    @BeforeAll
    static void setUp() {
        factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @AfterAll
    static void tearDown() {
        factory.close();
    }

    // Creates an actual customer to associate with test reviews.
    private User createCustomer() {
        return new User("Test Customer",
        "customer@email.com",
        "password",
        "CUSTOMER");
    }

    // Checks that the Review constructor correctly initializes its attributes correctly.
    @Test
    void testReviewConstructorAndGetters() {
        User user = createCustomer();
        Review review = new Review(user, "Excellent food!", 5);
        assertEquals(user, review.getAppUser());
        assertEquals("Excellent food!", review.getComment());
        assertEquals(5, review.getRating());
    }

    // Checks that the Review setters correctly update the object's attributes.
    @Test
    void testReviewSetters() {
        Review review = new Review();
        User user = createCustomer();
        review.setUser(user);
        review.setComment("Updated comment");
        review.setRating(4);
        review.setRId(10L);
        assertEquals(user, review.getAppUser());
        assertEquals("Updated comment", review.getComment());
        assertEquals(4, review.getRating());
        assertEquals(10L, review.getRid());
    }

    // Checks that the minimum allowed rating of 1 passes validation.
    // There should be no validation errors
    @Test
    void testMinimumValidRating() {
        Review review = new Review(createCustomer(), "Good!", 1);
        Set<ConstraintViolation<Review>> violations = validator.validate(review);
        assertTrue(violations.isEmpty());
    }

    // Checks that the maximum allowed rating of 5 passes validation.
    // There should be no validation errors
    @Test
    void testMaximumValidRating() {
        Review review = new Review(createCustomer(), "Excellent!", 5);

        Set<ConstraintViolation<Review>> violations = validator.validate(review);
        assertTrue(violations.isEmpty());
    }

    // Checks that a rating below the minimum value is rejected.
    // There should be a validation errors
    @Test
    void testRatingBelowMinimum() {
        Review review = new Review(createCustomer(), "Bad!", 0);
        assertFalse(validator.validate(review).isEmpty());
    }

    // Checks that a rating above the maximum value is rejected.
    // There should be a validation errors
    @Test
    void testRatingAboveMaximum() {
        Review review = new Review(createCustomer(), "Excellent!", 6);
        assertFalse(validator.validate(review).isEmpty());
    }

    // Checks that a comment containing exactly 500 characters is accepted.
    // the comment passes the @Size(max = 500) constraint.
    @Test
    void testCommentMaximumLength() {
        String comment = "a".repeat(500);
        Review review = new Review(createCustomer(), comment, 5);
        assertTrue(validator.validate(review).isEmpty());
    }

    // Checks that a comment containing more than 500 characters is rejected.
    // There should be a validation errors
    @Test
    void testCommentExceedingMaximumLength() {
        String comment = "a".repeat(501);
        Review review = new Review(createCustomer(), comment, 5);
        assertFalse(validator.validate(review).isEmpty());
    }

    // Checks that the creation lifecycle callback initializes both timestamps.
    @Test
    void testOnCreate() {
        Review review = new Review();
        review.onCreate(); //Call it manually as to 
        //simultaes what happen before inserting in a database

        assertNotNull(review.getCreatedAt());
        assertNotNull(review.getUpdatedAt());
    }

    // Checks that the update lifecycle callback changes updatedAt without changing createdAt.
    @Test
    void testOnUpdate() {
        Review review = new Review();
        review.onCreate(); //simultaes what happen before inserting in a database

        LocalDateTime originalCreatedAt = review.getCreatedAt();
        LocalDateTime originalUpdatedAt = review.getUpdatedAt();
        review.onUpdate(); //Call it manually as to 
        //simulates what happens when updating th object in the db

        assertEquals(originalCreatedAt, review.getCreatedAt());
        assertNotNull(review.getUpdatedAt());
        assertFalse(review.getUpdatedAt().isBefore(originalUpdatedAt));
    }
}
