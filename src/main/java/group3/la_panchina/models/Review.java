package group3.la_panchina.models;

import java.time.LocalDateTime;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

@Entity 
@Table (name="review")
public class Review { 

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long rid; //review id

    @OneToOne //One review can only be associated to One user
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User appUser;

    @Min(value = 1)
    @Max(value = 5)
    private Integer rating;

    @Size(max = 500, message = "Characters must not exceed 500 characters")
    private String comment;


    private LocalDateTime createdAt;
    private LocalDateTime updatedAt; 

    @PrePersist // runs before being first inserted in the database
    protected void onCreate(){
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate //runs before updating an entity in the database
    protected void onUpdate(){
        updatedAt = LocalDateTime.now();
    }

    public Review(){
    }

    public Review(User appUser, String comment, Integer rating) {
        this.appUser = appUser;
        this.comment = comment;
        this.rating = rating;
    }

    public Long getRid() {
        return rid;
    }

    public User getAppUser() {
            return appUser;
    }

    public String getComment(){
        return comment;
    }

    public Integer getRating(){
        return rating;
    }

    public LocalDateTime getCreatedAt(){
        return createdAt;
    }

    public LocalDateTime getUpdatedAt(){
        return updatedAt;
    }

    public void setRId(Long rid) {
        this.rid = rid;
    }

    public void setUser(User appUser){
        this.appUser = appUser;
    }

    public void setRating(Integer rating){
        this.rating = rating;
    }

    public void setComment(String comment){
        this.comment = comment;
    }


}
