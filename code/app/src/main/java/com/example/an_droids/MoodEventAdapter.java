package com.example.an_droids;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.text.BreakIterator;
import java.util.List;

public class MoodEventAdapter extends RecyclerView.Adapter<MoodEventAdapter.MoodEventViewHolder> {
    private List<MoodEvent> moodEvents;
    private Context context;

    public MoodEventAdapter(Context context, List<MoodEvent> moodEvents) {
        this.context = context;
        this.moodEvents = moodEvents;
    }

    @NonNull
    @Override
    public MoodEventViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.mood_event_item, parent, false);
        return new MoodEventViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MoodEventViewHolder holder, int position) {
        MoodEvent moodEvent = moodEvents.get(position);

        holder.moodTextView.setText(moodEvent.getMoodText());
        holder.moodContainer.setBackgroundColor(moodEvent.getMoodColor());

        String[] parts = moodEvent.getMoodText().split(" ", 2);

        // Safely handle date and mood text assignment
        if (parts.length > 1) {
            if (holder.dateTextView != null) {
                holder.dateTextView.setText(parts[0]); // Set the date text safely
            }
            holder.moodTextView.setText(parts[1]);
        } else {
            if (holder.dateTextView != null) {
                holder.dateTextView.setText(""); // Clear date text if not available
            }
            holder.moodTextView.setText(moodEvent.getMoodText());
        }

        // Set click listener for opening MoodActivity
        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(context, MoodActivity.class);
            intent.putExtra("position", position);
            ((Activity) context).startActivityForResult(intent, 1);
        });
    }



    @Override
    public int getItemCount() {
        return moodEvents.size();
    }

    public static class MoodEventViewHolder extends RecyclerView.ViewHolder {
        public BreakIterator dateTextView;
        TextView moodTextView;
        View moodContainer;

        public MoodEventViewHolder(@NonNull View itemView) {
            super(itemView);
            moodTextView = itemView.findViewById(R.id.moodTextView);
            moodContainer = itemView.findViewById(R.id.moodContainer);
        }
    }
}
