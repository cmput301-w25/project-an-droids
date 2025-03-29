package com.example.an_droids;

public class MoodLookup {
    private String moodId;
    private String userId;

    public MoodLookup() {}

    public MoodLookup(String moodId, String userId) {
        this.moodId = moodId;
        this.userId = userId;
    }

    public String getMoodId() {
        return moodId;
    }

    public void setMoodId(String moodId) {
        this.moodId = moodId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }
}
