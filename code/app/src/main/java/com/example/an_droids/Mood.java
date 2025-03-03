package com.example.an_droids;

import java.time.LocalDateTime;

/**
 * Represents a Mood event that captures a user's emotional state,
 * trigger and social situation.
 *
 * The emotional state is validated using predefined options (anger, confusion, etc.)
 * but is stored and accessed as a String.
 */
public class Mood {
    private LocalDateTime timestamp;  // Stores the date and time of the mood event
    private String trigger;           // Optional: trigger for the emotion
    private String socialSituation;   // Stores the social situation (e.g. "alone", "with one other person")
    private enum emotionalState {
        ANGER,
        CONFUSION,
        DISGUST,
        FEAR,
        HAPPINESS,
        SADNESS,
        SHAME,
        SURPRISE
    }
    private emotionalState emotion;

    public Mood(LocalDateTime timestamp, String trigger, String socialSituation, String emotion) {
        this.timestamp = timestamp;
        this.trigger = trigger;
        this.socialSituation = socialSituation;
        this.emotion = emotionalState.valueOf(emotion.toUpperCase());
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public String getTrigger() {
        return trigger;
    }

    public void setTrigger(String trigger) {
        this.trigger = trigger;
    }

    public String getSocialSituation() {
        return socialSituation;
    }

    public void setSocialSituation(String socialSituation) {
        this.socialSituation = socialSituation;
    }

    public emotionalState getEmotion() {
        return emotion;
    }

    public void setEmotion(emotionalState emotion) {
        this.emotion = emotion;
    }
}