package com.grawin.parkingreminder;

import android.Manifest;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.IntentSender;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

public class MapsActivity extends FragmentActivity implements
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        LocationListener,
        OnMapReadyCallback {

    private double currentLatitude;
    private double currentLongitude;

    private double parkLat;
    private double parkLong;

    private Marker parkMarker = null;

    //public static final String TAG = MapsActivity.class.getSimpleName();

    private final static int CONNECTION_FAILURE_RESOLUTION_REQUEST = 9000;

    private GoogleMap mMap; // Might be null if Google Play services APK is not available.

    private GoogleApiClient mGoogleApiClient;
    private LocationRequest mLocationRequest;

    private ImageButton mTimerButton = null;

    private TextView mTimerText = null;

    private boolean mMapReady = false;
    private boolean mConnected = false;
    private boolean mRequestingPermissions = false;

    /**
     * Broadcast receiver for the timer service.
     */
    private BroadcastReceiver br = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            handleTickEvent(intent);
        }
    };

    private void handleTickEvent(Intent intent) {
        mTimerText.setText(DataStore.formatTimeString(DataStore.getTimerSec()));

        if (mTimerText.getVisibility() == View.GONE) {
            mTimerText.setVisibility(View.VISIBLE);

            mTimerButton.setImageResource(R.drawable.ic_timer_off_red_500_24dp);
        }

        boolean isEndEvent = intent.getBooleanExtra("end", false);
        if (isEndEvent) {
            mTimerText.setVisibility(View.GONE);

            mTimerButton.setImageResource(R.drawable.ic_timer_blue_24dp);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);

        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();

        // Initialize the data store class that is used to access shared preferences and static
        // run-time data that lasts the lifetime of this application instance.
        DataStore.initialize(this);

        mTimerText = (TextView) findViewById(R.id.timer_text);

        mTimerButton = (ImageButton) findViewById(R.id.timer_button);

        setupParkingLoc();

        setUpMapIfNeeded();

        // Create the LocationRequest object
        mLocationRequest = LocationRequest.create()
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
                .setInterval(10 * 1000)        // 10 seconds, in milliseconds
                .setFastestInterval(1 * 1000); // 1 second, in milliseconds
    }

    @Override
    protected void onStart() {
        super.onStart();
        mGoogleApiClient.connect();
    }

    protected void onStop() {
        super.onStop();
        mGoogleApiClient.disconnect();
    }


    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMapReady = true;

        mMap = googleMap;
        // Disable stock map buttons, want to use custom controls.
        mMap.getUiSettings().setMapToolbarEnabled(false);
        setUpMap();

        requestMapLocationUpdates();
    }

    private void setUpMap() {
        SharedPreferences sharedPreferences = getPreferences(Context.MODE_PRIVATE);
        int mapType = sharedPreferences.getInt(getString(R.string.pref_layers),
                GoogleMap.MAP_TYPE_HYBRID);
        mMap.setMapType(mapType);

        // If parking has a value then setup the marker.
        if (!isZero(parkLat, EPSILON)) {
            LatLng latLng = new LatLng(parkLat, parkLong);

            // Add the new marker.
            MarkerOptions marker = new MarkerOptions()
                    .position(latLng)
                    .title("Parked Here")
                    .icon(BitmapDescriptorFactory
                            .defaultMarker(BitmapDescriptorFactory.HUE_AZURE));

            parkMarker = mMap.addMarker(marker);
        }
    }


    private void handleNewLocation(Location location) {
        // Store off new location, but don't move it until user hits button.
        currentLatitude = location.getLatitude();
        currentLongitude = location.getLongitude();

        // If automatic position tracking is enabled, then move the map camera if the user went
        // off screen.
        if (mMap != null && DataStore.isTrackPosEnabled()) {
            LatLng currentLatLng = new LatLng(currentLatitude, currentLongitude);
            LatLngBounds bounds = mMap.getProjection().getVisibleRegion().latLngBounds;
            if (!bounds.contains(currentLatLng)) {
                CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLng(currentLatLng);
                mMap.animateCamera(cameraUpdate);
            }
        }

        //LatLng latLng = new LatLng(currentLatitude, currentLongitude);

        // Remove the existing marker.
        /*
        if (currentMarker != null) {
            currentMarker.remove();
        }
        // Add the new marker.
        MarkerOptions options = new MarkerOptions()
                .position(latLng)
                .title("You are here")
                .icon(BitmapDescriptorFactory
                        .fromResource(android.R.drawable.ic_menu_mylocation));
        currentMarker = mMap.addMarker(options);
        */
        //mMap.moveCamera(CameraUpdateFactory.newLatLngZoom((latLng), 18.0f));
        //mMap.animateCamera(CameraUpdateFactory.zoomTo(18.0f), 2000, null);


    }

    @Override
    protected void onResume() {
        super.onResume();

        mGoogleApiClient.connect();

        registerReceiver(br, new IntentFilter(TimerService.TIMER_BR));
        setupParkingLoc();
        setUpMapIfNeeded();

        // If the timer was running, but stopped then cleanup timer UI.
        if (DataStore.getTimerSec() == 0) {
            mTimerText.setVisibility(View.GONE);
            mTimerButton.setImageResource(R.drawable.ic_timer_blue_24dp);
        } else {
            // Timer is running!
            mTimerText.setVisibility(View.VISIBLE);
            mTimerButton.setImageResource(R.drawable.ic_timer_off_red_500_24dp);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(br);
        if (mGoogleApiClient.isConnected()) {
            LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, this);
            mGoogleApiClient.disconnect();
        }
    }

    private void setUpMapIfNeeded() {
        // Do a null check to confirm that we have not already instantiated the map.
        if (mMap == null) {
            // Try to obtain the map from the SupportMapFragment.
            /*
            mMap = ((SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map))
                    .getMap();
            */
            SupportMapFragment mapFragment = ((SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map));
            mapFragment.getMapAsync(this);
            // Check if we were successful in obtaining the map.
            /*
            if (mMap != null) {
                // Disable stock map buttons, want to use custom controls.
                mMap.getUiSettings().setMapToolbarEnabled(false);
                setUpMap();
            }
            */
        }
    }

    @Override
    public void onConnected(Bundle bundle) {

        requestMapLocationUpdates();

        if (mMap != null) {
            LatLng latLng = new LatLng(currentLatitude, currentLongitude);
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom((latLng), 18.0f));
            mMap.animateCamera(CameraUpdateFactory.zoomTo(18.0f), 2000, null);
        }
    }

    void requestMapLocationUpdates() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == android.content.pm.PackageManager.PERMISSION_GRANTED) {

            if (mMap != null) {
                // Enable blue my location dot.
                mMap.setMyLocationEnabled(true);
                mMap.getUiSettings().setMyLocationButtonEnabled(false);
            }

            if (!mGoogleApiClient.isConnected()) {
                mGoogleApiClient.connect();
            } else {
                LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);
                Location location = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
                if (location != null) {
                    handleNewLocation(location);
                }
            }
        } else {
            checkLocationPermission();
        }
    }

    @Override
    public void onConnectionSuspended(int i) {
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        if (connectionResult.hasResolution()) {
            try {
                // Start an Activity that tries to resolve the error
                connectionResult.startResolutionForResult(this, CONNECTION_FAILURE_RESOLUTION_REQUEST);
                /*
                 * Thrown if Google Play services canceled the original
                 * PendingIntent
                 */
            } catch (IntentSender.SendIntentException e) {
                // Log the error
                e.printStackTrace();
            }
        } else {
            /*
             * If no resolution is available, display a dialog to the
             * user with the error.
             */
            //Log.i(TAG, "Location services connection failed with code " + connectionResult.getErrorCode());
        }
    }

    @Override
    public void onLocationChanged(Location location) {
        handleNewLocation(location);
    }

    public static final int MY_PERMISSIONS_REQUEST_LOCATION = 99;

    public boolean checkLocationPermission() {

        if (mRequestingPermissions) {
            return false;
        }
        mRequestingPermissions = true;

        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {

            // Should we show an explanation?
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.ACCESS_FINE_LOCATION)) {
                // Show an explanation to the user *asynchronously* -- don't block
                // this thread waiting for the user's response! After the user
                // sees the explanation, try again to request the permission.

                //Prompt the user once explanation has been shown
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                        MY_PERMISSIONS_REQUEST_LOCATION);


            } else {
                // No explanation needed, we can request the permission.
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                        MY_PERMISSIONS_REQUEST_LOCATION);
            }
            return false;
        } else {
            return true;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_LOCATION: {
                mRequestingPermissions = false;
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // Permission was granted, but don't actually do anything here for now
                    // because objects might not be initialized properly yet!
                } else {
                    // permission denied
                    Toast.makeText(this,
                            "Permission denied, but Parking Reminder requires location access.",
                            Toast.LENGTH_LONG).show();
                    checkLocationPermission();
                }
            }
            // Other case lines to check for other permissions this app might request would go here.
        }
    }

    private void setupParkingLoc() {
        SharedPreferences sharedPreferences = getPreferences(Context.MODE_PRIVATE);
        parkLat = sharedPreferences.getFloat(getString(R.string.pref_park_lat), 0.0f);
        parkLong = sharedPreferences.getFloat(getString(R.string.pref_park_long), 0.0f);

        //Log.i(TAG, "park lat " + parkLat + " long " + parkLong);
    }

    public static double EPSILON = 0.001;

    public boolean isZero(double value, double threshold) {
        return value >= -threshold && value <= threshold;
    }

    DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
        @Override
        public void onClick(DialogInterface dialog, int which) {
            switch (which) {
                case DialogInterface.BUTTON_POSITIVE:
                    // Yes button clicked
                    park();
                    break;

                case DialogInterface.BUTTON_NEGATIVE:
                    // No button clicked - do nothing!
                    break;
            }
        }
    };

    public void handlePark(View view) {

        if (!isZero(parkLat, EPSILON)) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setMessage("Overwrite current parked location?").setPositiveButton("Yes", dialogClickListener)
                    .setNegativeButton("No", dialogClickListener).show();
        } else {
            park();
        }
    }

    public void park() {
        parkLat = currentLatitude;
        parkLong = currentLongitude;

        // Save off parking location.
        SharedPreferences sharedPreferences = getPreferences(Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putFloat(getString(R.string.pref_park_lat), (float) parkLat);
        editor.putFloat(getString(R.string.pref_park_long), (float) parkLong);
        editor.apply();

        LatLng latLng = new LatLng(currentLatitude, currentLongitude);

        // Remove the existing marker.
        if (parkMarker != null) {
            parkMarker.remove();
        }
        // Add the new marker.
        MarkerOptions options = new MarkerOptions()
                .position(latLng)
                .title("Parked Here")
                .icon(BitmapDescriptorFactory
                        .defaultMarker(BitmapDescriptorFactory.HUE_AZURE));
        //.fromResource(R.drawable.ic_local_parking_blue_18dp));
        parkMarker = mMap.addMarker(options);
    }

    public void handleNavigate(View view) {
        if (!isZero(parkLat, EPSILON)) {
            Uri gmmIntentUri = Uri.parse("google.navigation:q=" + parkLat + ", " + parkLong + "&mode=w");
            Intent mapIntent = new Intent(Intent.ACTION_VIEW, gmmIntentUri);
            mapIntent.setPackage("com.google.android.apps.maps");
            startActivity(mapIntent);
        } else {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setMessage("Please park before navigating")
                    .setPositiveButton("OK", null)
                    .show();
        }
    }

    public void handleLocate(View view) {

        LatLng latLng = new LatLng(currentLatitude, currentLongitude);

        /*
        Zoom levels
        1: World
        5: Landmass/continent
        10: City
        15: Streets
        20: Buildings
         */

        //mMap.moveCamera(CameraUpdateFactory.newLatLngZoom((latLng), 18.0f));
        CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngZoom(latLng, 18.0f);
        mMap.animateCamera(cameraUpdate);
        //mMap.animateCamera(CameraUpdateFactory.zoomTo(18.0f), 2000, null);
    }

    public void handleLayers(View view) {
        // Alternate between hybrid and normal
        int newType = mMap.getMapType() == GoogleMap.MAP_TYPE_HYBRID ?
                GoogleMap.MAP_TYPE_NORMAL : GoogleMap.MAP_TYPE_HYBRID;
        mMap.setMapType(newType);

        // Store off last selected map type, to be set on resume.
        SharedPreferences sharedPreferences = getPreferences(Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putInt(getString(R.string.pref_layers), newType);
        editor.apply();
    }

    public void handleTimer(View view) {

        if (DataStore.getTimerSec() == 0) {
            // Start timer.
            Intent intent = new Intent(MapsActivity.this, TimerActivity.class);
            startActivity(intent);

        } else {
            // Stop service, clear time.
            stopService(new Intent(this, TimerService.class));
            DataStore.setTimerSec(0);
            // Clear notification.
            NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            notificationManager.cancel(DataStore.NOTIFICATION_ID);
        }
    }

    public void handlePreferenceButton(View view) {
        Intent intent = new Intent(this, MainPreferenceActivity.class);
        startActivity(intent);
    }

}