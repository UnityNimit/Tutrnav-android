package com.onrender.tutrnav;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Build; // Import Build
import android.os.Bundle;
import android.text.util.Linkify;
import android.view.WindowManager; // Import WindowManager
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.imageview.ShapeableImageView;

import java.util.HashSet;
import java.util.Set;

public class NotificationDetailActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notification_detail);

        // 1. APPLY NATIVE BLUR (Android 12+)
        applyNativeBlur();

        // 2. Initialize Views
        ShapeableImageView imgTeacher = findViewById(R.id.imgTeacherDetail);
        TextView tvTitle = findViewById(R.id.tvDetailTitle);
        TextView tvTuition = findViewById(R.id.tvDetailTuition);
        TextView tvBody = findViewById(R.id.tvDetailBody);
        TextView tvDate = findViewById(R.id.tvDetailDate);
        TextView tvType = findViewById(R.id.tvDetailType);
        ImageView btnDismiss = findViewById(R.id.btnDismiss);
        MaterialButton btnDelete = findViewById(R.id.btnDeleteNotif);

        // 3. Retrieve Data
        String id = getIntent().getStringExtra("id");
        String title = getIntent().getStringExtra("title");
        String tuition = getIntent().getStringExtra("tuition");
        String body = getIntent().getStringExtra("body");
        String time = getIntent().getStringExtra("time");
        String type = getIntent().getStringExtra("type");
        String photoUrl = getIntent().getStringExtra("teacherPhoto");

        // 4. Bind Data
        tvTitle.setText(title != null ? title : "Teacher");
        tvTuition.setText(tuition != null ? tuition : "Class Update");
        tvBody.setText(body != null ? body : "");
        tvDate.setText(time != null ? time : "");

        Linkify.addLinks(tvBody, Linkify.WEB_URLS);

        if (photoUrl != null && !photoUrl.isEmpty()) {
            Glide.with(this).load(photoUrl).circleCrop().into(imgTeacher);
        } else {
            imgTeacher.setImageResource(R.mipmap.ic_launcher);
        }

        // Handle Badge Type
        if (type == null) type = "NORMAL";
        tvType.setText(type);

        switch (type) {
            case "FEE":
                tvType.setTextColor(Color.parseColor("#4CAF50"));
                tvType.setBackgroundResource(R.drawable.bg_circle_dark);
                break;
            case "IMPORTANT":
                tvType.setTextColor(Color.parseColor("#FF5252"));
                break;
            default:
                tvType.setTextColor(Color.parseColor("#2196F3"));
                break;
        }

        // Click Listeners
        btnDismiss.setOnClickListener(v -> finish());

        btnDelete.setOnClickListener(v -> {
            if (id != null) {
                dismissNotificationLocal(id);
                Toast.makeText(this, "Notification Removed", Toast.LENGTH_SHORT).show();
                finish();
            }
        });
    }

    private void applyNativeBlur() {
        // This only works on Android 12 (API 31) and above.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            try {
                // Radius: 1 to 150. 30 is a nice frosted glass effect.
                getWindow().setBackgroundBlurRadius(30);
                // Ensure flags are set to allow the blur to happen
                getWindow().addFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND);
            } catch (Exception e) {
                // Fallback for devices that might throw errors on this API
            }
        } else {
            // FALLBACK FOR OLDER PHONES:
            // Since they can't blur across activities, we just dim the background
            // so the text is readable.
            getWindow().setDimAmount(0.6f);
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
        }
    }

    private void dismissNotificationLocal(String id) {
        SharedPreferences prefs = getSharedPreferences("Notifications", Context.MODE_PRIVATE);
        Set<String> dismissed = new HashSet<>(prefs.getStringSet("dismissed", new HashSet<>()));
        dismissed.add(id);
        prefs.edit().putStringSet("dismissed", dismissed).apply();
    }
}