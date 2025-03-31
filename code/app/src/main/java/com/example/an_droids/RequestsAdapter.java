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
 * Adapter class for displaying user follow requests in a RecyclerView.
 * Each request has "Accept" and "Reject" buttons that trigger the corresponding actions.
 *
 * <p>This class binds a list of usernames and their respective userIds to the RecyclerView.
 * It provides callbacks for when the accept or reject buttons are clicked for each request.</p>
 */
public class RequestsAdapter extends RecyclerView.Adapter<RequestsAdapter.ViewHolder> {

    private List<String> usernames; // List of usernames of users who requested to follow
    private List<String> userIds; // List of user IDs corresponding to the usernames
    private final RequestActionListener acceptListener; // Listener for accept button clicks
    private final RequestActionListener rejectListener; // Listener for reject button clicks

    /**
     * Interface for handling accept/reject actions on a follow request.
     */
    public interface RequestActionListener {
        /**
         * Called when an action (accept/reject) is triggered for a specific request.
         *
         * @param position The position of the request in the list
         */
        void onAction(int position);
    }

    /**
     * Constructor to initialize the adapter with the required data and action listeners.
     *
     * @param usernames List of usernames requesting to follow
     * @param userIds List of user IDs corresponding to the usernames
     * @param acceptListener Listener for when the accept button is clicked
     * @param rejectListener Listener for when the reject button is clicked
     */
    public RequestsAdapter(List<String> usernames,
                           List<String> userIds,
                           RequestActionListener acceptListener,
                           RequestActionListener rejectListener) {
        this.usernames = usernames;
        this.userIds = userIds;
        this.acceptListener = acceptListener;
        this.rejectListener = rejectListener;
    }

    /**
     * Creates a new ViewHolder to hold the layout for each follow request item.
     *
     * @param parent The parent ViewGroup to which the new view will be attached
     * @param viewType The type of the view
     * @return A new ViewHolder object
     */
    @NonNull
    @Override
    public RequestsAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Inflate the item layout for the request view
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_user_request, parent, false);
        return new ViewHolder(view);
    }

    /**
     * Binds the username and buttons for each request item in the RecyclerView.
     *
     * @param holder The ViewHolder that holds the view to be updated
     * @param position The position of the item in the dataset
     */
    @Override
    public void onBindViewHolder(@NonNull RequestsAdapter.ViewHolder holder, int position) {
        holder.usernameTextView.setText(usernames.get(position));
        holder.acceptButton.setOnClickListener(v -> acceptListener.onAction(position));
        holder.rejectButton.setOnClickListener(v -> rejectListener.onAction(position));
    }

    /**
     * Returns the total number of items in the dataset.
     *
     * @return The size of the list of usernames
     */
    @Override
    public int getItemCount() {
        return usernames.size();
    }

    /**
     * Updates the adapter with a new list of usernames and user IDs.
     *
     * @param newUsernames The new list of usernames
     * @param newUserIds The new list of user IDs
     */
    public void updateLists(List<String> newUsernames, List<String> newUserIds) {
        this.usernames = newUsernames;
        this.userIds = newUserIds;
        notifyDataSetChanged();
    }

    /**
     * ViewHolder class to hold references to the UI elements for each request item.
     */
    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView usernameTextView; // TextView to display the username
        Button acceptButton, rejectButton; // Buttons to accept or reject the request

        /**
         * Constructor to initialize the ViewHolder with the required views.
         *
         * @param itemView The view representing the request item
         */
        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            usernameTextView = itemView.findViewById(R.id.requestUsernameTextView);
            acceptButton = itemView.findViewById(R.id.acceptButton);
            rejectButton = itemView.findViewById(R.id.rejectButton);
        }
    }
}
