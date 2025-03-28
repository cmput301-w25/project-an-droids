package com.example.an_droids;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class Users {
    private String username;
    private String email;
    private Date dob;
    private List<String> followers;
    private List<String> following;

    public Users() {
        this.followers = new ArrayList<>();
        this.following = new ArrayList<>();
    }

    public Users(String username, String email, Date dob) {
        this.username = username;
        this.email = email;
        this.dob = dob;
        this.followers = new ArrayList<>();
        this.following = new ArrayList<>();
    }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public Date getDob() { return dob; }
    public void setDob(Date dob) { this.dob = dob; }
    public List<String> getFollowers() { return followers; }
    public void setFollowers(List<String> followers) { this.followers = followers; }
    public List<String> getFollowing() { return following; }
    public void setFollowing(List<String> following) { this.following = following; }
}