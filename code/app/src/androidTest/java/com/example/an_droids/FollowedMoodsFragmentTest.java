package com.example.an_droids;

import static androidx.test.espresso.Espresso.*;
import static androidx.test.espresso.action.ViewActions.*;
import static androidx.test.espresso.action.ViewActions.closeSoftKeyboard;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.RootMatchers.isDialog;
import static androidx.test.espresso.matcher.ViewMatchers.*;

import android.util.Log;
import android.view.View;
import android.widget.DatePicker;
import android.widget.EditText;

import androidx.test.espresso.UiController;
import androidx.test.espresso.ViewAction;
import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.*;

import org.hamcrest.Matcher;
import org.hamcrest.Matchers;
import org.junit.*;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.*;

import org.junit.runner.RunWith;

/**
 * Espresso UI test for FollowedMoodsFragment.
 * Tests navigation, filter, viewing mood details, and comment features.
 */
@RunWith(AndroidJUnit4.class)
public class FollowedMoodsFragmentTest {

    @Rule
    public ActivityScenarioRule<LoginActivity> activityRule =
            new ActivityScenarioRule<>(LoginActivity.class);

    private static FirebaseFirestore db;

    @BeforeClass
    public static void setupFirebaseEmulatorAndSeed() throws InterruptedException {
        FirebaseAuth.getInstance().useEmulator("10.0.2.2", 9099);
        db = FirebaseFirestore.getInstance();
        db.useEmulator("10.0.2.2", 8080);

        String uid = "followedTestUser";

        Map<String, Object> followedUser = new HashMap<>();
        followedUser.put("username", "followed_user");
        followedUser.put("email", "fuser@example.com");

        Mood mood = new Mood(
                Mood.EmotionalState.Happiness.name(),
                "Public mood for testing",
                new Date(),
                null,
                "Alone",
                Mood.Privacy.PUBLIC
        );
        mood.setOwnerId(uid);
        mood.setId(UUID.randomUUID().toString());

        db.collection("Users").document(uid).set(followedUser);
        db.collection("Users").document(uid).collection("Moods").document(mood.getId()).set(mood);

        Thread.sleep(1500);
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
                        return "Set DOB";
                    }

                    @Override
                    public void perform(UiController uiController, View view) {
                        ((DatePicker) view).updateDate(2001, 5, 10);
                    }
                });
        onView(withText("OK")).perform(click());
    }

    /**
     * Full test of FollowedMoodsFragment from signup to viewing and commenting on moods.
     */
    @Test
    public void testFollowedMoodFlow() throws InterruptedException {
        String dynamicEmail = "espresso" + System.currentTimeMillis() + "@test.com";

        // Click "Sign up" link
        onView(withId(R.id.signupLink)).perform(click());

        // Fill Signup form
        onView(withId(R.id.usernameInput)).perform(typeText("testuser"), closeSoftKeyboard());
        onView(withId(R.id.emailInput)).perform(typeText(dynamicEmail), closeSoftKeyboard());
        onView(withId(R.id.passwordInput)).perform(typeText("TestPass123"), closeSoftKeyboard());
        onView(withId(R.id.reenterPasswordInput)).perform(typeText("TestPass123"), closeSoftKeyboard());
        pickDOB();
        onView(withId(R.id.signupButton)).perform(click());

        Thread.sleep(2500);

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            String currentUserId = user.getUid();
            db.collection("Users").document(currentUserId).update("following", Arrays.asList("followedTestUser"));
        }

        Thread.sleep(1500);

        onView(withId(R.id.nav_feed)).perform(click());

        Thread.sleep(2000);

        onView(withId(R.id.moodFilterButton)).perform(click());
        onView(withText("By Reason")).inRoot(isDialog()).perform(click());
        onView(isAssignableFrom(EditText.class)).inRoot(isDialog()).perform(typeText("testing"), closeSoftKeyboard());
        onView(withText("Filter")).inRoot(isDialog()).perform(click());

        Thread.sleep(1500);

        onData(Matchers.anything()).inAdapterView(withId(R.id.followedMoodsListView))
                .atPosition(0)
                .onChildView(withId(R.id.infoButton))
                .perform(click());

        Thread.sleep(1000);

        onView(withId(R.id.moodCommentButton)).inRoot(isDialog()).perform(click());
        onView(isAssignableFrom(EditText.class)).inRoot(isDialog()).perform(typeText("Great mood!"), closeSoftKeyboard());
        onView(withText("Submit")).inRoot(isDialog()).perform(click());

        Thread.sleep(1000);

        onView(withId(R.id.moodViewCommentsButton)).inRoot(isDialog()).perform(click());
        onView(withText("Comments")).inRoot(isDialog()).check(matches(isDisplayed()));
        onView(withText("Close")).inRoot(isDialog()).perform(click());
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
            HttpURLConnection conn = (HttpURLConnection) authUrl.openConnection();
            conn.setRequestMethod("DELETE");
            conn.getResponseCode();
            conn.disconnect();
        } catch (IOException e) {
            Log.e("Auth Cleanup", Objects.requireNonNull(e.getMessage()));
        }
    }
}
