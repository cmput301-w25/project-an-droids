package com.example.an_droids;

import java.util.ArrayList;

public class Users {
    private String username;
    private String email;
    private String DOB;
    private String location;
    private ArrayList<Mood> moods;

    public Users(String username, String email, String location, String DOB, ArrayList<Mood> moods) {
        this.username = username;
        this.email = email;
        this.location = location;
        this.DOB = DOB;
        this.moods = moods;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getDOB() {
        return DOB;
    }

    public void setDOB(String DOB) {
        this.DOB = DOB;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public ArrayList<Mood> getMoods() {
        return moods;
    }

    public void setMoods(ArrayList<Mood> moods) {
        this.moods = moods;
    }
}
