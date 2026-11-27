package group3.la_panchina.models;

import java.io.Serializable;
import java.time.LocalDateTime;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import group3.la_panchina.models.User;

@Entity 
@Table (name = "users")
public class User implements Serializable{
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue (strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank (message = "Name is required")
    @Size (max = 100, message = "Name must be under 100 charactes")
    @Column (nullable = false, length = 100)
    private String name;

    @NotBlank(message = "Email is required")
    @Email (message = "Please enter a valid email address")
    @Column (unique = true, nullable = false, length = 120)
    private String email;

    @NotBlank (message = "Password is required")
    @Column (nullable = false, length = 255)
    private String password;

    @Column (nullable = false, length = 20)
    private String role = "CUSTOMER";

    @NotBlank (message = "Phone number is required")
    @Pattern (regexp = "^(\\+?\\d{1,2}\\s?)?\\(?\\d{3}\\)?[\\s.-]?\\d{3}[\\s.-]?\\d{4}$", 
             message = "Please enter a valid 10-digit phone number (e.g. 555-123-4567)")
    @Column (nullable = false, length = 20)
    private String phone;

    @Column (length = 10)
    private String contactPreference = "Email";

    private LocalDateTime lastLoginAt;
    private LocalDateTime previousLoginAt;
    public static final int MAX_FAILED_ATTEMPTS = 5;
    public static final int LOCK_DURATION_MINUTES = 15;

    private Integer failedLoginAttempts = 0;

    private LocalDateTime lockUntil;

    private Boolean suspended = false;

    public boolean isSuspended(){
        return Boolean.TRUE.equals(suspended);
    }

    public void setSuspended(Boolean suspended) {
        this.suspended = suspended;
    }

    public boolean accountLocked(){
        return lockUntil != null && LocalDateTime.now().isBefore(lockUntil);
    }

    public void registerFailedLogin(){
        // Reset the failed counter in the lock is expired
        if(lockUntil != null && !LocalDateTime.now().isBefore(lockUntil)){
            this.failedLoginAttempts = 0;
            this.lockUntil = null;
        }
        int current = (this.failedLoginAttempts == null) ? 0 : this.failedLoginAttempts;
        this.failedLoginAttempts = current + 1;

        if(this.failedLoginAttempts >= MAX_FAILED_ATTEMPTS){
            this.lockUntil = LocalDateTime.now().plusMinutes(LOCK_DURATION_MINUTES);
        }
    }

    public void resetFailedLogins(){
        this.failedLoginAttempts = 0;
        this.lockUntil = null;
    }

    public int getFailedLoginAttempts(){
        return (failedLoginAttempts == null) ? 0 : failedLoginAttempts;
    }

    public void setFailedLoginAttempts(Integer failedLoginAttempts){
        this.failedLoginAttempts = failedLoginAttempts;
    }

    public LocalDateTime getLockUntil(){
        return lockUntil;
    }

    public void setLockUntil(LocalDateTime lockUntil){
        this.lockUntil = lockUntil;
    }

    public LocalDateTime getLastLoginAt() {
        return lastLoginAt;
    }

    public void setLastLoginAt(LocalDateTime lastLoginAt) {
        this.lastLoginAt = lastLoginAt;
    }

    public void recordLogin(){
        this.previousLoginAt = this.lastLoginAt;
        this.lastLoginAt = LocalDateTime.now();
    }
    public LocalDateTime getPreviousLoginAt() {
        return previousLoginAt;
    }

    public void setPreviousLoginAt(LocalDateTime previousLoginAt) {
        this.previousLoginAt = previousLoginAt;
    }

    public User(){}

    public User(String name, String email, String password, String role){
        this.name = name;
        this.email = email;
        this.password = password;
        this.role = role;
    }

    //Getters & Setters

    public static long getSerialversionuid() {
        return serialVersionUID;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getContactPreference() {
        return contactPreference;
    }

    public void setContactPreference(String contactPreference) {
        this.contactPreference = contactPreference;
    }    
}
