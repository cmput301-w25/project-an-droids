package com.example.an_droids;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.closeSoftKeyboard;
import static androidx.test.espresso.action.ViewActions.typeText;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.*;

import android.util.Log;
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

@RunWith(AndroidJUnit4.class)
public class MapActivityTest {

    @Rule
    public ActivityScenarioRule<SignupActivity> activityRule =
            new ActivityScenarioRule<>(SignupActivity.class);

    @BeforeClass
    public static void setUp() {
        FirebaseFirestore.getInstance().useEmulator("10.0.2.2", 8080);
        FirebaseAuth.getInstance().useEmulator("10.0.2.2", 9099);
    }

    private void pickDate() {
        onView(withId(R.id.dobInput)).perform(click());
        onView(withClassName(Matchers.equalTo(DatePicker.class.getName())))
                .perform(new ViewAction() {
                    @Override
                    public Matcher<android.view.View> getConstraints() {
                        return isAssignableFrom(DatePicker.class);
                    }

                    @Override
                    public String getDescription() {
                        return "Set date on DatePicker";
                    }

                    @Override
                    public void perform(UiController uiController, android.view.View view) {
                        ((DatePicker) view).updateDate(2000, 1, 1);
                    }
                });
        onView(withText("OK")).perform(click());
    }

    @Test
    public void testMapFiltersAndMarkers() throws InterruptedException {
        String email = "mapuser" + System.currentTimeMillis() + "@gmail.com";

        // Sign up
        onView(withId(R.id.usernameInput)).perform(typeText("mapuser"), closeSoftKeyboard());
        onView(withId(R.id.emailInput)).perform(typeText(email), closeSoftKeyboard());
        onView(withId(R.id.passwordInput)).perform(typeText("ValidPass123"), closeSoftKeyboard());
        onView(withId(R.id.reenterPasswordInput)).perform(typeText("ValidPass123"), closeSoftKeyboard());
        pickDate();
        onView(withId(R.id.signupButton)).perform(click());
        Thread.sleep(2000);
        onView(withId(R.id.main_container)).check(matches(isDisplayed()));

        // Open Mood History
        onView(withId(R.id.bottom_navigation)).perform(click());
        onView(withId(R.id.nav_moods)).perform(click());
        Thread.sleep(1000);

        // Add Mood
        onView(withId(R.id.addMoodButton)).perform(click());
        onView(withId(R.id.reasonEditText)).perform(typeText("Map testing"), closeSoftKeyboard());
        onView(withText("Add")).perform(click());
        Thread.sleep(1500);

        // Open Map
        onView(withId(R.id.mapButton)).perform(click());
        Thread.sleep(3000);

        // Select "My Mood Events"
        onView(withId(R.id.filterButton)).perform(click());
        onView(withText("My Mood Events")).perform(click());
        Thread.sleep(3000);

        // Check marker title exists
        onView(withContentDescription("Google Map")).check(matches(isDisplayed()));

        // Filter by distance
        onView(withId(R.id.filterButton)).perform(click());
        onView(withText("Mood Events within 5 km")).perform(click());
        Thread.sleep(3000);

        // Back button works
        onView(withId(R.id.backButton)).perform(click());
        onView(withId(R.id.main_fragment_container)).check(matches(isDisplayed()));
    }

    @AfterClass
    public static void tearDownFirebase() {
        deleteFirebaseData("an_droids");
        deleteFirebaseData("an-droids-194ef");
    }

    private static void deleteFirebaseData(String projectId) {
        try {
            URL firestoreUrl = new URL("http://10.0.2.2:8080/emulator/v1/projects/" + projectId + "/databases/(default)/documents");
            HttpURLConnection conn = (HttpURLConnection) firestoreUrl.openConnection();
            conn.setRequestMethod("DELETE");
            int res1 = conn.getResponseCode();
            Log.i("TearDown Firestore", "Code: " + res1);
            conn.disconnect();

            URL authUrl = new URL("http://10.0.2.2:9099/emulator/v1/projects/" + projectId + "/accounts");
            HttpURLConnection conn2 = (HttpURLConnection) authUrl.openConnection();
            conn2.setRequestMethod("DELETE");
            int res2 = conn2.getResponseCode();
            Log.i("TearDown Auth", "Code: " + res2);
            conn2.disconnect();

        } catch (MalformedURLException e) {
            Log.e("TearDown URL", e.getMessage());
        } catch (IOException e) {
            Log.e("TearDown IO", e.getMessage());
        }
    }
}
