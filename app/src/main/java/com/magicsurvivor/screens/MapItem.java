package com.magicsurvivor.screens;

import android.net.Uri;

public class MapItem {
    private String name;
    private int imageResId;
    private Uri imageUri;
    private String description;
    private boolean isCustomImage;

    // Constructor for drawable resource
    public MapItem(String name, int imageResId, String description) {
        this.name = name;
        this.imageResId = imageResId;
        this.imageUri = null;
        this.description = description;
        this.isCustomImage = false;
    }

    // Constructor for URI-based image
    public MapItem(String name, Uri imageUri, String description) {
        this.name = name;
        this.imageResId = -1;
        this.imageUri = imageUri;
        this.description = description;
        this.isCustomImage = true;
    }

    public String getName() {
        return name;
    }

    public int getImageResId() {
        return imageResId;
    }

    public Uri getImageUri() {
        return imageUri;
    }

    public String getDescription() {
        return description;
    }

    public boolean isCustomImage() {
        return isCustomImage;
    }

    public void setImageUri(Uri uri) {
        this.imageUri = uri;
        this.isCustomImage = true;
    }
}
