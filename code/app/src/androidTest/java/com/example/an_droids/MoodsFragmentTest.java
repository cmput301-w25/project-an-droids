package com.example.an_droids;

import static androidx.test.espresso.Espresso.onData;
import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.*;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.RootMatchers.isDialog;
import static androidx.test.espresso.matcher.ViewMatchers.*;

import android.util.Log;
import android.view.View;
import android.widget.DatePicker;
import android.widget.TimePicker;

import androidx.test.espresso.UiController;
import androidx.test.espresso.ViewAction;
import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import org.hamcrest.Matcher;
import org.hamcrest.Matchers;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Objects;

/**
 * Espresso test for MoodsFragment.
 * Covers add, edit, delete, and details functionality of mood entries.
 */
@RunWith(AndroidJUnit4.class)
public class MoodsFragmentTest {

    @Rule
    public ActivityScenarioRule<LoginActivity> activityRule =
            new ActivityScenarioRule<>(LoginActivity.class);

    @BeforeClass
    public static void setUpFirebase() {
        FirebaseAuth.getInstance().useEmulator("10.0.2.2", 9099);
        FirebaseFirestore.getInstance().useEmulator("10.0.2.2", 8080);
    }

    private void pickDOB() {
        onView(withId(R.id.dobInput)).perform(click());
        onView(withClassName(Matchers.equalTo(DatePicker.class.getName())))
                .perform(new ViewAction() {
                    @Override
                    public Matcher<View> getConstraints() {
                        return isAssignableFrom(DatePicker.class);
                    }

                    @Override
                    public String getDescription() {
                        return "Pick DOB";
                    }

                    @Override
                    public void perform(UiController uiController, View view) {
                        ((DatePicker) view).updateDate(2002, 1, 15);
                    }
                });
        onView(withText("OK")).perform(click());
    }

    private void pickEditDate() {
        onView(withId(R.id.dateEditText)).perform(click());
        onView(withClassName(Matchers.equalTo(DatePicker.class.getName())))
                .perform(new ViewAction() {
                    @Override
                    public Matcher<View> getConstraints() {
                        return isAssignableFrom(DatePicker.class);
                    }

                    @Override
                    public String getDescription() {
                        return "Pick Date";
                    }

                    @Override
                    public void perform(UiController uiController, View view) {
                        ((DatePicker) view).updateDate(2023, 0, 1);
                    }
                });
        onView(withText("OK")).perform(click());
    }

    private void pickEditTime() {
        onView(withId(R.id.timeEditText)).perform(click());
        onView(withClassName(Matchers.equalTo(TimePicker.class.getName())))
                .perform(new ViewAction() {
                    @Override
                    public Matcher<View> getConstraints() {
                        return isAssignableFrom(TimePicker.class);
                    }

                    @Override
                    public String getDescription() {
                        return "Pick Time";
                    }

                    @Override
                    public void perform(UiController uiController, View view) {
                        ((TimePicker) view).setHour(10);
                        ((TimePicker) view).setMinute(30);
                    }
                });
        onView(withText("OK")).perform(click());
    }

    /**
     * Full test covering adding, editing, deleting and viewing details of a mood.
     */
    @Test
    public void testMoodCrudFlow() throws InterruptedException {
        String email = "mood" + System.currentTimeMillis() + "@test.com";

        onView(withId(R.id.signupLink)).perform(click());
        onView(withId(R.id.usernameInput)).perform(typeText("moodUser"), closeSoftKeyboard());
        onView(withId(R.id.emailInput)).perform(typeText(email), closeSoftKeyboard());
        onView(withId(R.id.passwordInput)).perform(typeText("MoodTest123"), closeSoftKeyboard());
        onView(withId(R.id.reenterPasswordInput)).perform(typeText("MoodTest123"), closeSoftKeyboard());
        pickDOB();
        onView(withId(R.id.signupButton)).perform(click());

        Thread.sleep(2000);

        onView(withId(R.id.nav_moods)).perform(click());
        onView(withId(R.id.addMoodButton)).perform(click());

        onView(withId(R.id.reasonEditText)).inRoot(isDialog()).perform(typeText("Feeling awesome"), closeSoftKeyboard());
        onView(withId(R.id.playVoiceButton)).inRoot(isDialog()).check(matches(isDisplayed()));
        onView(withText("Add")).inRoot(isDialog()).perform(click());

        Thread.sleep(1500);

        onData(Matchers.anything()).inAdapterView(withId(R.id.moodList)).atPosition(0).perform(click());

        onView(withId(R.id.reasonEditText)).inRoot(isDialog()).perform(replaceText("Feeling awesome edited"), closeSoftKeyboard());
        pickEditDate();
        pickEditTime();
        onView(withText("Save")).inRoot(isDialog()).perform(click());

        Thread.sleep(1000);

        onData(Matchers.anything()).inAdapterView(withId(R.id.moodList)).atPosition(0).perform(longClick());
        onView(withText("Yes")).inRoot(isDialog()).perform(click());

        Thread.sleep(1000);

        onView(withId(R.id.addMoodButton)).perform(click());
        onView(withId(R.id.reasonEditText)).inRoot(isDialog()).perform(typeText("For details test"), closeSoftKeyboard());
        onView(withText("Add")).inRoot(isDialog()).perform(click());

        Thread.sleep(1000);

        onData(Matchers.anything()).inAdapterView(withId(R.id.moodList)).atPosition(0)
                .onChildView(withId(R.id.infoButton)).perform(click());

        onView(withText("Mood Details")).inRoot(isDialog()).check(matches(isDisplayed()));
        onView(withText("Close")).perform(click());
    }

    @AfterClass
    public static void clearFirestore() {
        try {
            URL url = new URL("http://10.0.2.2:8080/emulator/v1/projects/an_droids/databases/(default)/documents");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("DELETE");
            conn.getResponseCode();
            conn.disconnect();
        } catch (IOException e) {
            Log.e("Firestore Cleanup", Objects.requireNonNull(e.getMessage()));
        }
    }

    @AfterClass
    public static void clearAuth() {
        try {
            URL authUrl = new URL("http://10.0.2.2:9099/emulator/v1/projects/an-droids-194ef/accounts");
            HttpURLConnection authConnection = (HttpURLConnection) authUrl.openConnection();
            authConnection.setRequestMethod("DELETE");
            authConnection.getResponseCode();
            authConnection.disconnect();
        } catch (IOException e) {
            Log.e("Auth Cleanup", Objects.requireNonNull(e.getMessage()));
        }
    }
}