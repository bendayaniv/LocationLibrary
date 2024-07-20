# MyLocation Library

## Introduction
MyLocation is a lightweight Android library that simplifies the process of requesting and handling location updates in Android applications. It manages location permissions, GPS settings, and provides an easy-to-use interface for obtaining the device's current location.

## Features
- Singleton pattern for easy integration
- Handles runtime permissions for location access
- Manages GPS settings and prompts user to enable if necessary
- Provides callback mechanism for location updates
- Configurable to either require location permission or allow optional use

## Installation

In setting.gradle.kts add the following line
```java
dependencyResolutionManagement {
    repositories {
        ...
        mavenCentral()
        maven { url = uri("https://jitpack.io") }
        ...
    }
}
```

In build.gradle.kts of the app add the following line
```java
dependencies {
    ...
    implementation("com.github.bendayaniv:LocationLibrary:1.00.03")
    ...
}
```

## Usage

### Initialize MyLocation

In your Application class:
```java
public class MyApp extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        MyLocation.getInstance().initializeApp(this, true); // Set to true if location is required, false if optional
    }
}
```

### Configure AndroidManifest.xml

Add the following permissions to your manifest:
```java
<uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />
<uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION" />
```

### Request Location Updates in an Activity

```java
public class MainActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        requestLocationUpdates();
    }

    private void requestLocationUpdates() {
        MyLocation.getInstance().checkLocationAndRequestUpdates(this, this::handleLocationUpdate);
    }

    private void handleLocationUpdate(Double latitude, Double longitude) {
        // Handle the location update here
        Log.d("MainActivity", "Location update: " + latitude + ", " + longitude);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        MyLocation.getInstance().onActivityResult(this, requestCode, resultCode);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        MyLocation.getInstance().onRequestPermissionsResult(this, requestCode, permissions, grantResults);
    }
}
```

## Methods

- getInstance(): Returns the singleton instance of MyLocation.
- initializeApp(Application application, Boolean mustToUse): Initializes the MyLocation instance.
- checkLocationAndRequestUpdates(AppCompatActivity activity, BiConsumer<Double, Double> locationCallback): Checks location settings and requests updates.
- isLocationEnabled(): Checks if location services are enabled on the device.
- onRequestPermissionsResult(AppCompatActivity activity, int requestCode, String[] permissions, int[] grantResults): Handles the result of permission requests.
- onActivityResult(AppCompatActivity activity, int requestCode, int resultCode): Handles the result of activities launched for location settings.

## License
This project is licensed under the MIT License - see the LICENSE.md file for details.

