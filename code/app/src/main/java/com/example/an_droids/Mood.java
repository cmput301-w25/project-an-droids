package com.example.an_droids;

import android.graphics.Bitmap;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Represents a Mood event that captures a user's emotional state,
 * trigger and social situation.
 *
 * The emotional state is validated using predefined options (anger, confusion, etc.)
 * but is stored and accessed as a String.
 */
public class Mood implements Serializable {
    private LocalDateTime timestamp;  // Stores the date and time of the mood event
    private String trigger;           // Optional: trigger for the emotion
    private String socialSituation;// Stores the social situation (e.g. "alone", "with one other person")
    public static final String[] SOCIAL_SITUATIONS = {
            "No Selection",
            "Alone",
            "With one other person",
            "With two to several people",
            "With a crowd"
    };
    private String reason;
    private Bitmap image;
    public enum EmotionalState {
        Anger("😠", "#FF6666"),
        Confusion("😕", "#C19A6B"),
        Disgust("🤢", "#90EE90"),
        Fear("😨", "#D8BFD8"),
        Happiness("😃", "#FFFF99"),
        Sadness("😢", "#ADD8E6"),
        Shame("😳", "#FFB6C1"),
        Surprise("😲", "#FFD580");

        private String emoji;
        private String colorHex;

        EmotionalState(String emoji, String colorHex) {
            this.emoji = emoji;
            this.colorHex = colorHex;
        }

        public String getEmoji() {
            return emoji;
        }

        public String getColorHex() {
            return colorHex;
        }
    }
    private EmotionalState emotion;

    public Mood(String emotion, String reason, String trigger, LocalDateTime timestamp, Bitmap image, String socialSituation) {
        this.timestamp = (timestamp != null) ? timestamp : LocalDateTime.now();
        this.emotion = EmotionalState.valueOf(emotion);
        this.reason = reason;
        this.trigger = trigger;
        this.socialSituation = socialSituation;
        this.image = image;
    }

    public Mood(String emotion, String reason, String trigger, LocalDateTime timestamp) {
        this.timestamp = (timestamp != null) ? timestamp : LocalDateTime.now();
        this.emotion = EmotionalState.valueOf(emotion);
        this.reason = reason;
        this.trigger = trigger;
        this.socialSituation = null;
        this.image = null;
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

    public EmotionalState getEmotion() {
        return emotion;
    }

    public void setEmotion(String emotion) {
        this.emotion = EmotionalState.valueOf(emotion);
    }

    public String getEmotionEmoji() {
        return emotion.getEmoji();
    }

    public String getEmotionColorHex() {
        return emotion.getColorHex();
    }

    public String getReason() {
        return reason;
    }
    public void setReason(String reason) {
        this.reason = reason;
    }

    public Bitmap getImage() {
        return image;
    }

    public void setImage(Bitmap image) {
        this.image = image;
    }
}
