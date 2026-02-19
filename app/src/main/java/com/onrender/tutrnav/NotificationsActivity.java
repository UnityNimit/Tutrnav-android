package com.onrender.tutrnav;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
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
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class NotificationsActivity extends AppCompatActivity {

    private RecyclerView rvNotifications;
    private NotificationAdapter adapter;
    private List<MessageModel> notificationList = new ArrayList<>();

    private FirebaseFirestore db;
    private String currentUserId;
    private Set<String> dismissedIds = new HashSet<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_notifications);

        // Handle System Insets (Notches/Status Bars)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, 0);
            return insets;
        });

        // Initialize Firebase
        db = FirebaseFirestore.getInstance();
        currentUserId = FirebaseAuth.getInstance().getUid();

        // Initialize UI
        initViews();
        setupNavigation();
        loadDismissedIds();

        // Fetch Data
        if (currentUserId != null) {
            fetchEnrollmentsThenMessages();
        } else {
            Toast.makeText(this, "Please log in to see notifications", Toast.LENGTH_SHORT).show();
        }
    }

    private void initViews() {
        rvNotifications = findViewById(R.id.rvNotifications);
        rvNotifications.setLayoutManager(new LinearLayoutManager(this));
        adapter = new NotificationAdapter(notificationList);
        rvNotifications.setAdapter(adapter);
    }

    private void loadDismissedIds() {
        SharedPreferences prefs = getSharedPreferences("Notifications", Context.MODE_PRIVATE);
        dismissedIds = prefs.getStringSet("dismissed", new HashSet<>());
    }

    private void fetchEnrollmentsThenMessages() {
        // 1. Find which tuitions the student is APPROVED in
        db.collection("enrollments")
                .whereEqualTo("studentId", currentUserId)
                .whereEqualTo("status", "approved")
                .addSnapshotListener((value, error) -> {
                    if (error != null || value == null) return;

                    List<String> tuitionIds = new ArrayList<>();
                    for (DocumentSnapshot doc : value) {
                        String tId = doc.getString("tuitionId");
                        if (tId != null) tuitionIds.add(tId);
                    }

                    if (!tuitionIds.isEmpty()) {
                        listenForRealTimeMessages(tuitionIds);
                    }
                });
    }

    private void listenForRealTimeMessages(List<String> tuitionIds) {
        // 2. Listen for any messages/broadcasts sent to these tuitions
        db.collection("messages")
                .whereIn("tuitionId", tuitionIds)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .addSnapshotListener((value, error) -> {
                    if (error != null || value == null) return;

                    notificationList.clear();
                    for (DocumentSnapshot doc : value) {
                        MessageModel msg = doc.toObject(MessageModel.class);
                        if (msg != null) {
                            msg.setMessageId(doc.getId());
                            // Only add if user hasn't dismissed it locally
                            if (!dismissedIds.contains(msg.getMessageId())) {
                                notificationList.add(msg);
                            }
                        }
                    }
                    adapter.notifyDataSetChanged();
                });
    }

    private void setupNavigation() {
        findViewById(R.id.navHome).setOnClickListener(v -> {
            startActivity(new Intent(this, StudentHomeActivity.class));
            finish();
        });
        findViewById(R.id.navSchedule).setOnClickListener(v -> {
            startActivity(new Intent(this, ScheduleActivity.class));
            finish();
        });
        findViewById(R.id.navMap).setOnClickListener(v -> {
            startActivity(new Intent(this, MapsActivity.class));
            finish();
        });
    }

    // ==========================================
    // RECYCLERVIEW ADAPTER (Legendary Perfection)
    // ==========================================
    public class NotificationAdapter extends RecyclerView.Adapter<NotificationAdapter.ViewHolder> {
        List<MessageModel> items;

        public NotificationAdapter(List<MessageModel> items) {
            this.items = items;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_notification, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            MessageModel item = items.get(position);

            holder.tvTitle.setText(item.getSenderName());
            holder.tvTuition.setText(item.getTuitionTitle());
            holder.tvBody.setText(item.getText());

            // 1. Formatted Timestamp
            if (item.getTimestamp() != null) {
                SimpleDateFormat sdf = new SimpleDateFormat("hh:mm a, dd MMM", Locale.getDefault());
                holder.tvTime.setText(sdf.format(item.getTimestamp().toDate()));
            }

            // 2. Teacher Profile Image
            Glide.with(holder.itemView.getContext())
                    .load(item.getTeacherPhoto())
                    .placeholder(R.mipmap.ic_launcher)
                    .circleCrop()
                    .into(holder.imgTeacher);

            // 3. Symbolic Logic (Icon & Color based on type)
            setupTypeUI(holder, item.getType());

            // 4. Click to See Full Details
            holder.itemView.setOnClickListener(v -> {
                Intent intent = new Intent(NotificationsActivity.this, NotificationDetailActivity.class);
                intent.putExtra("id", item.getMessageId());
                intent.putExtra("title", item.getSenderName());
                intent.putExtra("tuition", item.getTuitionTitle());
                intent.putExtra("body", item.getText());
                intent.putExtra("time", holder.tvTime.getText().toString());
                intent.putExtra("type", item.getType());
                startActivity(intent);
            });
        }

        private void setupTypeUI(ViewHolder holder, String type) {
            if (type == null) type = "NORMAL";

            switch (type) {
                case "FEE":
                    holder.imgIcon.setImageResource(android.R.drawable.ic_menu_save); // Save/Money icon
                    holder.imgIcon.setColorFilter(0xFF4CAF50); // Green
                    break;
                case "IMPORTANT":
                    holder.imgIcon.setImageResource(android.R.drawable.stat_sys_warning); // Warning icon
                    holder.imgIcon.setColorFilter(0xFFF44336); // Red
                    break;
                default: // NORMAL
                    holder.imgIcon.setImageResource(android.R.drawable.ic_menu_info_details);
                    holder.imgIcon.setColorFilter(0xFF2196F3); // Blue
                    break;
            }
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvTitle, tvTuition, tvBody, tvTime;
            ImageView imgTeacher, imgIcon;

            public ViewHolder(@NonNull View itemView) {
                super(itemView);
                tvTitle = itemView.findViewById(R.id.tvNotifTitle);
                tvTuition = itemView.findViewById(R.id.tvTuitionName); // From new Legendary item XML
                tvBody = itemView.findViewById(R.id.tvNotifBody);
                tvTime = itemView.findViewById(R.id.tvTime); // Updated from tvNotifDate
                imgTeacher = itemView.findViewById(R.id.imgTeacher); // From new Legendary item XML
                imgIcon = itemView.findViewById(R.id.imgTypeIcon); // From new Legendary item XML
            }
        }
    }
}