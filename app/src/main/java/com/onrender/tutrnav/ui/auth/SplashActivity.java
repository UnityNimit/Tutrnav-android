package com.onrender.tutrnav.ui.auth;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.onrender.tutrnav.R;
import com.onrender.tutrnav.ui.student.StudentHomeActivity;
import com.onrender.tutrnav.ui.teacher.TeacherHomeActivity;

@SuppressLint("CustomSplashScreen")
public class SplashActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_auth_splash);

        // Wait 2 seconds, then route perfectly
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            if (FirebaseAuth.getInstance().getCurrentUser() != null) {
                // Logged In: Check role and go to correct Dashboard
                SharedPreferences prefs = getSharedPreferences("TutrnavPrefs", MODE_PRIVATE);
                String role = prefs.getString("userType", "student");
                startActivity(new Intent(this, "teacher".equals(role) ? TeacherHomeActivity.class : StudentHomeActivity.class));
            } else {
                // Not Logged In: Go to our new Unified Auth Screen
                startActivity(new Intent(this, AuthActivity.class));
            }
            finish();
        }, 2000);
    }
}