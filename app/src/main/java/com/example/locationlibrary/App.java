package com.example.locationlibrary;

import android.app.Application;
import com.example.locationlibrary.MyLocation;

public class App extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        MyLocation.getInstance().initializeApp(this);
    }
}
