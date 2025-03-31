package com.example.an_droids;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.*;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.*;

import android.util.Log;
import android.view.View;
import android.widget.DatePicker;

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
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Objects;

/**
 * Full end-to-end test from login → signup → main activity.
 * Also tests bottom nav & profile interactions inside MainActivity.
 */
@RunWith(AndroidJUnit4.class)
public class MainActivityFlowTest {

    @Rule
    public ActivityScenarioRule<LoginActivity> activityRule =
            new ActivityScenarioRule<>(LoginActivity.class);

    private void pickDate() {
        onView(withId(R.id.dobInput)).perform(click());
        onView(withClassName(Matchers.equalTo(DatePicker.class.getName())))
                .perform(new ViewAction() {
                    @Override
                    public Matcher<View> getConstraints() {
                        return isAssignableFrom(DatePicker.class);
                    }

                    @Override
                    public String getDescription() {
                        return "Set DOB date";
                    }

                    @Override
                    public void perform(UiController uiController, View view) {
                        ((DatePicker) view).updateDate(2001, 2, 15); // March 15, 2001
                    }
                });
        onView(withText("OK")).perform(click());
    }

    @BeforeClass
    public static void setUpFirebase() {
        FirebaseAuth.getInstance().useEmulator("10.0.2.2", 9099);
        FirebaseFirestore.getInstance().useEmulator("10.0.2.2", 8080);
    }

    /**
     * Full user flow:
     * 1. From Login screen, click "Signup"
     * 2. Fill and submit form
     * 3. Arrive at MainActivity
     * 4. Navigate bottom nav, click profile icon
     */
    @Test
    public void testFullSignupAndMainNavigation() throws InterruptedException {
        String dynamicEmail = "espresso" + System.currentTimeMillis() + "@test.com";

        // Click "Sign up" link
        onView(withId(R.id.signupLink)).perform(click());

        // Fill Signup form
        onView(withId(R.id.usernameInput)).perform(typeText("testuser"), closeSoftKeyboard());
        onView(withId(R.id.emailInput)).perform(typeText(dynamicEmail), closeSoftKeyboard());
        onView(withId(R.id.passwordInput)).perform(typeText("TestPass123"), closeSoftKeyboard());
        onView(withId(R.id.reenterPasswordInput)).perform(typeText("TestPass123"), closeSoftKeyboard());
        pickDate();
        onView(withId(R.id.signupButton)).perform(click());

        // Wait for Firebase sign up and MainActivity to load
        Thread.sleep(2000);

        // Assert MainActivity container is shown
        onView(withId(R.id.main_container)).check(matches(isDisplayed()));

        // Bottom Nav → Feed
        onView(withId(R.id.nav_feed)).perform(click());
        onView(withId(R.id.main_fragment_container)).check(matches(isDisplayed()));

        // Bottom Nav → Search
        onView(withId(R.id.nav_search)).perform(click());
        onView(withId(R.id.main_fragment_container)).check(matches(isDisplayed()));

        // Bottom Nav → Moods
        onView(withId(R.id.nav_moods)).perform(click());
        onView(withId(R.id.main_fragment_container)).check(matches(isDisplayed()));

        // Bottom Nav → Requests
        onView(withId(R.id.nav_requests)).perform(click());
        onView(withId(R.id.main_fragment_container)).check(matches(isDisplayed()));

        // Click profile icon (toolbar)
        onView(withId(R.id.profile_icon)).perform(click());
        onView(withId(R.id.main_fragment_container)).check(matches(isDisplayed()));

        // Bottom Nav → Profile (launch ViewUserProfile)
        onView(withId(R.id.nav_profile)).perform(click());
        onView(withId(R.id.backButton)).perform(click());
        Thread.sleep(1500);
        onView(withId(R.id.main_container)).check(matches(isDisplayed()));
    }

    @AfterClass
    public static void tearDownFirestore() {
        try {
            URL url = new URL("http://10.0.2.2:8080/emulator/v1/projects/an_droids/databases/(default)/documents");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("DELETE");
            int res = conn.getResponseCode();
            Log.i("Firestore Cleanup", "Code: " + res);
            conn.disconnect();
        } catch (IOException e) {
            Log.e("Firestore Cleanup", Objects.requireNonNull(e.getMessage()));
        }
    }

    @AfterClass
    public static void tearDownAuth() {
        try {
            URL authUrl = new URL("http://10.0.2.2:9099/emulator/v1/projects/an-droids-194ef/accounts");
            HttpURLConnection authConnection = (HttpURLConnection) authUrl.openConnection();
            authConnection.setRequestMethod("DELETE");
            int authResponse = authConnection.getResponseCode();
            Log.i("Auth TearDown", "Code: " + authResponse);
            authConnection.disconnect();
        } catch (IOException e) {
            Log.e("Auth Cleanup", Objects.requireNonNull(e.getMessage()));
        }
    }
}