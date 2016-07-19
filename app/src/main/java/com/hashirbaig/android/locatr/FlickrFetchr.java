package com.hashirbaig.android.locatr;

import android.location.Location;
import android.net.Uri;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

public class FlickrFetchr {

    private static final String TAG = "FlickrFetchr";
    private static final String API_KEY = "3741e5f85bd098dcccfb4713e4921b22";
    private static final String METHOD_SEARCH = "flickr.photos.search";
    private static final Uri ENDPOINT = Uri.parse("https://api.flickr.com/services/rest/")
            .buildUpon()
            .appendQueryParameter("method", METHOD_SEARCH)
            .appendQueryParameter("api_key", API_KEY)
            .appendQueryParameter("format", "json")
            .appendQueryParameter("page", "1")
            .appendQueryParameter("nojsoncallback", "1")
            .appendQueryParameter("extras", "url_s, url_o")
            .build();

    public byte[] getUrlBytes(String urlSpec) throws IOException {
        URL url = new URL(urlSpec);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();

        try {
            InputStream input = connection.getInputStream();
            ByteArrayOutputStream out = new ByteArrayOutputStream();

            if(connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                throw new IOException(connection.getResponseMessage() + ": with " + urlSpec);
            }

            int bytesRead = 0;
            byte[] buffer = new byte[1024];

            while ((bytesRead = input.read(buffer)) > 0) {
                out.write(buffer, 0, bytesRead);
            }
            out.close();
            return out.toByteArray();
        } finally {
            connection.disconnect();
        }
    }

    public String getUrlString(String urlSpec) throws IOException {
        return new String(getUrlBytes(urlSpec));
    }

    private String urlBuilder (Location location) {
        Uri.Builder urlBuilder = ENDPOINT
                .buildUpon()
                .appendQueryParameter("lat", "" + location.getLatitude())
                .appendQueryParameter("lon", "" + location.getLongitude());

        return urlBuilder.build().toString();
    }

    public List<GalleryItem> searchPhotos(Location location) {
        String url = urlBuilder(location);
        return downloadItems(url);
    }

    private List<GalleryItem> downloadItems(String url) {

        List<GalleryItem> images = new ArrayList<>();
        try {
            String jsonString = getUrlString(url);
            Log.i(TAG, "Received JSON: " + jsonString);
            JSONObject jsonObject = new JSONObject(jsonString);
            images = parseGsonArray(jsonObject);
            int i = 0;

            Iterator<GalleryItem> itr = images.iterator();
            while (itr.hasNext()) {
                GalleryItem item = itr.next();

                if(item.getUrl() == null) {
                    itr.remove();
                }
            }

        } catch (IOException ioe) {
            Log.e(TAG, "Can't fetch data", ioe);
        } catch (JSONException e) {
            Log.e(TAG, "Can't parse JSON data " + e);
        }
        return images;
    }

    private List<GalleryItem> parseGsonArray(JSONObject jsonBody) throws JSONException {
        Gson gson = new GsonBuilder().create();
        JSONObject jsonImageData = jsonBody.getJSONObject("photos");
        JSONArray jsonImageArray = jsonImageData.getJSONArray("photo");

        return new ArrayList<>(Arrays.asList(gson.fromJson(jsonImageArray.toString(), GalleryItem[].class)));
    }
}
