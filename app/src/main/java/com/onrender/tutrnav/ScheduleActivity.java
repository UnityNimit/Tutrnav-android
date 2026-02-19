package com.onrender.tutrnav;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

public class ScheduleActivity extends AppCompatActivity {

    // UI Components
    private RecyclerView rvSchedule;
    private TextView tvEmptyState; // Optional: Add this ID to activity_schedule.xml if desired

    // Data & Firebase
    private StudentScheduleAdapter adapter;
    private List<EnrollmentModel> enrollmentList = new ArrayList<>();
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_schedule);

        // 1. Window Insets (Edge-to-Edge)
        setupWindowInsets();

        // 2. Initialize Firebase
        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        // 3. Setup RecyclerView
        rvSchedule = findViewById(R.id.rvSchedule);
        rvSchedule.setLayoutManager(new LinearLayoutManager(this));

        adapter = new StudentScheduleAdapter(enrollmentList);
        rvSchedule.setAdapter(adapter);

        // 4. Load Data
        fetchEnrollments();

        // 5. Setup Bottom Navigation
        setupBottomNavigation();
    }

    private void setupWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, 0); // Leave bottom for nav
            return insets;
        });
    }

    private void fetchEnrollments() {
        if (mAuth.getCurrentUser() == null) return;

        // Fetch "approved" enrollments for the current student
        db.collection("enrollments")
                .whereEqualTo("studentId", mAuth.getCurrentUser().getUid())
                .whereEqualTo("status", "approved")
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        Toast.makeText(this, "Error loading schedule", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    if (value != null) {
                        enrollmentList.clear();
                        for (DocumentSnapshot doc : value) {
                            EnrollmentModel model = doc.toObject(EnrollmentModel.class);
                            if (model != null) {
                                enrollmentList.add(model);
                            }
                        }
                        adapter.notifyDataSetChanged();

                        // Optional: Handle empty state visibility here
                        // if(enrollmentList.isEmpty()) showEmptyState();
                    }
                });
    }

    private void setupBottomNavigation() {
        // 1. Home
        findViewById(R.id.navHome).setOnClickListener(v -> {
            startActivity(new Intent(this, StudentHomeActivity.class));
            overridePendingTransition(0, 0);
            finish();
        });

        // 2. Schedule (Current) - No Action
        // You can add visual feedback (color change) here if not handled by XML

        // 3. Map
        View navMap = findViewById(R.id.navMap);
        if (navMap != null) {
            navMap.setOnClickListener(v -> {
                startActivity(new Intent(this, MapsActivity.class));
                overridePendingTransition(0, 0);
                finish();
            });
        }

        // 4. Notifications
        View navNotif = findViewById(R.id.navNotif);
        if (navNotif != null) {
            navNotif.setOnClickListener(v -> {
                startActivity(new Intent(this, NotificationsActivity.class));
                overridePendingTransition(0, 0);
                finish();
            });
        }
    }

    // ==========================================
    //       LEGENDARY ADAPTER IMPLEMENTATION
    // ==========================================

    private class StudentScheduleAdapter extends RecyclerView.Adapter<StudentScheduleAdapter.ViewHolder> {

        private final List<EnrollmentModel> list;

        public StudentScheduleAdapter(List<EnrollmentModel> list) {
            this.list = list;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            // Inflate the legendary item_schedule.xml
            return new ViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_schedule, parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            EnrollmentModel item = list.get(position);

            // 1. Title (Subject Name)
            String title = (item.getTuitionTitle() != null) ? item.getTuitionTitle() : "Class Session";
            holder.tvSubjectName.setText(title);

            // 2. Topic / Status Details
            holder.tvTopic.setText("Enrollment Approved • Active Series");

            // 3. Teacher Info
            String tId = (item.getTeacherId() != null) ? item.getTeacherId() : "Instructor";
            if (tId.length() > 8) tId = "ID: " + tId.substring(0, 5) + "...";
            holder.tvTutorName.setText(tId);

            // 4. Time & Duration (Placeholder until data model has time)
            holder.tvTimeStart.setText("TBA");
            holder.tvDuration.setText("60 min");

            // 5. Status Chip Styling
            holder.tvStatus.setText("ENROLLED");
            holder.tvStatus.setTextColor(Color.parseColor("#FFCA28")); // Gold
            holder.tvStatus.setBackgroundResource(R.drawable.bg_status_chip);

            // 6. Dynamic Color Strip (Hash subject name for consistent color)
            int color = getColorForSubject(title);
            holder.viewColorBar.setBackgroundColor(color);

            // 7. Profile Image (Placeholder)
            Glide.with(holder.itemView.getContext())
                    .load(R.mipmap.ic_launcher)
                    .circleCrop()
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .into(holder.imgTutor);

            // 8. Location
            holder.tvLocation.setText("Online / Campus");

            // 9. Button Action
            holder.btnAction.setOnClickListener(v ->
                    Toast.makeText(ScheduleActivity.this, "Opening Map for " + title, Toast.LENGTH_SHORT).show()
            );
        }

        @Override
        public int getItemCount() { return list.size(); }

        // Helper to generate consistent colors based on text
        private int getColorForSubject(String subject) {
            if (subject == null) return Color.GRAY;
            int hash = subject.hashCode();
            int[] colors = {
                    Color.parseColor("#42A5F5"), // Blue
                    Color.parseColor("#66BB6A"), // Green
                    Color.parseColor("#FFCA28"), // Gold
                    Color.parseColor("#AB47BC"), // Purple
                    Color.parseColor("#FF7043")  // Orange
            };
            return colors[Math.abs(hash) % colors.length];
        }

        // ViewHolder mapping to item_schedule.xml
        class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvSubjectName, tvTopic, tvTutorName, tvTimeStart, tvDuration, tvStatus, tvLocation;
            ImageView imgTutor;
            View viewColorBar;
            MaterialButton btnAction;

            public ViewHolder(@NonNull View v) {
                super(v);
                tvSubjectName = v.findViewById(R.id.tvSubjectName);
                tvTopic       = v.findViewById(R.id.tvTopic);
                tvTutorName   = v.findViewById(R.id.tvTutorName);
                tvTimeStart   = v.findViewById(R.id.tvTimeStart);
                tvDuration    = v.findViewById(R.id.tvDuration);
                tvStatus      = v.findViewById(R.id.tvStatus);
                tvLocation    = v.findViewById(R.id.tvLocation);
                imgTutor      = v.findViewById(R.id.imgTutor);
                viewColorBar  = v.findViewById(R.id.viewColorBar);
                btnAction     = v.findViewById(R.id.btnAction);
            }
        }
    }
}