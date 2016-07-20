package com.hashirbaig.android.locatr;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MarkerOptions;
import com.squareup.picasso.Picasso;

import java.io.IOException;
import java.util.List;

public class LocatrFragment extends SupportMapFragment {

    private static final String TAG = "LocatrFragment";

    private static final int REQUEST_LOCATION = 1;

    //private ImageView mImageView;
    //private ImageView mLoadView;

    private GoogleApiClient mGoogleApiClient;
    private List<GalleryItem> mItems;
    private GalleryItem mMapItem;
    private Bitmap mMapBitmap;
    private Location mCurrentLocation;
    private GoogleMap mMap;

    public static LocatrFragment newInstance() {
        return new LocatrFragment();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);

        mGoogleApiClient = new GoogleApiClient.Builder(getActivity())
                .addApi(LocationServices.API)
                .addConnectionCallbacks(new GoogleApiClient.ConnectionCallbacks() {
                    @Override
                    public void onConnected(@Nullable Bundle bundle) {
                        getActivity().invalidateOptionsMenu();
                    }

                    @Override
                    public void onConnectionSuspended(int i) {

                    }
                })
                .build();

        getMapAsync(new OnMapReadyCallback() {
            @Override
            public void onMapReady(GoogleMap googleMap) {
                mMap = googleMap;
                updateUI();
            }
        });
    }

    @Override
    public void onStart() {
        super.onStart();
        getActivity().invalidateOptionsMenu();
        mGoogleApiClient.connect();
    }

    @Override
    public void onStop() {
        super.onStop();
        mGoogleApiClient.disconnect();
    }

    /*
    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

        View v = inflater.inflate(R.layout.fragment_locatr, container, false);
        mImageView = (ImageView) v.findViewById(R.id.image);
        mLoadView = (ImageView) v.findViewById(R.id.loading);

        return v;
    }

    private void showLoading(boolean isVisible){
        mLoadView.setVisibility(isVisible ? View.VISIBLE : View.INVISIBLE);
    }

    */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        switch (requestCode) {
            case REQUEST_LOCATION:
                if(grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    findImage();
                }
                break;
            default:
                return;
        }
    }

    private void findImage() {
        LocationRequest request = LocationRequest.create();
        request.setInterval(0);
        request.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        request.setNumUpdates(1);

        if (ActivityCompat.checkSelfPermission(this.getContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this.getContext(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(getActivity(), new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, REQUEST_LOCATION);

            return;
        }

        LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, request, new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
                Log.i(TAG, "Got a fix: " + location);
                new FindImageTask().execute(location);
            }
        });
    }

    private void updateUI() {
        if(mMap == null || mMapBitmap == null)
            return;

        LatLng itemPoint = new LatLng(mMapItem.getLat(), mMapItem.getLon());
        LatLng myPoint = new LatLng(mCurrentLocation.getLatitude(), mCurrentLocation.getLongitude());

        BitmapDescriptor descriptor = BitmapDescriptorFactory.fromBitmap(mMapBitmap);
        MarkerOptions itemMarker = new MarkerOptions()
                .position(itemPoint)
                .icon(descriptor);
        MarkerOptions myMarker = new MarkerOptions()
                .position(myPoint);

        mMap.clear();
        mMap.addMarker(itemMarker);
        mMap.addMarker(myMarker);

        LatLngBounds bounds = new LatLngBounds.Builder()
                .include(itemPoint)
                .include(myPoint)
                .build();

        int margins = getResources().getDimensionPixelSize(R.dimen.map_inset_area);
        CameraUpdate update = CameraUpdateFactory.newLatLngBounds(bounds, margins);
        mMap.animateCamera(update);
    }

    private class FindImageTask extends AsyncTask<Location, Void, Void> {

        private Bitmap mBitmap;
        private Location mLocation;
        private GalleryItem mGalleryItem;

        @Override
        protected void onPreExecute() {
            //showLoading(true);
        }

        @Override
        protected Void doInBackground(Location... params) {

            mLocation = params[0];
            mItems = new FlickrFetchr().searchPhotos(mLocation);

            if(mItems.size() < 1)
                return null;

            mGalleryItem = mItems.get(0);

            try {
                byte[] mImageBytes = new FlickrFetchr().getUrlBytes(mItems.get(0).getUrl());
                mBitmap = BitmapFactory.decodeByteArray(mImageBytes, 0, mImageBytes.length);
            } catch (IOException ioe) {
                ioe.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            //showLoading(false);
            mMapBitmap = mBitmap;
            mCurrentLocation = mLocation;
            mMapItem = mGalleryItem;
            updateUI();
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.fragment_locatr, menu);

        MenuItem searchItem = menu.findItem(R.id.action_locate);
        searchItem.setEnabled(mGoogleApiClient.isConnected());
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {

            case R.id.action_locate:
                findImage();

            default:
                return super.onOptionsItemSelected(item);

        }
    }
}
