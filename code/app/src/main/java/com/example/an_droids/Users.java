package com.example.an_droids;

import java.util.List;

public class Users {
    private String username;
    private String email;
    private String DOB;
    private String location;
    private List<String> followers;  // List of user IDs
    private List<String> following;

    public Users() {}

    public Users(String username, String email, String location, String DOB) {
        this.username = username;
        this.email = email;
        this.location = location;
        this.DOB = DOB;
    }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getDOB() { return DOB; }
    public void setDOB(String DOB) { this.DOB = DOB; }
    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }
    public List<String> getFollowers() { return followers; }
    public void setFollowers(List<String> followers) { this.followers = followers; }
    public List<String> getFollowing() { return following; }
    public void setFollowing(List<String> following) { this.following = following; }
}
