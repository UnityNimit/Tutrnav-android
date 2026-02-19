package com.onrender.tutrnav;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

public class TeacherDashboardFragment extends Fragment {

    // UI Components
    private TextView tvTotalEarnings, tvActiveStudents, tvPendingCount, tvRating;
    private RecyclerView rvRequests;
    private ChipGroup chipGroupFilters;
    private LinearLayout emptyStateView;

    // Data
    private RequestAdapter adapter;
    private List<EnrollmentModel> allEnrollments = new ArrayList<>(); // Master Data
    private List<EnrollmentModel> pendingRequests = new ArrayList<>(); // Filtered for Adapter
    private List<TuitionModel> myTuitions = new ArrayList<>();

    // Firebase
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;

    // State
    private String selectedTuitionId = "ALL"; // "ALL" or specific ID

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_teacher_dashboard, container, false);

        initViews(view);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        if (mAuth.getCurrentUser() != null) {
            fetchAllData();
        }

        return view;
    }

    private void initViews(View v) {
        tvTotalEarnings = v.findViewById(R.id.tvTotalEarnings);
        tvActiveStudents = v.findViewById(R.id.tvActiveStudents);
        tvPendingCount = v.findViewById(R.id.tvPendingCount);
        tvRating = v.findViewById(R.id.tvRating);
        chipGroupFilters = v.findViewById(R.id.chipGroupFilters);
        emptyStateView = v.findViewById(R.id.emptyStateView);

        rvRequests = v.findViewById(R.id.rvRequests);
        rvRequests.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new RequestAdapter(pendingRequests);
        rvRequests.setAdapter(adapter);
    }

    // --- DATA FETCHING (Parallel) ---
    private void fetchAllData() {
        String uid = mAuth.getCurrentUser().getUid();

        // 1. Fetch Tuitions (To create chips & calculate fees)
        db.collection("tuitions").whereEqualTo("teacherId", uid).get()
                .addOnSuccessListener(snapshots -> {
                    myTuitions.clear();
                    for (DocumentSnapshot doc : snapshots) {
                        TuitionModel t = doc.toObject(TuitionModel.class);
                        if (t != null) myTuitions.add(t);
                    }
                    setupFilterChips();
                });

        // 2. Fetch Enrollments (To calculate stats & show requests)
        db.collection("enrollments").whereEqualTo("teacherId", uid)
                .addSnapshotListener((value, error) -> {
                    if (error != null) return;
                    if (value != null) {
                        allEnrollments.clear();
                        for (DocumentSnapshot doc : value) {
                            allEnrollments.add(doc.toObject(EnrollmentModel.class));
                        }
                        recalculateDashboard();
                    }
                });
    }

    // --- UI LOGIC ---
    private void setupFilterChips() {
        chipGroupFilters.removeAllViews();

        // "All Classes" Chip
        addChip("ALL", "All Classes", true);

        // Specific Class Chips
        for (TuitionModel t : myTuitions) {
            addChip(t.getTeacherId(), t.getTitle(), false); // Using teacherID as ID per your structure, ideally use doc ID
        }
    }

    private void addChip(String id, String label, boolean isChecked) {
        Chip chip = new Chip(getContext());
        chip.setText(label);
        chip.setCheckable(true);
        chip.setTag(id);
        chip.setChecked(isChecked);

        // Chip Styling
        chip.setChipBackgroundColorResource(R.color.white);

        chip.setOnCheckedChangeListener((buttonView, checked) -> {
            if (checked) {
                selectedTuitionId = id;
                recalculateDashboard();
            }
        });
        chipGroupFilters.addView(chip);
    }

    private void recalculateDashboard() {
        // 1. Filter Data based on Chip
        List<EnrollmentModel> filteredEnrollments = new ArrayList<>();
        if (selectedTuitionId.equals("ALL")) {
            filteredEnrollments.addAll(allEnrollments);
        } else {
            for (EnrollmentModel e : allEnrollments) {
                if (e.getTuitionId().equals(selectedTuitionId)) {
                    filteredEnrollments.add(e);
                }
            }
        }

        // 2. Calculate Stats
        int activeCount = 0;
        int pendingCount = 0;
        double totalEarnings = 0;

        pendingRequests.clear(); // Clear adapter list

        for (EnrollmentModel e : filteredEnrollments) {
            if ("approved".equals(e.getStatus())) {
                activeCount++;
                // Find tuition fee for this enrollment
                double fee = getFeeForTuition(e.getTuitionId());
                totalEarnings += fee;
            } else if ("pending".equals(e.getStatus())) {
                pendingCount++;
                pendingRequests.add(e); // Add to request list
            }
        }

        // 3. Update UI
        tvActiveStudents.setText(String.valueOf(activeCount));
        tvPendingCount.setText(String.valueOf(pendingCount));
        tvTotalEarnings.setText("₹" + (int)totalEarnings); // Format nicely
        tvRating.setText("4.9"); // Static for now, requires Reviews Collection implementation

        // Update Request List
        adapter.notifyDataSetChanged();
        if (pendingRequests.isEmpty()) {
            emptyStateView.setVisibility(View.VISIBLE);
            rvRequests.setVisibility(View.GONE);
        } else {
            emptyStateView.setVisibility(View.GONE);
            rvRequests.setVisibility(View.VISIBLE);
        }
    }

    private double getFeeForTuition(String tuitionId) {
        // Find the tuition object to get the fee string
        for (TuitionModel t : myTuitions) {
            // Note: In your current data model, you might be using teacherId as tuitionId.
            // Adjust logic if you add a specific docId to TuitionModel later.
            if (t.getTeacherId().equals(tuitionId)) {
                try {
                    // Remove non-numeric chars (e.g. "₹500")
                    String cleanFee = t.getFee().replaceAll("[^\\d.]", "");
                    return Double.parseDouble(cleanFee);
                } catch (Exception e) {
                    return 0;
                }
            }
        }
        return 0;
    }

    private void updateStatus(String enrollmentId, String status) {
        db.collection("enrollments").document(enrollmentId)
                .update("status", status)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(getContext(), status.toUpperCase(), Toast.LENGTH_SHORT).show();
                    // Listener will trigger recalculateDashboard automatically
                });
    }

    // --- ADAPTER ---
    private class RequestAdapter extends RecyclerView.Adapter<RequestAdapter.ViewHolder> {
        List<EnrollmentModel> list;
        public RequestAdapter(List<EnrollmentModel> list) { this.list = list; }

        @NonNull @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new ViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_teacher_request, parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            EnrollmentModel item = list.get(position);
            holder.tvName.setText(item.getStudentName());
            holder.tvClass.setText("Wants to join: " + item.getTuitionTitle());

            if (item.getStudentPhoto() != null && !item.getStudentPhoto().isEmpty()) {
                Glide.with(holder.itemView.getContext()).load(item.getStudentPhoto()).circleCrop().into(holder.imgProfile);
            } else {
                holder.imgProfile.setImageResource(R.mipmap.ic_launcher);
            }

            holder.btnApprove.setOnClickListener(v -> updateStatus(item.getEnrollmentId(), "approved"));
            holder.btnDecline.setOnClickListener(v -> updateStatus(item.getEnrollmentId(), "rejected"));
        }

        @Override public int getItemCount() { return list.size(); }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvName, tvClass;
            ImageView imgProfile;
            MaterialButton btnApprove, btnDecline;

            public ViewHolder(@NonNull View v) {
                super(v);
                tvName = v.findViewById(R.id.tvStudentName);
                tvClass = v.findViewById(R.id.tvRequestClass);
                imgProfile = v.findViewById(R.id.imgStudent);
                btnApprove = v.findViewById(R.id.btnApprove);
                btnDecline = v.findViewById(R.id.btnDecline);
            }
        }
    }
}