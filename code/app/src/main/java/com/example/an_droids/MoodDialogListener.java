package com.example.an_droids;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

/**
 * Interface for handling mood-related actions in dialogs.
 */
public interface MoodDialogListener {

    /**
     * Adds a new mood entry.
     *
     * @param mood The {@link Mood} object representing the mood to be added.
     */
    void AddMood(Mood mood);

    /**
     * Edits an existing mood entry.
     *
     * @param mood The {@link Mood} object representing the mood to be edited.
     */
    void EditMood(Mood mood);

}
