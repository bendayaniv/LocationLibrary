package com.example.locationlibrary;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Looper;
import android.provider.Settings;
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

/**
 * Singleton class to manage location-related operations throughout the app.
 */
public class MyLocation {

    private static MyLocation instance = null;
    private Context applicationContext;
    private LocationRequest locationRequest;
    private LocationManager locationManager;

    private static final int REQUEST_CHECK_SETTINGS = 1001;
    private static final int SETTINGS_REQUEST_CODE = 1002;
    private static final String TAG = "MyLocation";

    private BiConsumer<Double, Double> locationCallback;
    private AppCompatActivity activity;

    private Boolean mustToUse;

    /**
     * Private constructor to prevent instantiation.
     */
    private MyLocation() {
        // Private constructor to prevent instantiation
    }

    /**
     * Gets the singleton instance of MyLocation.
     *
     * @return The MyLocation instance.
     */
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

    /**
     * Initializes the MyLocation with the application context.
     *
     * @param application The Application instance.
     */
    public void initializeApp(Application application, Boolean mustToUse) {
        this.mustToUse = mustToUse;
        this.applicationContext = application.getApplicationContext();
        this.locationManager = (LocationManager) applicationContext.getSystemService(Context.LOCATION_SERVICE);
        initializeLocationRequest();
    }

    /**
     * Initializes the LocationRequest with default settings.
     */
    private void initializeLocationRequest() {
        locationRequest = new LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY)
                .setIntervalMillis(10000)
                .setMinUpdateIntervalMillis(5000)
                .build();
    }

    /**
     * Checks location settings and requests updates if enabled.
     *
     * @param activity         The calling activity.
     * @param locationCallback Callback to handle the location result.
     */
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

    /**
     * Checks if location services are enabled on the device.
     *
     * @return true if location is enabled, false otherwise.
     */
    public boolean isLocationEnabled() {
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
    }

    /**
     * Checks for location permission and requests it if not granted.
     *
     * @param activity         The calling activity.
     * @param locationCallback Callback to handle the location result.
     */
    private void checkLocationPermission(AppCompatActivity activity, BiConsumer<Double, Double> locationCallback) {
        if (ContextCompat.checkSelfPermission(applicationContext, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            Log.d(TAG, "Location permission not granted.");
            if (ActivityCompat.shouldShowRequestPermissionRationale(activity, Manifest.permission.ACCESS_FINE_LOCATION)) {
                showLocationPermissionRationale(activity);
            } else {
                requestLocationPermission(activity);
            }
        } else {
            Log.d(TAG, "Location permission granted.");
            getCurrentLocation(activity, locationCallback);
        }
    }

    /**
     * Shows a dialog explaining why the location permission is needed.
     *
     * @param activity The calling activity.
     */
    private void showLocationPermissionRationale(AppCompatActivity activity) {
        new AlertDialog.Builder(activity)
                .setTitle("Location Permission Needed")
                .setMessage("This app needs the Location permission to function properly. Please grant the permission.")
                .setPositiveButton("OK", (dialog, which) -> {
                    requestLocationPermission(activity);
                })
                .setNegativeButton("Cancel", (dialog, which) -> {
//                    handleLocationError("Location permission is required but was denied.");
                    checkLocationAndRequestUpdates(activity, locationCallback);
                })
                .show();
    }

    /**
     * Requests the location permission.
     *
     * @param activity The calling activity.
     */
    private void requestLocationPermission(AppCompatActivity activity) {
        ActivityCompat.requestPermissions(activity,
                new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_CHECK_SETTINGS);
    }

    /**
     * Shows a dialog to prompt the user to enable location services.
     *
     * @param activity The calling activity.
     */
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
//                    handleLocationError("Location services are required but not enabled.");
                    checkLocationAndRequestUpdates(activity, locationCallback);
                })
                .show();
    }

    /**
     * Attempts to turn on GPS by opening location settings.
     *
     * @param activity The calling activity.
     */
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
//                    handleLocationError("Unable to start location settings resolution.");
                    checkLocationAndRequestUpdates(activity, locationCallback);
                }
            } else {
//                handleLocationError("Location settings are not satisfied.");
                checkLocationAndRequestUpdates(activity, locationCallback);
            }
        });
    }

    /**
     * Requests the current location of the device.
     *
     * @param activity         The calling activity.
     * @param locationCallback Callback to handle the location result.
     */
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

    /**
     * Handles the location update by calling the callback and showing a toast.
     *
     * @param location         The updated location.
     * @param locationCallback Callback to handle the location result.
     */
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

//    /**
//     * Handles location-related errors by logging and showing a toast.
//     *
//     * @param error The error message.
//     */
//    private void handleLocationError(String error) {
//        Log.e(TAG, "Location error: " + error);
//        Toast.makeText(applicationContext, "Location error: " + error, Toast.LENGTH_LONG).show();
//        checkLocationAndRequestUpdates(activity, locationCallback);
//    }

    /**
     * Handles the result of the permission request.
     *
     * @param activity     The calling activity.
     * @param requestCode  The request code.
     * @param permissions  The requested permissions.
     * @param grantResults The grant results for the corresponding permissions.
     */
    public void onRequestPermissionsResult(AppCompatActivity activity, int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == REQUEST_CHECK_SETTINGS) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.d(TAG, "Location permission granted.");
                getCurrentLocation(activity, locationCallback);
            } else {
                if (mustToUse) {
                    Log.d(TAG, "Location permission denied.");
                    showAppSettings(activity);
                }
            }
        }
    }

    /**
     * Shows a dialog to direct the user to the app settings.
     *
     * @param activity The calling activity.
     */
    private void showAppSettings(AppCompatActivity activity) {
        new AlertDialog.Builder(activity)
                .setTitle("Permission Required")
                .setMessage("Location permission is required for this app to function properly. Please grant the permission in the app settings.")
                .setPositiveButton("Open Settings", (dialog, which) -> {
                    Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                    Uri uri = Uri.fromParts("package", activity.getPackageName(), null);
                    intent.setData(uri);
                    activity.startActivityForResult(intent, SETTINGS_REQUEST_CODE);
                })
                .setNegativeButton("Cancel", (dialog, which) -> {
//                    activity.finish(); // Close the app if user cancels
                    showAppSettings(activity);
                })
                .setCancelable(false)
                .show();
    }

    /**
     * Handles the result of the activity launched to change location settings.
     *
     * @param activity    The calling activity.
     * @param requestCode The request code.
     * @param resultCode  The result code.
     */
    public void onActivityResult(AppCompatActivity activity, int requestCode, int resultCode) {
        if (requestCode == REQUEST_CHECK_SETTINGS) {
            if (resultCode == AppCompatActivity.RESULT_OK) {
                Log.d(TAG, "User agreed to make required location settings changes.");
                getCurrentLocation(activity, locationCallback);
            } else {
                Log.d(TAG, "User chose not to make required location settings changes.");
                activity.finish(); // Close the app if user cancels
            }
        } else if (requestCode == SETTINGS_REQUEST_CODE) {
            // Check if the permission is now granted
            if (ContextCompat.checkSelfPermission(activity, Manifest.permission.ACCESS_FINE_LOCATION)
                    == PackageManager.PERMISSION_GRANTED) {
                getCurrentLocation(activity, locationCallback);
            } else {
                activity.finish(); // Close the app if permission is still not granted
            }
        }
    }
}