package com.onrender.tutrnav;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
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

public class NotificationsFragment extends Fragment {

    private RecyclerView rvNotifications;
    private TextView tvEmptyState;
    private ProgressBar progressBar;

    private NotificationAdapter adapter;
    private List<MessageModel> notificationList = new ArrayList<>();

    private FirebaseFirestore db;
    private String currentUserId;
    private Set<String> dismissedIds = new HashSet<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_notifications, container, false);

        // Init Firebase
        db = FirebaseFirestore.getInstance();
        currentUserId = FirebaseAuth.getInstance().getUid();

        // Load Local Data
        loadDismissedIds();

        // Init Views
        rvNotifications = view.findViewById(R.id.rvNotifications);

        // You should add these IDs to your fragment_notifications.xml for full perfection
        // If they don't exist yet, the code handles null safely or you can add them.
        tvEmptyState = view.findViewById(R.id.tvEmptyState);
        progressBar = view.findViewById(R.id.progressBar);

        rvNotifications.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new NotificationAdapter(notificationList);
        rvNotifications.setAdapter(adapter);

        if (currentUserId != null) {
            if(progressBar != null) progressBar.setVisibility(View.VISIBLE);
            fetchEnrollmentsAndMessages();
        } else {
            if(progressBar != null) progressBar.setVisibility(View.GONE);
            updateEmptyState();
        }

        return view;
    }

    private void loadDismissedIds() {
        if (getActivity() == null) return;
        SharedPreferences prefs = getActivity().getSharedPreferences("Notifications", Context.MODE_PRIVATE);
        dismissedIds = prefs.getStringSet("dismissed", new HashSet<>());
    }

    private void fetchEnrollmentsAndMessages() {
        // 1. Get Enrolled Classes
        db.collection("enrollments")
                .whereEqualTo("studentId", currentUserId)
                .whereEqualTo("status", "approved")
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        if(progressBar != null) progressBar.setVisibility(View.GONE);
                        return;
                    }

                    if (value == null || value.isEmpty()) {
                        if(progressBar != null) progressBar.setVisibility(View.GONE);
                        updateEmptyState();
                        return;
                    }

                    List<String> tuitionIds = new ArrayList<>();
                    for (DocumentSnapshot doc : value) {
                        String tId = doc.getString("tuitionId");
                        if (tId != null) tuitionIds.add(tId);
                    }

                    if (!tuitionIds.isEmpty()) {
                        listenForMessages(tuitionIds);
                    } else {
                        if(progressBar != null) progressBar.setVisibility(View.GONE);
                        updateEmptyState();
                    }
                });
    }

    private void listenForMessages(List<String> tuitionIds) {
        // 2. Get Messages for those classes
        // NOTE: Firestore limits 'whereIn' to 10 items. In a production app, handle batches.
        db.collection("messages")
                .whereIn("tuitionId", tuitionIds)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .addSnapshotListener((value, error) -> {
                    if(progressBar != null) progressBar.setVisibility(View.GONE);

                    if (error != null || value == null) return;

                    notificationList.clear();
                    for (DocumentSnapshot doc : value) {
                        MessageModel msg = doc.toObject(MessageModel.class);
                        if (msg != null && !dismissedIds.contains(doc.getId())) {
                            msg.setMessageId(doc.getId());
                            notificationList.add(msg);
                        }
                    }
                    adapter.notifyDataSetChanged();
                    updateEmptyState();
                });
    }

    private void updateEmptyState() {
        if (tvEmptyState != null) {
            tvEmptyState.setVisibility(notificationList.isEmpty() ? View.VISIBLE : View.GONE);
            rvNotifications.setVisibility(notificationList.isEmpty() ? View.GONE : View.VISIBLE);
        }
    }

    // --- ADAPTER ---
    private class NotificationAdapter extends RecyclerView.Adapter<NotificationAdapter.ViewHolder> {
        List<MessageModel> items;

        public NotificationAdapter(List<MessageModel> items) { this.items = items; }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new ViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_notification, parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            MessageModel item = items.get(position);

            holder.tvTitle.setText(item.getSenderName());
            holder.tvTuition.setText(item.getTuitionTitle());
            holder.tvBody.setText(item.getText());

            // 1. Format Time
            if (item.getTimestamp() != null) {
                SimpleDateFormat sdf = new SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault());
                holder.tvTime.setText(sdf.format(item.getTimestamp().toDate()));
            }

            // 2. Load Profile Picture (THE CRITICAL FIX)
            if (item.getTeacherPhoto() != null && !item.getTeacherPhoto().isEmpty()) {
                Glide.with(holder.itemView.getContext())
                        .load(item.getTeacherPhoto())
                        .diskCacheStrategy(DiskCacheStrategy.ALL) // Cache image for speed
                        .placeholder(R.mipmap.ic_launcher) // Show while loading
                        .error(R.mipmap.ic_launcher) // Show if URL fails
                        .circleCrop()
                        .into(holder.imgTeacher);
            } else {
                // Fallback if no URL
                holder.imgTeacher.setImageResource(R.mipmap.ic_launcher);
            }

            // 3. Set Type Icon
            String type = item.getType();
            if (type == null) type = "NORMAL";

            switch (type) {
                case "FEE":
                    holder.imgIcon.setImageResource(android.R.drawable.ic_menu_save);
                    holder.imgIcon.setColorFilter(0xFF4CAF50); // Green
                    break;
                case "IMPORTANT":
                    holder.imgIcon.setImageResource(android.R.drawable.stat_sys_warning);
                    holder.imgIcon.setColorFilter(0xFFF44336); // Red
                    break;
                default:
                    holder.imgIcon.setImageResource(android.R.drawable.ic_menu_info_details);
                    holder.imgIcon.setColorFilter(0xFF2196F3); // Blue
                    break;
            }

            // 4. Click Listener
            holder.itemView.setOnClickListener(v -> {
                Intent intent = new Intent(getContext(), NotificationDetailActivity.class);
                intent.putExtra("title", item.getSenderName());
                intent.putExtra("tuition", item.getTuitionTitle());
                intent.putExtra("body", item.getText());
                intent.putExtra("type", item.getType());
                intent.putExtra("time", holder.tvTime.getText().toString());
                intent.putExtra("id", item.getMessageId());

                // CRITICAL: Pass the photo URL to the detail screen too!
                intent.putExtra("teacherPhoto", item.getTeacherPhoto());

                startActivity(intent);
            });
        }

        @Override
        public int getItemCount() { return items.size(); }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvTitle, tvTuition, tvBody, tvTime;
            ImageView imgTeacher, imgIcon;

            public ViewHolder(@NonNull View v) {
                super(v);
                tvTitle = v.findViewById(R.id.tvNotifTitle);
                tvTuition = v.findViewById(R.id.tvTuitionName);
                tvBody = v.findViewById(R.id.tvNotifBody);
                tvTime = v.findViewById(R.id.tvTime);
                imgTeacher = v.findViewById(R.id.imgTeacher);
                imgIcon = v.findViewById(R.id.imgTypeIcon);
            }
        }
    }
}