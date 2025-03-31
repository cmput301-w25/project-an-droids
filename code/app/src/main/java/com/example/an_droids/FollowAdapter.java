package com.example.an_droids;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class FollowAdapter extends RecyclerView.Adapter<FollowAdapter.FollowViewHolder> {
    private List<String> usernames;
    private List<String> userIds;
    private UnfollowCallback unfollowCallback;
    private String buttonLabel;


    public interface UnfollowCallback {
        void onUnfollow(int position);
    }

    public FollowAdapter(List<String> usernames, List<String> userIds, UnfollowCallback unfollowCallback, String buttonLabel) {
        this.usernames = usernames != null ? usernames : new ArrayList<>();
        this.userIds = userIds != null ? userIds : new ArrayList<>();
        this.unfollowCallback = unfollowCallback;
        this.buttonLabel = buttonLabel;
    }

    @NonNull
    @Override
    public FollowViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_following, parent, false);
        return new FollowViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull FollowViewHolder holder, int position) {
        String username = usernames.get(position);
        holder.usernameTextView.setText(username);
        holder.unfollowButton.setText(buttonLabel);

        holder.unfollowButton.setOnClickListener(v -> {
            if (unfollowCallback != null) {
                unfollowCallback.onUnfollow(position);
            }
        });
    }


    @Override
    public int getItemCount() {
        return usernames.size();
    }

    public void updateLists(List<String> newUsernames, List<String> newUserIds) {
        this.usernames = newUsernames != null ? newUsernames : new ArrayList<>();
        this.userIds = newUserIds != null ? newUserIds : new ArrayList<>();
        notifyDataSetChanged();
    }

    static class FollowViewHolder extends RecyclerView.ViewHolder {
        TextView usernameTextView;
        Button unfollowButton;

        public FollowViewHolder(@NonNull View itemView) {
            super(itemView);
            usernameTextView = itemView.findViewById(R.id.usernameTextView);
            unfollowButton = itemView.findViewById(R.id.unfollowButton);
        }
    }
}