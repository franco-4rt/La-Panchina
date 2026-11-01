package group3.la_panchina.repositories;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import group3.la_panchina.models.Review;

public interface ReviewRepository extends JpaRepository<Review, Long> {
    
    List<Review> findByRid(Long rid);
    List<Review> findByUser(String user);
}
