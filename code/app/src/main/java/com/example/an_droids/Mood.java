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
    private String emotionalState;    // Stores the emotional state (e.g."happy", "sad")
    private String trigger;           // Optional: trigger for the emotion
    private String socialSituation;   // Stores the social situation (e.g. "alone", "with one other person")

    /**
     * Constructor for a Mood event with emotional state and social situation.
     * The timestamp is automatically set to the current time.
     * The trigger is set to null since it's optional.
     * @param emotionalState  The emotion felt (e.g., "happy", "sad").
     * @param socialSituation The social context of the emotion.
     * @throws IllegalArgumentException if the emotional state is not a valid option.
     */
    public Mood(String emotionalState, String socialSituation) {
        this.timestamp = LocalDateTime.now(); // Automatically captures the current time

        // Convert String to Enum, then back to String to ensure valid input
        if (isValidEmotion(emotionalState)) {
            this.emotionalState = emotionalState.toUpperCase();
        } else {
            throw new IllegalArgumentException("Invalid emotional state: " + emotionalState);
        }

        this.trigger = null; // Since trigger is optional, set it to null
        this.socialSituation = socialSituation;
    }

    /**
     * Constructor for a Mood event with emotional state, trigger, and social situation.
     * The timestamp is automatically set to the current time.
     * @param emotionalState  The emotion felt (e.g., "happy", "sad").
     * @param trigger         The reason for the emotion (optional).
     * @param socialSituation The social context of the emotion.
     * @throws IllegalArgumentException if the emotional state is not a valid option.
     */
    public Mood(String emotionalState, String trigger, String socialSituation) {
        this.timestamp = LocalDateTime.now(); // Automatically captures the current time
        // Convert String to Enum, then back to String to ensure valid input
        if (isValidEmotion(emotionalState)) {
            this.emotionalState = emotionalState.toUpperCase();
        } else {
            throw new IllegalArgumentException("Invalid emotional state: " + emotionalState);
        }
        this.trigger = trigger;
        this.socialSituation = socialSituation;
    }

    /**
     * Validates if the provided emotional state is a valid value from the predefined set of emotions.
     *
     * @param emotionalState The emotional state to validate.
     * @return true if the emotional state is valid, false otherwise.
     */
    private boolean isValidEmotion(String emotionalState) {
        try {
            Emotional_States.valueOf(emotionalState.toUpperCase()); // Checks if it's a valid emotion
            return true;
        } catch (IllegalArgumentException e) {
            return false;  // Invalid emotional state
        }
    }

    // Getter and Setter methods

    /**
     * Returns the timestamp of the mood event.
     * @return The timestamp as a LocalDateTime object.
     */
    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    /**
     * Sets a custom timestamp for the mood event.
     * @param timestamp The new timestamp to set.
     */
    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    /**
     * Returns the emotional state of the mood event.
     * @return The emotional state as a String.
     */
    public String getEmotionalState() {
        return emotionalState; // Return as String
    }

    /**
     * Updates the emotional state of the mood event.
     * @param emotionalState The new emotional state.
     * @throws IllegalArgumentException if the emotional state is not valid.
     */
    public void setEmotionalState(String emotionalState) {
        // Validate that the chosen state is a valid option
        if (isValidEmotion(emotionalState)) {
            this.emotionalState = emotionalState.toUpperCase(); // Validates and sets the emotional state
        } else {
            throw new IllegalArgumentException("Invalid emotional state: " + emotionalState);
        }
    }

    /**
     * Returns the trigger for the emotional state.
     * @return The trigger as a String, or null if not set.
     */
    public String getTrigger() {
        return trigger;
    }

    /**
     * Updates the trigger for the emotional state.
     * @param trigger The new trigger reason.
     */
    public void setTrigger(String trigger) {
        this.trigger = trigger;
    }

    /**
     * Returns the social situation of the mood event.
     * @return The social situation as a String, or null if not set.
     */
    public String getSocialSituation() {
        return socialSituation;
    }

    /**
     * Updates the social situation for the mood event.
     * @param socialSituation The new social context.
     */
    public void setSocialSituation(String socialSituation) {
        this.socialSituation = socialSituation;
    }

}
