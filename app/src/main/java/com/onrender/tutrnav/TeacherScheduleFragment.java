package com.onrender.tutrnav;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TeacherScheduleFragment extends Fragment {

    // --- UI Components ---
    private RecyclerView rvSchedule;
    private ChipGroup chipGroup;
    private ExtendedFloatingActionButton fabBroadcast;
    private LinearLayout layoutEmptyState;
    private TextView tvTotalStudents, tvTotalClasses;

    // --- Data ---
    private TeacherScheduleAdapter adapter;
    private final List<EnrollmentModel> allEnrollments = new ArrayList<>();
    private final List<EnrollmentModel> filteredList = new ArrayList<>();
    private final Map<String, String> tuitionIdToTitleMap = new HashMap<>();

    // --- Firebase ---
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private ListenerRegistration enrollmentsListener;

    // --- State ---
    private String selectedTuitionId = "ALL";
    private String selectedTuitionTitle = "All Students";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_teacher_schedule, container, false);

        initViews(view);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        setupRecyclerView();
        setupBroadcastButton();

        if (mAuth.getCurrentUser() != null) {
            fetchTuitionsAndStudents();
        }

        return view;
    }

    private void initViews(View view) {
        rvSchedule = view.findViewById(R.id.rvTeacherSchedule);
        chipGroup = view.findViewById(R.id.chipGroupTuitions);
        fabBroadcast = view.findViewById(R.id.fabBroadcastAll);
        layoutEmptyState = view.findViewById(R.id.layoutEmptyState);
        tvTotalStudents = view.findViewById(R.id.tvTotalStudents);
        tvTotalClasses = view.findViewById(R.id.tvTotalClasses);
    }

    private void setupRecyclerView() {
        rvSchedule.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new TeacherScheduleAdapter(requireContext(), filteredList, this::showComposeDialog);
        rvSchedule.setAdapter(adapter);
    }

    private void setupBroadcastButton() {
        fabBroadcast.setOnClickListener(v -> {
            if (filteredList.isEmpty()) {
                Toast.makeText(getContext(), "No students to message.", Toast.LENGTH_SHORT).show();
            } else {
                // Pass null to indicate a BROADCAST to the selected group
                showComposeDialog(null);
            }
        });
    }

    // --- FIREBASE DATA FETCHING ---
    private void fetchTuitionsAndStudents() {
        String uid = mAuth.getCurrentUser().getUid();

        // 1. Fetch Teacher's Tuitions to build the exact Class List for Chips
        db.collection("tuitions").whereEqualTo("teacherId", uid).get()
                .addOnSuccessListener(snapshots -> {
                    tuitionIdToTitleMap.clear();
                    int activeClasses = 0;

                    for (DocumentSnapshot doc : snapshots) {
                        TuitionModel t = doc.toObject(TuitionModel.class);
                        if (t != null) {
                            tuitionIdToTitleMap.put(doc.getId(), t.getTitle());
                            activeClasses++;
                        }
                    }
                    tvTotalClasses.setText(String.valueOf(activeClasses));

                    // 2. Fetch Enrollments once tuitions are mapped
                    fetchEnrollments(uid);
                })
                .addOnFailureListener(e -> Toast.makeText(getContext(), "Failed to load classes", Toast.LENGTH_SHORT).show());
    }

    @SuppressLint("SetTextI18n")
    private void fetchEnrollments(String uid) {
        enrollmentsListener = db.collection("enrollments")
                .whereEqualTo("teacherId", uid)
                .whereEqualTo("status", "approved")
                .addSnapshotListener((value, error) -> {
                    if (error != null) return;

                    if (value != null) {
                        allEnrollments.clear();
                        for (DocumentSnapshot doc : value) {
                            EnrollmentModel model = doc.toObject(EnrollmentModel.class);
                            if (model != null) {
                                // Ensure Title is accurate based on live Tuition map
                                if (tuitionIdToTitleMap.containsKey(model.getTuitionId())) {
                                    model.setTuitionTitle(tuitionIdToTitleMap.get(model.getTuitionId()));
                                }
                                allEnrollments.add(model);
                            }
                        }
                        tvTotalStudents.setText(String.valueOf(allEnrollments.size()));

                        buildFilterChips();
                        filterList(selectedTuitionId);
                    }
                });
    }

    // --- UI: CHIPS & FILTERING ---
    private void buildFilterChips() {
        chipGroup.removeAllViews();

        // Always add "ALL" if there are students
        if (!allEnrollments.isEmpty()) {
            addChip("ALL", "All Students");
        }

        // Add dynamically loaded classes
        for (Map.Entry<String, String> entry : tuitionIdToTitleMap.entrySet()) {
            // Only add chip if there are actual students in this class
            boolean hasStudents = false;
            for (EnrollmentModel e : allEnrollments) {
                if (entry.getKey().equals(e.getTuitionId())) {
                    hasStudents = true;
                    break;
                }
            }
            if (hasStudents) {
                addChip(entry.getKey(), entry.getValue());
            }
        }
    }

    private void addChip(String id, String label) {
        Chip chip = new Chip(requireContext());
        chip.setText(label);
        chip.setCheckable(true);
        chip.setClickable(true);

        // Premium Chip Styling
        chip.setChipBackgroundColorResource(R.color.white);
        chip.setTextColor(Color.parseColor("#2E2345"));

        chip.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                selectedTuitionId = id;
                selectedTuitionTitle = label;
                filterList(id);
            }
        });

        if (id.equals(selectedTuitionId)) {
            chip.setChecked(true);
        }

        chipGroup.addView(chip);
    }

    @SuppressLint("NotifyDataSetChanged")
    private void filterList(String tuitionId) {
        filteredList.clear();

        if ("ALL".equals(tuitionId)) {
            filteredList.addAll(allEnrollments);
            fabBroadcast.setText("Broadcast to All");
            fabBroadcast.setIconResource(android.R.drawable.ic_menu_send);
        } else {
            for (EnrollmentModel m : allEnrollments) {
                if (tuitionId.equals(m.getTuitionId())) {
                    filteredList.add(m);
                }
            }
            fabBroadcast.setText("Message Class");
            fabBroadcast.setIconResource(android.R.drawable.ic_menu_sort_by_size);
        }

        adapter.notifyDataSetChanged();

        // Empty State Handler
        if (filteredList.isEmpty()) {
            layoutEmptyState.setVisibility(View.VISIBLE);
            rvSchedule.setVisibility(View.GONE);
        } else {
            layoutEmptyState.setVisibility(View.GONE);
            rvSchedule.setVisibility(View.VISIBLE);
        }
    }

    // --- MESSAGING LOGIC ---
    private void showComposeDialog(@Nullable EnrollmentModel targetStudent) {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_send_message, null);
        builder.setView(dialogView);

        AlertDialog dialog = builder.create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            dialog.getWindow().getAttributes().windowAnimations = android.R.style.Animation_Dialog;
        }

        TextView title = dialogView.findViewById(R.id.tvDialogTitle);
        EditText etMsg = dialogView.findViewById(R.id.etMessage);
        MaterialButton btnSend = dialogView.findViewById(R.id.btnSend);
        MaterialButton btnCancel = dialogView.findViewById(R.id.btnCancel);

        if (targetStudent == null) {
            title.setText("Broadcast: " + selectedTuitionTitle);
            btnSend.setText("Broadcast");
        } else {
            title.setText("Message " + targetStudent.getStudentName());
            btnSend.setText("Send");
        }

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        btnSend.setOnClickListener(v -> {
            String msgText = etMsg.getText().toString().trim();
            if (TextUtils.isEmpty(msgText)) {
                etMsg.setError("Cannot be empty");
                return;
            }
            sendMessage(msgText, targetStudent, dialog);
        });

        dialog.show();
    }

    private void sendMessage(String text, @Nullable EnrollmentModel target, AlertDialog dialog) {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) return;

        Map<String, Object> msgMap = new HashMap<>();
        msgMap.put("text", text);
        msgMap.put("senderId", user.getUid());
        msgMap.put("senderName", user.getDisplayName() != null ? user.getDisplayName() : "Teacher");
        msgMap.put("senderPhoto", user.getPhotoUrl() != null ? user.getPhotoUrl().toString() : "");
        msgMap.put("timestamp", new Date());

        if (target != null) {
            // Private Message
            msgMap.put("studentId", target.getStudentId());
            msgMap.put("tuitionId", target.getTuitionId());
            msgMap.put("type", "PRIVATE");
        } else {
            // Broadcast
            msgMap.put("tuitionId", selectedTuitionId);
            msgMap.put("tuitionTitle", selectedTuitionTitle);
            msgMap.put("type", "BROADCAST");
        }

        db.collection("messages").add(msgMap)
                .addOnSuccessListener(docRef -> {
                    Toast.makeText(getContext(), "Sent Successfully!", Toast.LENGTH_SHORT).show();
                    dialog.dismiss();
                })
                .addOnFailureListener(e -> Toast.makeText(getContext(), "Failed: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (enrollmentsListener != null) {
            enrollmentsListener.remove(); // Prevent Memory Leaks
        }
    }

    // --- INTERFACES & ADAPTER ---
    public interface OnStudentClickListener {
        void onClick(EnrollmentModel student);
    }

    public static class TeacherScheduleAdapter extends RecyclerView.Adapter<TeacherScheduleAdapter.ViewHolder> {
        private final List<EnrollmentModel> items;
        private final OnStudentClickListener listener;
        private final Context context;

        public TeacherScheduleAdapter(Context context, List<EnrollmentModel> items, OnStudentClickListener listener) {
            this.context = context;
            this.items = items;
            this.listener = listener;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new ViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_schedule, parent, false));
        }

        @SuppressLint("SetTextI18n")
        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            EnrollmentModel item = items.get(position);

            // Re-purposing the schedule card to look great for Students
            holder.tvSubjectName.setText(item.getStudentName() != null ? item.getStudentName() : "Unknown Student");
            holder.tvTopic.setText(item.getTuitionTitle() != null ? item.getTuitionTitle() : "Enrolled Class");

            holder.tvStatus.setText("ACTIVE");
            holder.tvStatus.setTextColor(Color.parseColor("#4CAF50"));

            holder.tvTimeStart.setText("Student");
            holder.tvDuration.setVisibility(View.GONE);

            holder.tvTutorName.setText("Tap to interact");
            holder.tvLocation.setVisibility(View.GONE); // Hide location icon logic for students

            if (item.getStudentPhoto() != null && !item.getStudentPhoto().isEmpty()) {
                Glide.with(context).load(item.getStudentPhoto()).placeholder(R.mipmap.ic_launcher).circleCrop().into(holder.imgProfile);
            } else {
                holder.imgProfile.setImageResource(R.mipmap.ic_launcher);
            }

            holder.btnAction.setText("Message");
            holder.btnAction.setIconResource(android.R.drawable.ic_menu_send);

            holder.btnAction.setOnClickListener(v -> listener.onClick(item));
            holder.itemView.setOnClickListener(v -> listener.onClick(item));
        }

        @Override
        public int getItemCount() { return items.size(); }

        static class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvSubjectName, tvTopic, tvStatus, tvTimeStart, tvDuration, tvTutorName, tvLocation;
            ImageView imgProfile;
            MaterialButton btnAction;

            public ViewHolder(@NonNull View v) {
                super(v);
                tvSubjectName = v.findViewById(R.id.tvSubjectName);
                tvTopic       = v.findViewById(R.id.tvTopic);
                tvStatus      = v.findViewById(R.id.tvStatus);
                tvTimeStart   = v.findViewById(R.id.tvTimeStart);
                tvDuration    = v.findViewById(R.id.tvDuration);
                tvTutorName   = v.findViewById(R.id.tvTutorName);
                tvLocation    = v.findViewById(R.id.tvLocation);
                imgProfile    = v.findViewById(R.id.imgTutor);
                btnAction     = v.findViewById(R.id.btnAction);
            }
        }
    }
}