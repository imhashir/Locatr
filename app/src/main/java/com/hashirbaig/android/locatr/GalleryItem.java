package com.hashirbaig.android.locatr;

import android.net.Uri;

import com.google.gson.annotations.SerializedName;

public class GalleryItem {

    @SerializedName("title")
    private String mCaption;

    @SerializedName("id")
    private String mId;

    @SerializedName("url_s")
    private String mUrl;

    @SerializedName("url_o")
    private String mOriginalUrl;

    @SerializedName("owner")
    private String mOwner;

    @Override
    public String toString() {
        return mCaption;
    }

    public String getId() {
        return mId;
    }

    public void setId(String id) {
        mId = id;
    }

    public String getUrl() {
        return mUrl;
    }

    public Uri getPhotoPageUri() {
        return Uri.parse("https://www.flickr.com/photos/")
                .buildUpon()
                .appendPath(mOwner)
                .appendPath(mId)
                .build();
    }

    public String getOriginalUrl() {
        return mOriginalUrl;
    }
}
