package com.example.an_droids;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

public interface MoodDialogListener {
    void AddMood(Mood mood);
    void EditMood(Mood mood);

    @NonNull
    Fragment getItem(int position);

    int getCount();
}
