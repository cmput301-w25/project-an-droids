package com.example.an_droids;

import static androidx.test.espresso.Espresso.*;
import static androidx.test.espresso.action.ViewActions.*;
import static androidx.test.espresso.action.ViewActions.closeSoftKeyboard;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.*;

import static org.hamcrest.CoreMatchers.allOf;

import android.util.Log;
import android.view.View;
import android.widget.DatePicker;

import androidx.test.espresso.UiController;
import androidx.test.espresso.ViewAction;
import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import org.hamcrest.Matcher;
import org.hamcrest.Matchers;
import org.junit.*;
import org.junit.runner.RunWith;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.*;

/**
 * Instrumented test for {@link SearchFragment} and {@link ViewUserProfile}.
 *
 * ✅ Covers:
 * - Searching for a user
 * - Navigating to profile
 * - Following
 * - Viewing moods
 * - Viewing followers & following list
 */
@RunWith(AndroidJUnit4.class)
@LargeTest
public class SearchAndViewProfileTest {

    /**
     * Launches the SignupActivity before each test.
     */
    @Rule
    public ActivityScenarioRule<SignupActivity> activityRule =
            new ActivityScenarioRule<>(SignupActivity.class);

    /**
     * Sets Firebase Auth and Firestore to use emulator before test run.
     */
    @BeforeClass
    public static void setUp() {
        String localhost = "10.0.2.2";
        FirebaseFirestore.getInstance().useEmulator(localhost, 8080);
        FirebaseAuth.getInstance().useEmulator(localhost, 9099);
    }

    /**
     * Selects a static date (2000-01-01) on DatePicker.
     */
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
                        return "Set DOB";
                    }

                    @Override
                    public void perform(UiController uiController, View view) {
                        ((DatePicker) view).updateDate(2000, 0, 1);
                    }
                });
        onView(withText("OK")).perform(click());
    }

    /**
     * Tests full flow of searching for a user, visiting their profile,
     * sending follow request, viewing their moods and follower/following lists.
     *
     * @throws InterruptedException to allow async Firebase operations to complete
     */
    @Test
    public void testSearchFollowAndViewUserProfile() throws InterruptedException {
        String email = "mood" + System.currentTimeMillis() + "@test.com";

        onView(withId(R.id.usernameInput)).perform(typeText("moodUser"), closeSoftKeyboard());
        onView(withId(R.id.emailInput)).perform(typeText(email), closeSoftKeyboard());
        onView(withId(R.id.passwordInput)).perform(typeText("MoodTest123"), closeSoftKeyboard());
        onView(withId(R.id.reenterPasswordInput)).perform(typeText("MoodTest123"), closeSoftKeyboard());
        pickDate();
        onView(withId(R.id.signupButton)).perform(click());
        Thread.sleep(1500);

        // Seed a target user
        String targetUid = "target_user_id";
        Map<String, Object> user = new HashMap<>();
        user.put("username", "target_user");
        user.put("email", "target@gmail.com");
        user.put("dob", new Date());

        FirebaseFirestore.getInstance()
                .collection("Users")
                .document(targetUid)
                .set(user);

        // Add moods to target user
        for (int i = 0; i < 3; i++) {
            Mood mood = new Mood("Happiness", "Mood reason " + i, new Date(), "Alone", Mood.Privacy.PUBLIC);
            FirebaseFirestore.getInstance()
                    .collection("Users")
                    .document(targetUid)
                    .collection("Moods")
                    .add(mood);
        }

        Thread.sleep(2000); // Allow seed

        // Navigate to search tab
        onView(withId(R.id.bottom_navigation)).perform(click());
        onView(withId(R.id.nav_search)).perform(click());

        // Search
        onView(withId(R.id.searchEditText)).perform(typeText("target_user"), closeSoftKeyboard());
        Thread.sleep(2000);

        // Tap user
        onView(allOf(
                withText("target_user"),
                isDescendantOfA(withId(R.id.searchResultsListView)),
                isDisplayed()
        )).perform(click());
        Thread.sleep(2000);

        // Follow
        onView(withId(R.id.followButton)).check(matches(withText("Follow")));
        onView(withId(R.id.followButton)).perform(click());
        Thread.sleep(1500);
        onView(withId(R.id.followButton)).check(matches(withText("Requested")));

        // View moods
        onView(withId(R.id.viewMoodsButton)).perform(click());
        Thread.sleep(1500);
        onView(withId(R.id.moodListView)).check(matches(isDisplayed()));

        // Open followers list
        onView(withId(R.id.followersText)).perform(click());
        Thread.sleep(1000);

        // Open following list
        onView(withId(R.id.followingText)).perform(click());
        Thread.sleep(1000);
    }

    /**
     * Cleans Firestore data for emulator project "an_droids".
     */
    @AfterClass
    public static void tearDownFirestore() {
        try {
            String projectId = "an_droids";
            URL url = new URL("http://10.0.2.2:8080/emulator/v1/projects/" + projectId + "/databases/(default)/documents");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("DELETE");
            int code = conn.getResponseCode();
            Log.i("TearDownFirestore", "Firestore cleared: " + code);
            conn.disconnect();
        } catch (IOException e) {
            Log.e("TearDownFirestore", Objects.requireNonNull(e.getMessage()));
        }
    }

    /**
     * Cleans Auth data for emulator project "an-droids-194ef".
     */
    @AfterClass
    public static void tearDownAuth() {
        try {
            String projectId = "an-droids-194ef";
            URL authUrl = new URL("http://10.0.2.2:9099/emulator/v1/projects/" + projectId + "/accounts");
            HttpURLConnection authConn = (HttpURLConnection) authUrl.openConnection();
            authConn.setRequestMethod("DELETE");
            int code = authConn.getResponseCode();
            Log.i("TearDownAuth", "Auth accounts cleared: " + code);
            authConn.disconnect();
        } catch (IOException e) {
            Log.e("TearDownAuth", Objects.requireNonNull(e.getMessage()));
        }
    }
}