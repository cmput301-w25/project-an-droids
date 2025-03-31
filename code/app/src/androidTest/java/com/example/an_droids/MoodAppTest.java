package com.example.an_droids;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.closeSoftKeyboard;
import static androidx.test.espresso.action.ViewActions.typeText;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;

import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;

import com.google.firebase.firestore.FirebaseFirestore;

import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
@LargeTest
public class MoodAppTest {

    @Rule
    public ActivityScenarioRule<LoginActivity> activityRule =
            new ActivityScenarioRule<>(LoginActivity.class);

    @BeforeClass
    public static void setUp() {
        String androidLocalhost = "10.0.2.2";
        int portNumber = 8080;
        FirebaseFirestore.getInstance().useEmulator(androidLocalhost, portNumber);
    }
    @Test
    public void testLogin() {
        onView(withId(R.id.emailInput)).check(matches(isDisplayed()));
        onView(withId(R.id.passwordInput)).check(matches(isDisplayed()));
        onView(withId(R.id.loginButton)).check(matches(isDisplayed()));
        onView(withId(R.id.emailInput))
                .perform(typeText("test@example.com"), closeSoftKeyboard());

        // Click login button
        onView(withId(R.id.loginButton)).perform(click());
        onView(withId(R.id.login_container)).check(matches(isDisplayed()));
        onView(withId(R.id.signupLink)).perform(click());

        // Verify we moved to register activity
        // Replace RegisterActivity.class with your actual register activity
        ActivityScenario<SignupActivity> scenario = ActivityScenario.launch(SignupActivity.class);
        onView(withId(R.id.signup_container)).check(matches(isDisplayed()));
        onView(withId(R.id.loginLink)).perform(click());
        onView(withId(R.id.login_container)).check(matches(isDisplayed()));
        onView(withId(R.id.emailInput))
                .perform(typeText("swilson@ualberta.ca"), closeSoftKeyboard());
        onView(withId(R.id.passwordInput))
                .perform(typeText("Test@1234"), closeSoftKeyboard());
        onView(withId(R.id.loginButton)).perform(click());
        ActivityScenario<MainActivity> main = ActivityScenario.launch(MainActivity.class);
        onView(withId(R.id.main_container)).check(matches(isDisplayed()));
    }
}