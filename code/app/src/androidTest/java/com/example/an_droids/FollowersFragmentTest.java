package com.example.an_droids;

import static androidx.test.espresso.Espresso.*;
import static androidx.test.espresso.action.ViewActions.*;
import static androidx.test.espresso.action.ViewActions.closeSoftKeyboard;
import static androidx.test.espresso.assertion.ViewAssertions.doesNotExist;
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
 * Espresso UI test for FollowersFragment.
 * Verifies follow request appears, can be accepted, and follower can be removed.
 */
@RunWith(AndroidJUnit4.class)
public class FollowersFragmentTest {

    @Rule
    public ActivityScenarioRule<LoginActivity> activityRule =
            new ActivityScenarioRule<>(LoginActivity.class);

    private static FirebaseFirestore db;

    @BeforeClass
    public static void setupFirebaseAndSeedRequest() throws InterruptedException {
        FirebaseAuth.getInstance().useEmulator("10.0.2.2", 9099);
        db = FirebaseFirestore.getInstance();
        db.useEmulator("10.0.2.2", 8080);

        String senderUid = "followSenderTest";
        Map<String, Object> senderData = new HashMap<>();
        senderData.put("username", "sender_user");
        senderData.put("email", "sender@example.com");

        db.collection("Users").document(senderUid).set(senderData);
        Thread.sleep(1000);
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
                        ((DatePicker) view).updateDate(2000, 1, 1);
                    }
                });
        onView(withText("OK")).perform(click());
    }

    /**
     * Full test flow for viewing and accepting a follow request.
     */
    @Test
    public void testFollowRequestAcceptAndRemove() throws InterruptedException {
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

        Thread.sleep(2000);

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            db.collection("Users").document(user.getUid())
                    .update("followRequests", FieldValue.arrayUnion("followSenderTest"));
        }

        Thread.sleep(1000);

        onView(withId(R.id.nav_requests)).perform(click());

        Thread.sleep(1000);

        onView(withId(R.id.requestsRecyclerView)).check(matches(isDisplayed()));
        onView(withText("sender_user")).check(matches(isDisplayed()));

        onView(withText("Accept")).perform(click());

        Thread.sleep(1500);

        onView(withId(R.id.followersRecyclerView)).check(matches(isDisplayed()));
        onView(withText("sender_user")).check(matches(isDisplayed()));

        onView(withText("Remove")).perform(click());

        Thread.sleep(1000);
        onView(withText("sender_user")).check(doesNotExist());
    }

    @AfterClass
    public static void teardownFirestore() {
        try {
            URL url = new URL("http://10.0.2.2:8080/emulator/v1/projects/an_droids/databases/(default)/documents");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("DELETE");
            conn.getResponseCode();
            conn.disconnect();
        } catch (IOException e) {
            Log.e("Firestore Cleanup", e.getMessage());
        }
    }

    @AfterClass
    public static void teardownAuth() {
        try {
            URL url = new URL("http://10.0.2.2:9099/emulator/v1/projects/an-droids-194ef/accounts");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("DELETE");
            conn.getResponseCode();
            conn.disconnect();
        } catch (IOException e) {
            Log.e("Auth Cleanup", e.getMessage());
        }
    }
}