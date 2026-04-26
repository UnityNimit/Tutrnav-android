package com.onrender.tutrnav.core;

import android.app.Application;
import com.cloudinary.android.MediaManager;
import java.util.HashMap;
import java.util.Map;

public class TutrnavApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();

        // Initialize Cloudinary ONCE when the app starts
        try {
            Map<String, String> config = new HashMap<>();
            config.put("cloud_name", "drukt0qau"); // <--- REPLACE THIS LATER
            config.put("secure", "true");
            MediaManager.init(this, config);
        } catch (Exception e) {
            // Already initialized, ignore
        }
    }
}