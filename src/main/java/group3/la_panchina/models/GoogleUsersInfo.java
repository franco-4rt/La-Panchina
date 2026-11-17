package group3.la_panchina.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class GoogleUsersInfo {
    private String gid;      // Google's unique id
    private String name;     // Full name
    private String email;    // Email address
    private String picture;  // Avatar URL
    private Boolean email_verified;

    public GoogleUsersInfo() {}

    public String getGid() { return gid; }
    public void setGid(String gid) { this.gid = gid; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPicture() { return picture; }
    public void setPicture(String picture) { this.picture = picture; }

    public Boolean getEmail_verified() { return email_verified; }
    public void setEmail_verified(Boolean email_verified) { this.email_verified = email_verified; }
}