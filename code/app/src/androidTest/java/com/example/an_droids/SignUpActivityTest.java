package com.example.an_droids;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.closeSoftKeyboard;
import static androidx.test.espresso.action.ViewActions.typeText;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isAssignableFrom;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withClassName;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;

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
 * Instrumented tests for {@link SignupActivity}.
 * <p>
 * Covers input validation logic, successful sign-up, and integration with Firebase Auth & Firestore emulator.
 */
@RunWith(AndroidJUnit4.class)
@LargeTest
public class SignUpActivityTest {

    /**
     * Launches the SignupActivity before each test.
     */
    @Rule
    public ActivityScenarioRule<SignupActivity> activityRule =
            new ActivityScenarioRule<>(SignupActivity.class);

    /**
     * Configures Firebase Auth and Firestore to use local emulators before any test is run.
     */
    @BeforeClass
    public static void setUp() {
        String androidLocalhost = "10.0.2.2";
        int portNumber = 8080;
        FirebaseFirestore.getInstance().useEmulator(androidLocalhost, portNumber);
        FirebaseAuth.getInstance().useEmulator(androidLocalhost, 9099);
    }

    /**
     * Selects a static date (2000-02-01) using the DatePicker.
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
                        return "Set date on DatePicker";
                    }

                    @Override
                    public void perform(UiController uiController, View view) {
                        ((DatePicker) view).updateDate(2000, 1, 1);
                    }
                });
        onView(withText("OK")).perform(click());
    }

    /**
     * Tests behavior when all input fields are empty and sign-up is attempted.
     */
    @Test
    public void testAllFieldsEmpty() {
        onView(withId(R.id.signupButton)).perform(click());
        onView(withId(R.id.signup_container)).check(matches(isDisplayed()));
    }

    /**
     * Tests invalid email format input and verifies user is not signed up.
     */
    @Test
    public void testInvalidEmail() {
        onView(withId(R.id.usernameInput)).perform(typeText("tester"), closeSoftKeyboard());
        onView(withId(R.id.emailInput)).perform(typeText("notanemail"), closeSoftKeyboard());
        onView(withId(R.id.passwordInput)).perform(typeText("password123"), closeSoftKeyboard());
        onView(withId(R.id.reenterPasswordInput)).perform(typeText("password123"), closeSoftKeyboard());
        pickDate();
        onView(withId(R.id.signupButton)).perform(click());
        onView(withId(R.id.signup_container)).check(matches(isDisplayed()));
    }

    /**
     * Tests case where password and confirm password fields do not match.
     */
    @Test
    public void testPasswordMismatch() {
        onView(withId(R.id.usernameInput)).perform(typeText("tester2"), closeSoftKeyboard());
        onView(withId(R.id.emailInput)).perform(typeText("test2@gmail.com"), closeSoftKeyboard());
        onView(withId(R.id.passwordInput)).perform(typeText("pass1"), closeSoftKeyboard());
        onView(withId(R.id.reenterPasswordInput)).perform(typeText("pass2"), closeSoftKeyboard());
        pickDate();
        onView(withId(R.id.signupButton)).perform(click());
        onView(withId(R.id.signup_container)).check(matches(isDisplayed()));
    }

    /**
     * Tests case where password is too short (<6 characters).
     */
    @Test
    public void testShortPassword() {
        onView(withId(R.id.usernameInput)).perform(typeText("tester3"), closeSoftKeyboard());
        onView(withId(R.id.emailInput)).perform(typeText("test3@gmail.com"), closeSoftKeyboard());
        onView(withId(R.id.passwordInput)).perform(typeText("123"), closeSoftKeyboard());
        onView(withId(R.id.reenterPasswordInput)).perform(typeText("123"), closeSoftKeyboard());
        pickDate();
        onView(withId(R.id.signupButton)).perform(click());
        onView(withId(R.id.signup_container)).check(matches(isDisplayed()));
    }

    /**
     * Tests a valid sign-up scenario and verifies user is redirected to MainActivity.
     *
     * @throws InterruptedException in case thread sleep is interrupted
     */
    @Test
    public void testValidSignup() throws InterruptedException {
        String email = "working" + System.currentTimeMillis() + "@gmail.com";

        onView(withId(R.id.usernameInput)).perform(typeText("workingUser"), closeSoftKeyboard());
        onView(withId(R.id.emailInput)).perform(typeText(email), closeSoftKeyboard());
        onView(withId(R.id.passwordInput)).perform(typeText("ValidPass123"), closeSoftKeyboard());
        onView(withId(R.id.reenterPasswordInput)).perform(typeText("ValidPass123"), closeSoftKeyboard());
        pickDate();
        onView(withId(R.id.signupButton)).perform(click());

        Thread.sleep(1500); // Firebase sign-up and redirect
        onView(withId(R.id.main_container)).check(matches(isDisplayed()));
    }

    /**
     * Deletes Firestore documents from local emulator for the custom project "an_droids".
     */
    @AfterClass
    public static void tearDown_firebase() {
        String projectId = "an_droids";
        try {
            URL url = new URL("http://10.0.2.2:8080/emulator/v1/projects/" + projectId + "/databases/(default)/documents");
            HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.setRequestMethod("DELETE");
            int response = urlConnection.getResponseCode();
            Log.i("Response Code", "Firestore Cleanup: " + response);
            urlConnection.disconnect();
        } catch (IOException exception) {
            Log.e("Firestore Cleanup", Objects.requireNonNull(exception.getMessage()));
        }
    }

    /**
     * Deletes all Auth accounts and Firestore data for project "an-droids-194ef" on the emulator.
     *
     * @throws IOException in case HTTP request fails
     */
    @AfterClass
    public static void tearDown_auth() throws IOException {
        String projectId = "an-droids-194ef";

        URL firestoreUrl = new URL("http://10.0.2.2:8080/emulator/v1/projects/" + projectId + "/databases/(default)/documents");
        HttpURLConnection firestoreConnection = (HttpURLConnection) firestoreUrl.openConnection();
        firestoreConnection.setRequestMethod("DELETE");
        Log.i("Firestore TearDown", "Response code: " + firestoreConnection.getResponseCode());
        firestoreConnection.disconnect();

        URL authUrl = new URL("http://10.0.2.2:9099/emulator/v1/projects/" + projectId + "/accounts");
        HttpURLConnection authConnection = (HttpURLConnection) authUrl.openConnection();
        authConnection.setRequestMethod("DELETE");
        Log.i("Auth TearDown", "Response code: " + authConnection.getResponseCode());
        authConnection.disconnect();
    }
}