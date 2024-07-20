package com.example.locationlibrary;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Application;
import android.content.Context;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.common.api.ResolvableApiException;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResponse;
import com.google.android.gms.location.Priority;
import com.google.android.gms.location.SettingsClient;
import com.google.android.gms.tasks.Task;

import java.util.function.BiConsumer;

public class MyLocation {

    private static MyLocation instance = null;
    private Context applicationContext;
    private LocationRequest locationRequest;
    private LocationManager locationManager;

    private static final int REQUEST_CHECK_SETTINGS = 1001;
    private static final String TAG = "MyLocation";

    private BiConsumer<Double, Double> locationCallback;
    private AppCompatActivity activity;

    private MyLocation() {
        // Private constructor to prevent instantiation
    }

    public static MyLocation getInstance() {
        if (instance == null) {
            synchronized (MyLocation.class) {
                if (instance == null) {
                    instance = new MyLocation();
                }
            }
        }
        return instance;
    }

    public void initializeApp(Application application) {
        this.applicationContext = application.getApplicationContext();
        this.locationManager = (LocationManager) applicationContext.getSystemService(Context.LOCATION_SERVICE);
        initializeLocationRequest();
    }

    private void initializeLocationRequest() {
        locationRequest = new LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY)
                .setIntervalMillis(10000)
                .setMinUpdateIntervalMillis(5000)
                .build();
    }

    public void checkLocationAndRequestUpdates(AppCompatActivity activity, BiConsumer<Double, Double> locationCallback) {
        this.activity = activity;
        this.locationCallback = locationCallback;

        if (!isLocationEnabled()) {
            showEnableLocationDialog(activity);
        } else {
            Log.d(TAG, "Location services are enabled.");
            checkLocationPermission(activity, locationCallback);
        }
    }

    private boolean isLocationEnabled() {
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
    }

    private void checkLocationPermission(AppCompatActivity activity, BiConsumer<Double, Double> locationCallback) {
        if (ContextCompat.checkSelfPermission(applicationContext, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            Log.d(TAG, "Location permission not granted.");
            ActivityCompat.requestPermissions(activity,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_CHECK_SETTINGS);
        } else {
            Log.d(TAG, "Location permission granted.");
            getCurrentLocation(activity, locationCallback);
        }
    }

    private void showEnableLocationDialog(AppCompatActivity activity) {
        new AlertDialog.Builder(activity)
                .setTitle("Location Services Not Enabled")
                .setMessage("Please enable Location Services to use this feature.")
                .setPositiveButton("Enable", (dialog, which) -> {
                    dialog.dismiss();
                    turnOnGPS(activity);
                })
                .setNegativeButton("Cancel", (dialog, which) -> {
                    dialog.dismiss();
                    handleLocationError("Location services are required but not enabled.");
                })
                .show();
    }

    private void turnOnGPS(AppCompatActivity activity) {
        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder()
                .addLocationRequest(locationRequest);
        builder.setAlwaysShow(true);

        SettingsClient settingsClient = LocationServices.getSettingsClient(activity);
        Task<LocationSettingsResponse> task = settingsClient.checkLocationSettings(builder.build());

        task.addOnSuccessListener(activity, locationSettingsResponse ->
                getCurrentLocation(activity, null));

        task.addOnFailureListener(activity, e -> {
            if (e instanceof ResolvableApiException) {
                try {
                    ResolvableApiException resolvable = (ResolvableApiException) e;
                    resolvable.startResolutionForResult(activity, REQUEST_CHECK_SETTINGS);
                } catch (IntentSender.SendIntentException sendEx) {
                    Log.e(TAG, "Error starting resolution for GPS settings", sendEx);
                    handleLocationError("Unable to start location settings resolution.");
                }
            } else {
                handleLocationError("Location settings are not satisfied.");
            }
        });
    }

    @SuppressLint("MissingPermission")
    private void getCurrentLocation(AppCompatActivity activity, BiConsumer<Double, Double> locationCallback) {
        if (ContextCompat.checkSelfPermission(applicationContext, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            LocationServices.getFusedLocationProviderClient(activity)
                    .requestLocationUpdates(locationRequest, new LocationCallback() {
                        @Override
                        public void onLocationResult(@NonNull LocationResult locationResult) {
                            super.onLocationResult(locationResult);
                            Log.d(TAG, "Location result received.");
                            LocationServices.getFusedLocationProviderClient(activity)
                                    .removeLocationUpdates(this);
                            if (!locationResult.getLocations().isEmpty()) {
                                Log.d(TAG, "Location found.");
                                android.location.Location lastLocation = locationResult.getLastLocation();
                                handleLocationUpdate(lastLocation, locationCallback);
                            }
                        }
                    }, Looper.getMainLooper());
        } else {
            Log.d(TAG, "Location permission not granted.");
            checkLocationPermission(activity, locationCallback);
        }
    }

    private void handleLocationUpdate(android.location.Location location, BiConsumer<Double, Double> locationCallback) {
        double latitude = location.getLatitude();
        double longitude = location.getLongitude();
        String message = "Location updated: " + latitude + ", " + longitude;
        Log.d(TAG, message);
        Toast.makeText(applicationContext, message, Toast.LENGTH_SHORT).show();
        if (locationCallback != null) {
            locationCallback.accept(latitude, longitude);
        }
    }

    private void handleLocationError(String error) {
        Log.e(TAG, "Location error: " + error);
        Toast.makeText(applicationContext, "Location error: " + error, Toast.LENGTH_LONG).show();
    }

    public void onRequestPermissionsResult(AppCompatActivity activity, int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == REQUEST_CHECK_SETTINGS) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.d(TAG, "Location permission granted.");
                getCurrentLocation(activity, locationCallback);
            } else {
                Log.d(TAG, "Location permission denied.");
                handleLocationError("Location permission is required but was denied.");
            }
        }
    }

    public void onActivityResult(AppCompatActivity activity, int requestCode, int resultCode) {
        if (requestCode == REQUEST_CHECK_SETTINGS) {
            if (resultCode == AppCompatActivity.RESULT_OK) {
                Log.d(TAG, "User agreed to make required location settings changes.");
                getCurrentLocation(activity, locationCallback);
            } else {
                Log.d(TAG, "User chose not to make required location settings changes.");
                handleLocationError("Location settings are not satisfied.");
            }
        }
    }
}

