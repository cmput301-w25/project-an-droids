package com.example.an_droids;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import androidx.annotation.NonNull;
import androidx.test.core.app.ActivityScenario;
import androidx.test.espresso.Espresso;
import androidx.test.espresso.IdlingRegistry;
import androidx.test.espresso.IdlingResource;
import androidx.test.espresso.action.ViewActions;
import androidx.test.espresso.matcher.ViewMatchers;
import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;
import com.google.firebase.firestore.FirebaseFirestore;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.Espresso.onData;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.replaceText;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.isRoot;
import static org.hamcrest.Matchers.anything;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import java.util.Date;

@RunWith(AndroidJUnit4.class)
@LargeTest
public class MoodAppTest {

//    @Rule
//    public ActivityScenarioRule<MainActivity> activityRule = new ActivityScenarioRule<>(MainActivity.class);
//
//    private MoodProvider moodProvider;
//
//    @Before
//    public void setUp() {
//        moodProvider = new MoodProvider();
//    }
//
//    @After
//    public void tearDown() {
//        FirebaseFirestore.getInstance().collection("Moods").get().addOnCompleteListener(task -> {
//            if (task.isSuccessful() && task.getResult() != null) {
//                task.getResult().forEach(document -> document.getReference().delete());
//            }
//        });
//    }


//    @Test
//    public void testEditMood() {
//        Mood mood = new Mood("Happiness", "Feeling good", null, new Date(), null, "Alone", Mood.Privacy.PUBLIC);
//        moodProvider.addMood(mood, new MoodProvider.OnMoodOperationListener() {
//            @Override
//            public void onSuccess() {
//                activityRule.getScenario().onActivity(activity -> {
//                    onView(withText("Happiness 😃")).perform(click());
//                    onView(withId(R.id.reasonEditText)).check(matches(isDisplayed()));
//                    onView(withId(R.id.reasonEditText)).perform(replaceText("Feeling great"));
//                    onView(withText("Save")).perform(click());
//
//                    onView(withId(R.id.moodTitle)).check(matches(withText("Feeling great")));
//                });
//            }
//
//            @Override
//            public void onFailure(Exception e) {
//            }
//        });
//    }
//
//    @Test
//    public void testDeleteMood() {
//        Mood mood = new Mood("Happiness", "Feeling good", null, new Date(), null, "Alone", Mood.Privacy.PRIVATE);
//        moodProvider.addMood(mood, new MoodProvider.OnMoodOperationListener() {
//            @Override
//            public void onSuccess() {
//                activityRule.getScenario().onActivity(activity -> {
//                    onView(withText("Happiness 😃")).perform(ViewActions.longClick());
//                    onView(withText("Yes")).perform(click());
//
//                    onView(withId(R.id.moodList)).check(matches(withText("")));
//                });
//            }
//
//            @Override
//            public void onFailure(Exception e) {
//            }
//        });
//    }
}



