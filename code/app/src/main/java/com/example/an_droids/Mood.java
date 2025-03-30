package com.example.an_droids;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;

import com.google.firebase.firestore.Blob;
import com.google.firebase.firestore.Exclude;
import java.io.ByteArrayOutputStream;
import java.io.Serializable;
import java.util.Date;
import java.util.UUID;

public class Mood implements Serializable {
    private String id;
    private Date timestamp;
    private String socialSituation;
    private String reason;
    @Exclude
    private transient Bitmap image;
    private Blob imageBlob;
    // NEW: add ownerId field that will be saved to Firestore
    private String ownerId;

    public static final String[] SOCIAL_SITUATIONS = {
            "No Selection", "Alone", "With one other person", "With two to several people", "With a crowd"
    };
    private double latitude;
    private double longitude;
    private String address;
    private Blob voiceNoteBlob;
    @Exclude
    private transient Uri voiceNoteUri;

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

        public String getEmoji() {
            return emoji;
        }

        public String getColorHex() {
            return colorHex;
        }
    }

    public enum Privacy {
        PRIVATE,
        PUBLIC
    }

    private EmotionalState emotion;
    private Privacy privacy;

    public Mood() {}

    // Updated constructor: you can later set the ownerId via setter
    public Mood(String emotion, String reason, Date timestamp, Bitmap image, String socialSituation, Privacy privacy) {
        this.id = UUID.randomUUID().toString();
        this.timestamp = (timestamp != null) ? timestamp : new Date();
        this.emotion = EmotionalState.valueOf(emotion);
        this.reason = reason;
        this.socialSituation = socialSituation;
        this.privacy = privacy;
        setImage(image);
    }

    public Mood(String emotion, String reason, Date timestamp, String socialSituation, Privacy privacy) {
        this(emotion, reason, timestamp, null, socialSituation, privacy);
    }

    // Getters and setters for all fields ...

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

    public Privacy getPrivacy() {
        return privacy;
    }

    public void setPrivacy(Privacy privacy) {
        this.privacy = privacy;
    }

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

    public Blob getImageBlob() {
        return imageBlob;
    }

    public void setImageBlob(Blob imageBlob) {
        this.imageBlob = imageBlob;
        if (imageBlob != null) {
            byte[] bytes = imageBlob.toBytes();
            image = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
        } else {
            image = null;
        }
    }

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getOwnerId() {
        return ownerId;
    }

    public void setOwnerId(String ownerId) {
        this.ownerId = ownerId;
    }

    public Blob getVoiceNoteBlob() {
        return voiceNoteBlob;
    }

    public void setVoiceNoteBlob(Blob voiceNoteBlob) {
        this.voiceNoteBlob = voiceNoteBlob;
    }

    @Exclude
    public Uri getVoiceNoteUri() {
        return voiceNoteUri;
    }

    @Exclude
    public void setVoiceNoteUri(Uri uri) {
        this.voiceNoteUri = uri;
    }

    @Exclude
    public String getSocialSituationEmojiLabel() {
        switch (socialSituation) {
            case "Alone":
                return "🧍 Alone";
            case "With one other person":
                return "🧑‍🤝‍🧑 With one other";
            case "With two to several people":
                return "👨‍👩‍👧 Several people";
            case "With a crowd":
                return "👥 Crowd";
            default:
                return "❔ No selection";
        }
    }
}
