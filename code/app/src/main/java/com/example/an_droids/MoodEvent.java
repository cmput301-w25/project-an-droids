package com.example.an_droids;


/**
 * MoodEvent class represents a single mood event block.
 * It stores the mood text (emotion + emoji) and background color.
 */


public class MoodEvent {
    private String moodText;
    private int moodColor; // Store color as an int
    private int color;

    public MoodEvent(String moodText, int moodColor) {
        this.moodText = moodText;
        this.moodColor = moodColor;
    }

    public String getMoodText() {
        return moodText;
    }

    public void setMoodText(String moodText) {
        this.moodText = moodText;
    }

    public int getMoodColor() {
        return moodColor;
    }

    public void setMoodColor(int moodColor) {
        this.moodColor = moodColor;
    }
    public void setColor(int color) {
        this.color = color;
    }

}

