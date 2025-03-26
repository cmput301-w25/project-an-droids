package com.example.an_droids;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

/**
 * Displays each user who requested to follow me,
 * with "Accept" / "Reject" buttons.
 */
public class RequestsAdapter extends RecyclerView.Adapter<RequestsAdapter.ViewHolder> {

    private List<String> usernames;
    private List<String> userIds;
    private final RequestActionListener acceptListener;
    private final RequestActionListener rejectListener;

    public interface RequestActionListener {
        void onAction(int position);
    }

    public RequestsAdapter(List<String> usernames,
                           List<String> userIds,
                           RequestActionListener acceptListener,
                           RequestActionListener rejectListener) {
        this.usernames = usernames;
        this.userIds = userIds;
        this.acceptListener = acceptListener;
        this.rejectListener = rejectListener;
    }

    @NonNull
    @Override
    public RequestsAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // For simplicity, we reuse the same layout or create a new one
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_user_request, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RequestsAdapter.ViewHolder holder, int position) {
        holder.usernameTextView.setText(usernames.get(position));
        holder.acceptButton.setOnClickListener(v -> acceptListener.onAction(position));
        holder.rejectButton.setOnClickListener(v -> rejectListener.onAction(position));
    }

    @Override
    public int getItemCount() {
        return usernames.size();
    }

    public void updateLists(List<String> newUsernames, List<String> newUserIds) {
        this.usernames = newUsernames;
        this.userIds = newUserIds;
        notifyDataSetChanged();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView usernameTextView;
        Button acceptButton, rejectButton;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            usernameTextView = itemView.findViewById(R.id.requestUsernameTextView);
            acceptButton = itemView.findViewById(R.id.acceptButton);
            rejectButton = itemView.findViewById(R.id.rejectButton);
        }
    }
}
