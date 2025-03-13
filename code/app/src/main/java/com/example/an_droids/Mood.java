package com.example.an_droids;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import com.google.firebase.firestore.Blob;
import com.google.firebase.firestore.Exclude;
import java.io.ByteArrayOutputStream;
import java.io.Serializable;
import java.util.Date;
import java.util.UUID;

public class Mood implements Serializable {
    private String id;
    private Date timestamp;
    private String trigger;
    private String socialSituation;
    private String reason;
    @Exclude
    private transient Bitmap image;
    private Blob imageBlob;
    public static final String[] SOCIAL_SITUATIONS = {
            "No Selection", "Alone", "With one other person", "With two to several people", "With a crowd"
    };
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
    public Mood() {}
    public Mood(String emotion, String reason, String trigger, Date timestamp, Bitmap image, String socialSituation) {
        this.id = UUID.randomUUID().toString();
        this.timestamp = (timestamp != null) ? timestamp : new Date();
        this.emotion = EmotionalState.valueOf(emotion);
        this.reason = reason;
        this.trigger = trigger;
        this.socialSituation = socialSituation;
        setImage(image);
    }
    public Mood(String emotion, String reason, String trigger, Date timestamp, String socialSituation) {
        this(emotion, reason, trigger, timestamp, null, socialSituation);
    }
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public Date getTimestamp() { return timestamp; }
    public void setTimestamp(Date timestamp) { this.timestamp = timestamp; }
    public String getTrigger() { return trigger; }
    public void setTrigger(String trigger) { this.trigger = trigger; }
    public String getSocialSituation() { return socialSituation; }
    public void setSocialSituation(String socialSituation) { this.socialSituation = socialSituation; }
    public EmotionalState getEmotion() { return emotion; }
    public void setEmotion(String emotion) { this.emotion = EmotionalState.valueOf(emotion); }
    public String getEmotionEmoji() { return emotion.getEmoji(); }
    public String getEmotionColorHex() { return emotion.getColorHex(); }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
    @Exclude
    public Bitmap getImage() {
        if (image == null && imageBlob != null) {
            byte[] bytes = imageBlob.toBytes();
            image = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
        }
        return image;
    }
    @Exclude
    public void setImage(Bitmap image) {
        this.image = image;
        if (image != null) {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            image.compress(Bitmap.CompressFormat.JPEG, 80, baos);
            byte[] bytes = baos.toByteArray();
            this.imageBlob = Blob.fromBytes(bytes);
        } else {
            this.imageBlob = null;
        }
    }
    public Blob getImageBlob() { return imageBlob; }
    public void setImageBlob(Blob imageBlob) {
        this.imageBlob = imageBlob;
        if (imageBlob != null) {
            byte[] bytes = imageBlob.toBytes();
            image = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
        } else {
            image = null;
        }
    }
}
