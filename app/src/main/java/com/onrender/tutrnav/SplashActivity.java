package com.onrender.tutrnav;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

@SuppressLint("CustomSplashScreen")
public class SplashActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 1. Enable modern Edge-to-Edge display
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_splash);

        // Handle window insets to prevent content form being hidden behind system bars
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(android.R.id.content), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // 2. Wait 2 seconds (branding display), then route the user
        new Handler(Looper.getMainLooper()).postDelayed(() -> {

            // Get current Firebase User
            FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();

            // Get local preferences to check User Type ("student" or "teacher")
            SharedPreferences prefs = getSharedPreferences("TutrnavPrefs", MODE_PRIVATE);
            String userType = prefs.getString("userType", "");

            if (currentUser != null) {
                // --- CASE A: User is Logged In ---

                // Route to appropriate Home Screen based on saved role
                if ("teacher".equalsIgnoreCase(userType)) {
                    startActivity(new Intent(SplashActivity.this, TeacherHomeActivity.class));
                } else {
                    // Default to Student Dashboard if "student" or missing
                    startActivity(new Intent(SplashActivity.this, StudentHomeActivity.class));
                }
            } else {
                // --- CASE B: User is NOT Logged In ---

                if (userType.isEmpty()) {
                    // Sub-case: First time user (No role selected) -> Go to Onboarding
                    startActivity(new Intent(SplashActivity.this, OnboardingActivity.class));
                } else {
                    // Sub-case: Returning user (Role selected, but logged out) -> Go to Auth
                    startActivity(new Intent(SplashActivity.this, AuthActivity.class));
                }
            }

            // Close SplashActivity so the user cannot press Back to return to it
            finish();

        }, 2000); // 2000ms delay
    }
}