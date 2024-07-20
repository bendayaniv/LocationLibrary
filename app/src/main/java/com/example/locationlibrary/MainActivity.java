package com.example.locationlibrary;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;

import com.example.locationlibrary.MyLocation;

/**
 * MainActivity class that demonstrates the use of MyLocation library.
 */
public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    private TextView statusTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        statusTextView = findViewById(R.id.status_text);

        Button btnGetLocation = findViewById(R.id.btn_get_location);
        btnGetLocation.setOnClickListener(v -> {
            Log.d(TAG, "Get Location button clicked");
            updateStatus("Starting location request process...");
            MyLocation.getInstance().checkLocationAndRequestUpdates(this, this::updateLocationStatus);
        });

        requestLocationUpdates();
    }

    private void requestLocationUpdates() {
        MyLocation.getInstance().checkLocationAndRequestUpdates(this, this::updateLocationStatus);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Log.d(TAG, "onActivityResult: requestCode=" + requestCode + ", resultCode=" + resultCode);
        MyLocation.getInstance().onActivityResult(this, requestCode, resultCode);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        MyLocation.getInstance().onRequestPermissionsResult(this, requestCode, permissions, grantResults);
    }

    /**
     * Updates the location status in the UI.
     *
     * @param latitude  The latitude of the current location.
     * @param longitude The longitude of the current location.
     */
    private void updateLocationStatus(Double latitude, Double longitude) {
        String message = String.format("Location obtained: %.6f, %.6f", latitude, longitude);
        Log.d(TAG, message);
        updateStatus(message);
    }

    /**
     * Updates the status TextView in the UI.
     *
     * @param status The status message to display.
     */
    private void updateStatus(String status) {
        runOnUiThread(() -> statusTextView.setText(status));
    }
}