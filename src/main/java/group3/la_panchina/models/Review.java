package group3.la_panchina.models;

import java.time.LocalDateTime;

import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

public class Review {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long rid; //review id

    @Email
    private String user; //At the moment it is simply an email but will be later integrated with the Login/user

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

    };

    public Long getRid() {
        return rid;
    }

    public String getUser() {
            return user;
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

    public String setUser(String user){
        return this.user = user;
    }

    public Integer setRating(Integer rating){
        return this.rating = rating;
    }

    public String setComment(String comment){
        return this.comment = comment;
    }


}
