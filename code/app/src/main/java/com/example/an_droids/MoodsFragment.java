package com.example.an_droids;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import androidx.fragment.app.Fragment;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.ArrayList;

public class MoodsFragment extends Fragment {
    private ListView moodListView;
    private MoodArrayAdapter moodArrayAdapter;
    private ArrayList<Mood> moodList;
    private String userId;
    private MoodProvider moodProvider;

    public MoodsFragment(String userId) {
        this.userId = userId;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_moods, container, false);

        moodListView = view.findViewById(R.id.moodList);
        moodList = new ArrayList<>();
        moodArrayAdapter = new MoodArrayAdapter(getContext(), moodList);
        moodListView.setAdapter(moodArrayAdapter);

        moodProvider = new MoodProvider(FirebaseFirestore.getInstance(), userId);
        loadMoods();

        return view;
    }

    private void loadMoods() {
        moodList.addAll(moodProvider.getMoods());
        moodArrayAdapter.notifyDataSetChanged();

        moodProvider.listenForUpdates(new MoodProvider.DataStatus() {
            @Override
            public void onDataUpdated() {
                moodList.clear();
                moodList.addAll(moodProvider.getMoods());
                moodArrayAdapter.notifyDataSetChanged();
            }

            @Override
            public void onError(String error) {
                Log.e("MoodsFragment", "Error loading moods: " + error);
            }
        });
    }
}