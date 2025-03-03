package com.example.an_droids;

import android.content.Context;
import android.graphics.Movie;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;

public class MoodArrayAdapter extends ArrayAdapter<Mood> {
    private ArrayList<Mood> moods;
    private Context context;

    public MoodArrayAdapter(Context context, ArrayList<Mood> moods){
        super(context, 0, moods);
        this.moods = moods;
        this.context = context;
    }

    @NonNull
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent){
        View view = convertView;
        if (view == null){
            view = LayoutInflater.from(context).inflate(R.layout.layout_mood, parent, false);
        }

        Mood mood = moods.get(position);
//        TextView movieName = view.findViewById(R.id.textMovieName);
//        TextView movieGenre = view.findViewById(R.id.textMovieGenre);
//        TextView movieYear = view.findViewById(R.id.textMovieYear);
        TextView moodTitle = view.findViewById(R.id.)

//        movieName.setText(movie.getTitle());
//        movieGenre.setText(movie.getGenre());
//        movieYear.setText(movie.getYear());

        return view;
    }
}
