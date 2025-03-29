package com.example.an_droids;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import com.google.firebase.firestore.Blob;
import com.google.firebase.firestore.Exclude;

import java.io.ByteArrayOutputStream;
import java.io.Serializable;
import java.util.Date;

public class Users implements Serializable {
    private String username;
    private String email;
    private Date dob;
    private Blob profileImageBlob;

    @Exclude
    private transient Bitmap profileBitmap;

    public Users() {}

    public Users(String username, String email, Date dob) {
        this.username = username;
        this.email = email;
        this.dob = dob;
    }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public Date getDob() { return dob; }
    public void setDob(Date dob) { this.dob = dob; }

    public Blob getProfileImageBlob() {
        return profileImageBlob;
    }

    public void setProfileImageBlob(Blob blob) {
        this.profileImageBlob = blob;
        if (blob != null) {
            byte[] bytes = blob.toBytes();
            this.profileBitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
        }
    }

    @Exclude
    public Bitmap getProfileBitmap() {
        if (profileBitmap == null && profileImageBlob != null) {
            byte[] bytes = profileImageBlob.toBytes();
            profileBitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
        }
        return profileBitmap;
    }

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