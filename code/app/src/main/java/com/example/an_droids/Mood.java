package com.example.an_droids;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;

import com.google.firebase.firestore.Blob;
import com.google.firebase.firestore.Exclude;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Serializable;
import java.net.HttpURLConnection;
import java.net.URL;
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

    // NEW: add weather field to store current weather information
    private String weather;

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

    // Updated constructor: you can later set the ownerId and weather via setters
    public Mood(String emotion, String reason, Date timestamp, Bitmap image, String socialSituation, Privacy privacy) {
        this.id = UUID.randomUUID().toString();
        this.timestamp = (timestamp != null) ? timestamp : new Date();
        this.emotion = EmotionalState.valueOf(emotion);
        this.reason = reason;
        this.socialSituation = socialSituation;
        this.privacy = privacy;
        setImage(image);
        // Optionally update weather here if location is already set.
        // updateWeather();
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
        // Update weather based on new location.
        updateWeather();
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
        // Update weather based on new location.
        updateWeather();
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

    // NEW: Weather field getters and setters
    public String getWeather() {
        return weather;
    }

    public void setWeather(String weather) {
        this.weather = weather;
    }

    /**
     * Extracts the current weather for the Mood's location using the free Open-Meteo API.
     * This method runs on a background thread and updates the weather field based on the response.
     */
    public void updateWeather() {
        // Check if the location is valid.
        if (latitude == 0 && longitude == 0) {
            this.weather = "Unknown";
            return;
        }

        new Thread(() -> {
            HttpURLConnection connection = null;
            try {
                // Open-Meteo free API endpoint; no API key required.
                String urlString = "https://api.open-meteo.com/v1/forecast?latitude="
                        + latitude + "&longitude=" + longitude + "&current_weather=true";
                URL url = new URL(urlString);
                connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.setConnectTimeout(5000);
                connection.setReadTimeout(5000);

                int responseCode = connection.getResponseCode();
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    InputStream is = connection.getInputStream();
                    BufferedReader reader = new BufferedReader(new InputStreamReader(is));
                    StringBuilder response = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        response.append(line);
                    }
                    reader.close();

                    JSONObject jsonObject = new JSONObject(response.toString());
                    if (jsonObject.has("current_weather")) {
                        JSONObject currentWeather = jsonObject.getJSONObject("current_weather");
                        // Extract weathercode and temperature.
                        int weatherCode = currentWeather.getInt("weathercode");
                        double temperature = currentWeather.getDouble("temperature");

                        String weatherDescription = getWeatherDescription(weatherCode);
                        // For example: "Clear sky, 15°C"
                        this.weather = weatherDescription + ", " + temperature + "°C";
                    } else {
                        this.weather = "Unknown";
                    }
                } else {
                    this.weather = "Error: " + responseCode;
                }
            } catch (Exception e) {
                e.printStackTrace();
                this.weather = "Error";
            } finally {
                if (connection != null) {
                    connection.disconnect();
                }
            }
        }).start();
    }

    /**
     * Helper method to convert Open-Meteo weather codes into human-readable descriptions.
     */
    private String getWeatherDescription(int code) {
        switch (code) {
            case 0:
                return "Clear sky";
            case 1:
                return "Mainly clear";
            case 2:
                return "Partly cloudy";
            case 3:
                return "Overcast";
            case 45:
                return "Fog";
            case 48:
                return "Depositing rime fog";
            case 51:
                return "Light drizzle";
            case 53:
                return "Moderate drizzle";
            case 55:
                return "Dense drizzle";
            case 56:
                return "Light freezing drizzle";
            case 57:
                return "Dense freezing drizzle";
            case 61:
                return "Slight rain";
            case 63:
                return "Moderate rain";
            case 65:
                return "Heavy rain";
            case 66:
                return "Light freezing rain";
            case 67:
                return "Heavy freezing rain";
            case 71:
                return "Slight snow fall";
            case 73:
                return "Moderate snow fall";
            case 75:
                return "Heavy snow fall";
            case 77:
                return "Snow grains";
            case 80:
                return "Slight rain showers";
            case 81:
                return "Moderate rain showers";
            case 82:
                return "Violent rain showers";
            case 85:
                return "Slight snow showers";
            case 86:
                return "Heavy snow showers";
            case 95:
                return "Thunderstorm";
            case 96:
                return "Thunderstorm with slight hail";
            case 99:
                return "Thunderstorm with heavy hail";
            default:
                return "Unknown weather";
        }
    }
}
