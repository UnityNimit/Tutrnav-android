package com.onrender.tutrnav;

import android.app.AlertDialog;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class TeacherScheduleFragment extends Fragment {

    // --- UI Components ---
    private RecyclerView rvSchedule;
    private ChipGroup chipGroup;
    private ExtendedFloatingActionButton fabBroadcast;
    private TextView tvEmptyState;

    // --- Data ---
    private TeacherScheduleAdapter adapter;
    private List<EnrollmentModel> allEnrollments = new ArrayList<>();
    private List<EnrollmentModel> filteredList = new ArrayList<>();

    // --- Firebase ---
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;

    // --- State ---
    private String selectedTuitionId = "ALL";
    private String selectedTuitionTitle = "All Classes";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_teacher_schedule, container, false);

        // 1. Initialize Views
        initViews(view);

        // 2. Setup Firebase
        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        // 3. Setup RecyclerView
        setupRecyclerView();

        // 4. Load Data
        fetchMyStudents();

        // 5. Setup Broadcast Button
        setupBroadcastButton();

        return view;
    }

    private void initViews(View view) {
        rvSchedule = view.findViewById(R.id.rvTeacherSchedule);
        chipGroup = view.findViewById(R.id.chipGroupTuitions);
        fabBroadcast = view.findViewById(R.id.fabBroadcast);
        // Add a TextView in your XML with id tvEmptyState if you want an empty state message
        // tvEmptyState = view.findViewById(R.id.tvEmptyState);
    }

    private void setupRecyclerView() {
        rvSchedule.setLayoutManager(new LinearLayoutManager(getContext()));
        // Pass a click listener to open message dialog for specific student
        adapter = new TeacherScheduleAdapter(getContext(), filteredList, this::showComposeDialog);
        rvSchedule.setAdapter(adapter);
    }

    private void setupBroadcastButton() {
        fabBroadcast.setOnClickListener(v -> {
            if (filteredList.isEmpty()) {
                Toast.makeText(getContext(), "No students found.", Toast.LENGTH_SHORT).show();
            } else if ("ALL".equals(selectedTuitionId)) {
                Toast.makeText(getContext(), "Select a specific class to broadcast.", Toast.LENGTH_SHORT).show();
            } else {
                // Broadcast to the selected class (pass null as target student)
                showComposeDialog(null);
            }
        });
    }

    // --- FIREBASE DATA FETCHING ---

    private void fetchMyStudents() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) return;

        db.collection("enrollments")
                .whereEqualTo("teacherId", currentUser.getUid())
                .whereEqualTo("status", "approved")
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        Toast.makeText(getContext(), "Error loading students", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    if (value != null) {
                        allEnrollments.clear();
                        for (DocumentSnapshot doc : value) {
                            EnrollmentModel model = doc.toObject(EnrollmentModel.class);
                            if (model != null) {
                                allEnrollments.add(model);
                            }
                        }
                        updateChips();
                        filterList(selectedTuitionId); // Refresh list
                    }
                });
    }

    // --- CHIP FILTER LOGIC ---

    private void updateChips() {
        chipGroup.removeAllViews();

        Set<String> tuitionIds = new HashSet<>();
        Map<String, String> idToTitleMap = new HashMap<>();

        // Collect unique Tuition IDs
        for(EnrollmentModel m : allEnrollments) {
            if(m.getTuitionId() != null) {
                tuitionIds.add(m.getTuitionId());
                idToTitleMap.put(m.getTuitionId(), m.getTuitionTitle());
            }
        }

        // Add "ALL" Chip if there's more than 1 class
        if (tuitionIds.size() > 1) {
            addChip("ALL", "All Classes");
        }

        // Add Chips for each class
        for(String tId : tuitionIds) {
            String title = idToTitleMap.get(tId);
            addChip(tId, (title != null) ? title : "Class " + tId.substring(0, 4));
        }
    }

    private void addChip(String id, String label) {
        Chip chip = new Chip(getContext());
        chip.setText(label);
        chip.setCheckable(true);
        chip.setClickable(true);
        chip.setChipBackgroundColorResource(android.R.color.white);

        // Handle Selection
        chip.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                selectedTuitionId = id;
                selectedTuitionTitle = label;
                filterList(id);
            }
        });

        // Default Selection
        if (id.equals(selectedTuitionId)) {
            chip.setChecked(true);
        }

        chipGroup.addView(chip);
    }

    private void filterList(String tuitionId) {
        filteredList.clear();

        if ("ALL".equals(tuitionId)) {
            filteredList.addAll(allEnrollments);
            fabBroadcast.setVisibility(View.GONE); // Hide broadcast on "ALL" view
        } else {
            for (EnrollmentModel m : allEnrollments) {
                if (tuitionId.equals(m.getTuitionId())) {
                    filteredList.add(m);
                }
            }
            fabBroadcast.setVisibility(View.VISIBLE); // Show broadcast for specific class
        }

        adapter.notifyDataSetChanged();
    }

    // --- MESSAGING LOGIC ---

    /**
     * Shows dialog to send message.
     * @param targetStudent If null, it's a broadcast to the selected class.
     */
    private void showComposeDialog(@Nullable EnrollmentModel targetStudent) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_send_message, null);
        builder.setView(dialogView);

        AlertDialog dialog = builder.create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        TextView title = dialogView.findViewById(R.id.tvDialogTitle);
        EditText etMsg = dialogView.findViewById(R.id.etMessage);
        MaterialButton btnSend = dialogView.findViewById(R.id.btnSend);
        MaterialButton btnCancel = dialogView.findViewById(R.id.btnCancel);

        // Set Title
        if (targetStudent == null) {
            title.setText("Broadcast to: " + selectedTuitionTitle);
        } else {
            title.setText("Message to: " + targetStudent.getStudentName()); // Ensure getStudentName exists
        }

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        btnSend.setOnClickListener(v -> {
            String msgText = etMsg.getText().toString().trim();
            if (TextUtils.isEmpty(msgText)) {
                etMsg.setError("Message cannot be empty");
                return;
            }
            sendMessage(msgText, targetStudent, dialog);
        });

        dialog.show();
    }

    private void sendMessage(String text, @Nullable EnrollmentModel target, AlertDialog dialog) {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) return;

        // Prepare Message Data
        Map<String, Object> msgMap = new HashMap<>();
        msgMap.put("text", text);
        msgMap.put("senderId", user.getUid());
        msgMap.put("senderName", user.getDisplayName());
        msgMap.put("senderPhoto", (user.getPhotoUrl() != null) ? user.getPhotoUrl().toString() : "");
        msgMap.put("timestamp", new Date());

        // Target Logic
        if (target != null) {
            // Private Message to specific student
            msgMap.put("studentId", target.getStudentId()); // Ensure getStudentId exists
            msgMap.put("tuitionId", target.getTuitionId());
            msgMap.put("type", "PRIVATE");
        } else {
            // Broadcast to the whole class
            msgMap.put("tuitionId", selectedTuitionId);
            msgMap.put("tuitionTitle", selectedTuitionTitle);
            msgMap.put("type", "BROADCAST");
        }

        db.collection("messages").add(msgMap)
                .addOnSuccessListener(docRef -> {
                    Toast.makeText(getContext(), "Message Sent!", Toast.LENGTH_SHORT).show();
                    dialog.dismiss();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "Failed to send: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    public interface OnStudentClickListener {
        void onClick(EnrollmentModel student);
    }

    // --- ADAPTER CLASS (Using Legendary Layout) ---

    public static class TeacherScheduleAdapter extends RecyclerView.Adapter<TeacherScheduleAdapter.ViewHolder> {

        private final List<EnrollmentModel> items;
        private final OnStudentClickListener listener;
        private final android.content.Context context;

        public TeacherScheduleAdapter(android.content.Context context, List<EnrollmentModel> items, OnStudentClickListener listener) {
            this.context = context;
            this.items = items;
            this.listener = listener;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            // Use the Legendary item_schedule.xml
            return new ViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_schedule, parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            EnrollmentModel item = items.get(position);

            // 1. Student Name as Title
            String name = (item.getStudentName() != null) ? item.getStudentName() : "Student"; // Ensure getStudentName exists
            holder.tvSubjectName.setText(name);

            // 2. Class Name as Topic
            String className = (item.getTuitionTitle() != null) ? item.getTuitionTitle() : "Class";
            holder.tvTopic.setText(className);

            // 3. Status Text
            holder.tvStatus.setText("ENROLLED");
            holder.tvStatus.setTextColor(Color.parseColor("#4CAF50")); // Green
            holder.tvStatus.setBackgroundResource(R.drawable.bg_status_chip);

            // 4. Time/Duration (Placeholder)
            holder.tvTimeStart.setText("Active");
            holder.tvDuration.setVisibility(View.GONE); // Hide duration for student list

            // 5. Teacher/Location Info (Used for extra info here)
            holder.tvTutorName.setText("Tap to message");
            holder.tvLocation.setText("View Profile");

            // 6. Profile Image
            // Assuming EnrollmentModel has getStudentPhoto()
            // If not, just use default or update model
            String photoUrl = null;
            // photoUrl = item.getStudentPhoto();

            if(photoUrl != null && !photoUrl.isEmpty()){
                Glide.with(context)
                        .load(photoUrl)
                        .placeholder(R.mipmap.ic_launcher)
                        .circleCrop()
                        .into(holder.imgProfile);
            } else {
                holder.imgProfile.setImageResource(R.mipmap.ic_launcher);
            }

            // 7. Button Action
            holder.btnAction.setText("Chat");
            holder.btnAction.setOnClickListener(v -> listener.onClick(item));

            // Whole card click
            holder.itemView.setOnClickListener(v -> listener.onClick(item));
        }

        @Override
        public int getItemCount() { return items.size(); }

        static class ViewHolder extends RecyclerView.ViewHolder {
            // New Legendary IDs
            TextView tvSubjectName, tvTopic, tvStatus, tvTimeStart, tvDuration, tvTutorName, tvLocation;
            ImageView imgProfile;
            MaterialButton btnAction;

            public ViewHolder(@NonNull View v) {
                super(v);
                tvSubjectName = v.findViewById(R.id.tvSubjectName); // Used for Student Name
                tvTopic       = v.findViewById(R.id.tvTopic);       // Used for Class Name
                tvStatus      = v.findViewById(R.id.tvStatus);
                tvTimeStart   = v.findViewById(R.id.tvTimeStart);
                tvDuration    = v.findViewById(R.id.tvDuration);
                tvTutorName   = v.findViewById(R.id.tvTutorName);
                tvLocation    = v.findViewById(R.id.tvLocation);
                imgProfile    = v.findViewById(R.id.imgTutor);      // Used for Student Image
                btnAction     = v.findViewById(R.id.btnAction);
            }
        }
    }
}