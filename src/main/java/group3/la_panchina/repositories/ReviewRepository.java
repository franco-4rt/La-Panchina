package group3.la_panchina.repositories;
import org.springframework.data.jpa.repository.JpaRepository;

import group3.la_panchina.models.Review;
import group3.la_panchina.models.User;

public interface ReviewRepository extends JpaRepository<Review, Long> {
    
    Review findByAppUser(User user);
}
