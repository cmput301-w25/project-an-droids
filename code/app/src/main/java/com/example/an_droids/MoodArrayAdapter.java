package com.example.an_droids;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

public class MoodArrayAdapter extends ArrayAdapter<Mood> {
    private ArrayList<Mood> moods;
    private Context context;

    public MoodArrayAdapter(Context context, ArrayList<Mood> moods) {
        super(context, 0, moods);
        this.moods = moods;
        this.context = context;
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        View view = convertView;
        if (view == null) {
            view = LayoutInflater.from(context).inflate(R.layout.layout_mood, parent, false);
        }
        Mood mood = moods.get(position);
        TextView moodName = view.findViewById(R.id.moodTitle);
        TextView dateView = view.findViewById(R.id.dateAdded);
        TextView timeView = view.findViewById(R.id.timeAdded);
        Date timestamp = mood.getTimestamp();
        moodName.setText(mood.getEmotion().toString() + " " + mood.getEmotion().getEmoji());
        SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        SimpleDateFormat timeFormatter = new SimpleDateFormat("HH:mm", Locale.getDefault());
        if (timestamp != null) {
            dateView.setText(dateFormatter.format(timestamp));
            timeView.setText(timeFormatter.format(timestamp));
        } else {
            dateView.setText("");
            timeView.setText("");
        }
        view.setBackgroundColor(Color.parseColor(mood.getEmotion().getColorHex()));
        return view;
    }
}