package com.onrender.tutrnav;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.text.util.Linkify;
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

        // 1. Initialize Views
        ShapeableImageView imgTeacher = findViewById(R.id.imgTeacherDetail);
        TextView tvTitle = findViewById(R.id.tvDetailTitle);
        TextView tvTuition = findViewById(R.id.tvDetailTuition);
        TextView tvBody = findViewById(R.id.tvDetailBody);
        TextView tvDate = findViewById(R.id.tvDetailDate);
        TextView tvType = findViewById(R.id.tvDetailType);
        ImageView btnDismiss = findViewById(R.id.btnDismiss);
        MaterialButton btnDelete = findViewById(R.id.btnDeleteNotif);

        // 2. Retrieve Data from Intent
        String id = getIntent().getStringExtra("id");
        String title = getIntent().getStringExtra("title");
        String tuition = getIntent().getStringExtra("tuition");
        String body = getIntent().getStringExtra("body");
        String time = getIntent().getStringExtra("time");
        String type = getIntent().getStringExtra("type");
        String photoUrl = getIntent().getStringExtra("teacherPhoto");

        // 3. Bind Text Data
        tvTitle.setText(title != null ? title : "Teacher");
        tvTuition.setText(tuition != null ? tuition : "Class Update");
        tvBody.setText(body != null ? body : "");
        tvDate.setText(time != null ? time : "");

        // 4. Legendary Feature: Auto-Linkify
        // This makes "https://zoom.us/..." clickable immediately
        Linkify.addLinks(tvBody, Linkify.WEB_URLS);

        // 5. Load Teacher Profile Picture
        if (photoUrl != null && !photoUrl.isEmpty()) {
            Glide.with(this)
                    .load(photoUrl)
                    .placeholder(R.mipmap.ic_launcher)
                    .error(R.mipmap.ic_launcher)
                    .circleCrop()
                    .into(imgTeacher);
        } else {
            imgTeacher.setImageResource(R.mipmap.ic_launcher);
        }

        // 6. Handle Badge Type (Color & Text)
        if (type == null) type = "NORMAL";
        tvType.setText(type);

        switch (type) {
            case "FEE":
                tvType.setTextColor(Color.parseColor("#4CAF50")); // Green
                tvType.setBackgroundResource(R.drawable.bg_circle_dark); // Ensure you have this drawable or generic
                break;
            case "IMPORTANT":
                tvType.setTextColor(Color.parseColor("#FF5252")); // Red
                break;
            default:
                tvType.setTextColor(Color.parseColor("#2196F3")); // Blue
                break;
        }

        // 7. Click Listeners
        btnDismiss.setOnClickListener(v -> finish());

        btnDelete.setOnClickListener(v -> {
            if (id != null) {
                dismissNotificationLocal(id);
                Toast.makeText(this, "Notification Removed", Toast.LENGTH_SHORT).show();
                finish(); // Close activity and go back to list
            }
        });
    }

    // Stores the ID in SharedPreferences so it doesn't show up in the list again
    private void dismissNotificationLocal(String id) {
        SharedPreferences prefs = getSharedPreferences("Notifications", Context.MODE_PRIVATE);
        Set<String> dismissed = new HashSet<>(prefs.getStringSet("dismissed", new HashSet<>()));
        dismissed.add(id);
        prefs.edit().putStringSet("dismissed", dismissed).apply();
    }
}