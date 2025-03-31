package com.example.an_droids;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import com.google.firebase.firestore.Blob;
import com.google.firebase.firestore.Exclude;

import java.io.ByteArrayOutputStream;
import java.io.Serializable;
import java.util.Date;

/**
 * Represents a user in the application, containing user information such as
 * username, email, date of birth, and profile image (stored as a Blob).
 * This class is used for storing user data in Firebase Firestore and
 * allows converting between image formats for profile pictures.
 */
public class Users implements Serializable {

    private String username;
    private String email;
    private Date dob;
    private Blob profileImageBlob;

    @Exclude
    private transient Bitmap profileBitmap;

    /**
     * Default constructor for serialization and Firebase.
     */
    public Users() {}

    /**
     * Constructor to create a user with the specified username, email, and date of birth.
     *
     * @param username The username of the user.
     * @param email The email of the user.
     * @param dob The date of birth of the user.
     */
    public Users(String username, String email, Date dob) {
        this.username = username;
        this.email = email;
        this.dob = dob;
    }

    /**
     * Gets the username of the user.
     *
     * @return The username of the user.
     */
    public String getUsername() {
        return username;
    }

    /**
     * Sets the username of the user.
     *
     * @param username The username to set for the user.
     */
    public void setUsername(String username) {
        this.username = username;
    }

    /**
     * Gets the email of the user.
     *
     * @return The email of the user.
     */
    public String getEmail() {
        return email;
    }

    /**
     * Sets the email of the user.
     *
     * @param email The email to set for the user.
     */
    public void setEmail(String email) {
        this.email = email;
    }

    /**
     * Gets the date of birth of the user.
     *
     * @return The date of birth of the user.
     */
    public Date getDob() {
        return dob;
    }

    /**
     * Sets the date of birth of the user.
     *
     * @param dob The date of birth to set for the user.
     */
    public void setDob(Date dob) {
        this.dob = dob;
    }

    /**
     * Gets the profile image stored as a Blob in Firebase Firestore.
     *
     * @return The profile image as a Blob, or null if no image is set.
     */
    public Blob getProfileImageBlob() {
        return profileImageBlob;
    }

    /**
     * Sets the profile image using a Blob. If the Blob is non-null, the
     * corresponding Bitmap is decoded and stored for later retrieval.
     *
     * @param blob The Blob containing the profile image.
     */
    public void setProfileImageBlob(Blob blob) {
        this.profileImageBlob = blob;
        if (blob != null) {
            byte[] bytes = blob.toBytes();
            this.profileBitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
        }
    }

    /**
     * Gets the profile image as a Bitmap. If the Bitmap is not already
     * loaded, it is decoded from the stored Blob.
     *
     * @return The profile image as a Bitmap, or null if no image is set.
     */
    @Exclude
    public Bitmap getProfileBitmap() {
        if (profileBitmap == null && profileImageBlob != null) {
            byte[] bytes = profileImageBlob.toBytes();
            profileBitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
        }
        return profileBitmap;
    }

    /**
     * Sets the profile image using a Bitmap. The Bitmap is compressed and
     * stored as a Blob for Firebase Firestore.
     *
     * @param bitmap The Bitmap to set as the profile image.
     */
    @Exclude
    public void setProfileBitmap(Bitmap bitmap) {
        this.profileBitmap = bitmap;
        if (bitmap != null) {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.JPEG, 80, baos);
            byte[] bytes = baos.toByteArray();
            this.profileImageBlob = Blob.fromBytes(bytes);
        } else {
            this.profileImageBlob = null;
        }
    }
}
