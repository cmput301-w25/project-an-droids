package com.example.an_droids;

import android.graphics.Bitmap;

import java.io.Serializable;
import java.util.Date;
import java.util.UUID;

public class Mood implements Serializable {
    private String id;
    private Date timestamp;       // Use Date instead of LocalDateTime
    private String trigger;
    private String socialSituation;
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

        private final String emoji;
        private final String colorHex;

        EmotionalState(String emoji, String colorHex) {
            this.emoji = emoji;
            this.colorHex = colorHex;
        }
        public String getEmoji() { return emoji; }
        public String getColorHex() { return colorHex; }
    }

    private EmotionalState emotion;

    // Firestore requires a no-arg constructor
    public Mood() {}

    // Constructors that accept a Date
    public Mood(String emotion, String reason, String trigger, Date timestamp, Bitmap image) {
        this.id = UUID.randomUUID().toString();
        // If timestamp is null, default to now
        this.timestamp = (timestamp != null) ? timestamp : new Date();
        this.emotion = EmotionalState.valueOf(emotion);
        this.reason = reason;
        this.trigger = trigger;
        this.socialSituation = null;
        this.image = image;
    }

    public Mood(String emotion, String reason, String trigger, Date timestamp) {
        this(emotion, reason, trigger, timestamp, null);
    }

    // Getters/Setters
    public String getId() {
        return id;
    }
    public void setId(String id) {
        this.id = id;
    }

    public Date getTimestamp() {
        return timestamp;
    }
    public void setTimestamp(Date timestamp) {
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
