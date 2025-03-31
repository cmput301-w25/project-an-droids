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

/**
 * Adapter for displaying a list of followed users with an option to unfollow.
 */
public class FollowAdapter extends RecyclerView.Adapter<FollowAdapter.FollowViewHolder> {
    private List<String> usernames;
    private List<String> userIds;
    private UnfollowCallback unfollowCallback;
    private String buttonLabel;


    /**
     * Callback interface for handling unfollow actions.
     */
    public interface UnfollowCallback {
        /**
         * Called when a user is unfollowed.
         *
         * @param position the position of the user in the list
         */
        void onUnfollow(int position);
    }

    /**
     * Constructs a FollowAdapter with the given user data and callback.
     *
     * @param usernames        list of usernames to display
     * @param userIds          list of corresponding user IDs
     * @param unfollowCallback callback for unfollow actions
     * @param buttonLabel      label for the action button
     */
    public FollowAdapter(List<String> usernames, List<String> userIds, UnfollowCallback unfollowCallback, String buttonLabel) {
        this.usernames = usernames != null ? usernames : new ArrayList<>();
        this.userIds = userIds != null ? userIds : new ArrayList<>();
        this.unfollowCallback = unfollowCallback;
        this.buttonLabel = buttonLabel;
    }

    /**
     * Creates and returns a new FollowViewHolder instance.
     *
     * @param parent   the parent view group
     * @param viewType the type of view
     * @return a new FollowViewHolder
     */
    @NonNull
    @Override
    public FollowViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_following, parent, false);
        return new FollowViewHolder(view);
    }

    /**
     * Binds data to the given ViewHolder.
     *
     * @param holder   the ViewHolder to bind data to
     * @param position the position of the item in the list
     */
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


    /**
     * Returns the total number of items in the adapter.
     *
     * @return the number of items
     */
    @Override
    public int getItemCount() {
        return usernames.size();
    }

    /**
     * Updates the list of usernames and user IDs and refreshes the adapter.
     *
     * @param newUsernames the new list of usernames
     * @param newUserIds   the new list of user IDs
     */
    public void updateLists(List<String> newUsernames, List<String> newUserIds) {
        this.usernames = newUsernames != null ? newUsernames : new ArrayList<>();
        this.userIds = newUserIds != null ? newUserIds : new ArrayList<>();
        notifyDataSetChanged();
    }

    /**
     * ViewHolder class for holding views in the RecyclerView.
     */
    static class FollowViewHolder extends RecyclerView.ViewHolder {
        TextView usernameTextView;
        Button unfollowButton;

        /**
         * Constructs a FollowViewHolder and initializes UI components.
         *
         * @param itemView the view for this ViewHolder
         */
        public FollowViewHolder(@NonNull View itemView) {
            super(itemView);
            usernameTextView = itemView.findViewById(R.id.usernameTextView);
            unfollowButton = itemView.findViewById(R.id.unfollowButton);
        }
    }
}