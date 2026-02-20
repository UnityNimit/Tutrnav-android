package com.onrender.tutrnav;

import android.annotation.SuppressLint;
import android.os.Bundle;
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
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class TeacherDashboardFragment extends Fragment {

    // --- UI Components ---
    private TextView tvHeroTitle, tvHeroTime;
    private MaterialButton btnStartClass;
    private TextView tvTotalEarnings, tvActiveStudents, tvRating, btnViewAllRequests;
    private LinearLayout emptyStateView;
    private RecyclerView rvRequests;

    // --- Data & Adapters ---
    private RequestAdapter adapter;
    private List<EnrollmentModel> allEnrollments = new ArrayList<>();
    private List<EnrollmentModel> pendingRequests = new ArrayList<>();
    private List<TuitionModel> myTuitions = new ArrayList<>();

    // Quick lookup for calculating total earnings efficiently
    private Map<String, Double> tuitionFeesMap = new HashMap<>();

    // --- Firebase ---
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;

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
        // Hero Section
        tvHeroTitle = v.findViewById(R.id.tvHeroTitle);
        tvHeroTime = v.findViewById(R.id.tvHeroTime);
        btnStartClass = v.findViewById(R.id.btnStartClass);

        // Stats Section
        tvTotalEarnings = v.findViewById(R.id.tvTotalEarnings);
        tvActiveStudents = v.findViewById(R.id.tvActiveStudents);
        tvRating = v.findViewById(R.id.tvRating);

        // Requests Section
        btnViewAllRequests = v.findViewById(R.id.btnViewAllRequests);
        emptyStateView = v.findViewById(R.id.emptyStateView);
        rvRequests = v.findViewById(R.id.rvRequests);

        // Setup RecyclerView
        rvRequests.setLayoutManager(new LinearLayoutManager(getContext()));
        rvRequests.setNestedScrollingEnabled(false);
        adapter = new RequestAdapter(pendingRequests);
        rvRequests.setAdapter(adapter);

        // Setup Button Listeners
        btnStartClass.setOnClickListener(view ->
                Toast.makeText(getContext(), "Starting Live Room...", Toast.LENGTH_SHORT).show()
        );

        btnViewAllRequests.setOnClickListener(view ->
                Toast.makeText(getContext(), "Opening all requests...", Toast.LENGTH_SHORT).show()
        );
    }

    // --- DATA FETCHING (Parallel) ---
    private void fetchAllData() {
        String uid = mAuth.getCurrentUser().getUid();

        // 1. Fetch Tuitions (To update Hero Card & map pricing)
        db.collection("tuitions").whereEqualTo("teacherId", uid).get()
                .addOnSuccessListener(snapshots -> {
                    myTuitions.clear();
                    tuitionFeesMap.clear();

                    for (DocumentSnapshot doc : snapshots) {
                        TuitionModel t = doc.toObject(TuitionModel.class);
                        if (t != null) {
                            myTuitions.add(t);

                            // Safely parse fee to a Double and store mapping by Document ID
                            double fee = 0.0;
                            if (t.getFee() != null && !t.getFee().isEmpty()) {
                                try {
                                    String cleanFee = t.getFee().replaceAll("", "");
                                    fee = Double.parseDouble(cleanFee);
                                } catch (NumberFormatException ignored) {}
                            }
                            tuitionFeesMap.put(doc.getId(), fee);
                        }
                    }
                    updateHeroCard();
                    recalculateDashboard(); // Recalculate just in case enrollments loaded first
                });

        // 2. Fetch Enrollments (To populate requests and active students)
        db.collection("enrollments").whereEqualTo("teacherId", uid)
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        return;
                    }
                    if (value != null) {
                        allEnrollments.clear();
                        for (DocumentSnapshot doc : value) {
                            allEnrollments.add(doc.toObject(EnrollmentModel.class));
                        }
                        recalculateDashboard();
                    }
                });
    }

    // --- UI UPDATES ---
    @SuppressLint("SetTextI18n")
    private void updateHeroCard() {
        if (!myTuitions.isEmpty()) {
            // Just picking the first class for the Hero Card demonstration
            TuitionModel nextClass = myTuitions.get(0);
            tvHeroTitle.setText(nextClass.getTitle());
            tvHeroTime.setText((nextClass.getTime() != null ? nextClass.getTime() : "Timing TBD") + " • Live");
            btnStartClass.setVisibility(View.VISIBLE);
        } else {
            tvHeroTitle.setText("No Active Classes");
            tvHeroTime.setText("Tap on My Classes to create one.");
            btnStartClass.setVisibility(View.GONE);
        }
    }

    @SuppressLint({"SetTextI18n", "NotifyDataSetChanged"})
    private void recalculateDashboard() {
        int activeCount = 0;
        double totalEarnings = 0.0;

        pendingRequests.clear();

        for (EnrollmentModel e : allEnrollments) {
            if ("approved".equals(e.getStatus())) {
                activeCount++;

                // Fetch pricing mapping using the tuition ID attached to the enrollment
                if (e.getTuitionId() != null && tuitionFeesMap.containsKey(e.getTuitionId())) {
                    totalEarnings += tuitionFeesMap.get(e.getTuitionId());
                }
            } else if ("pending".equals(e.getStatus())) {
                pendingRequests.add(e);
            }
        }

        // 3. Update Text UI
        tvActiveStudents.setText(String.valueOf(activeCount));
        tvRating.setText("4.9"); // Can be updated with a real aggregation later

        // Format Currency dynamically
        NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(new Locale("en", "IN"));
        currencyFormat.setMaximumFractionDigits(0);

        // If it's a very large number, format as k (e.g., 45k)
        if (totalEarnings >= 1000) {
            tvTotalEarnings.setText("₹" + (int)(totalEarnings / 1000) + "k");
        } else {
            tvTotalEarnings.setText(currencyFormat.format(totalEarnings));
        }

        // 4. Update Requests List & Empty State
        adapter.notifyDataSetChanged();
        if (pendingRequests.isEmpty()) {
            emptyStateView.setVisibility(View.VISIBLE);
            rvRequests.setVisibility(View.GONE);
        } else {
            emptyStateView.setVisibility(View.GONE);
            rvRequests.setVisibility(View.VISIBLE);
        }
    }

    private void updateStatus(String enrollmentId, String status) {
        if (enrollmentId == null || enrollmentId.isEmpty()) return;

        db.collection("enrollments").document(enrollmentId)
                .update("status", status)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(getContext(), "Request " + status.toUpperCase(), Toast.LENGTH_SHORT).show();
                    // Notice: No need to manually update lists here, the SnapshotListener on Enrollments
                    // triggers instantly and `recalculateDashboard()` fires automatically.
                })
                .addOnFailureListener(e -> Toast.makeText(getContext(), "Failed to update", Toast.LENGTH_SHORT).show());
    }

    // --- RECYCLERVIEW ADAPTER ---
    private class RequestAdapter extends RecyclerView.Adapter<RequestAdapter.ViewHolder> {
        List<EnrollmentModel> list;

        public RequestAdapter(List<EnrollmentModel> list) {
            this.list = list;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new ViewHolder(LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_teacher_request, parent, false));
        }

        @SuppressLint("SetTextI18n")
        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            EnrollmentModel item = list.get(position);

            holder.tvName.setText(item.getStudentName() != null ? item.getStudentName() : "Unknown Student");
            holder.tvClass.setText("Wants to join: " + (item.getTuitionTitle() != null ? item.getTuitionTitle() : "Class"));

            // Safely load image with a circular crop
            if (item.getStudentPhoto() != null && !item.getStudentPhoto().isEmpty()) {
                Glide.with(holder.itemView.getContext())
                        .load(item.getStudentPhoto())
                        .circleCrop()
                        .into(holder.imgProfile);
            } else {
                holder.imgProfile.setImageResource(R.mipmap.ic_launcher);
            }

            // CTA Buttons
            holder.btnApprove.setOnClickListener(v -> updateStatus(item.getEnrollmentId(), "approved"));
            holder.btnDecline.setOnClickListener(v -> updateStatus(item.getEnrollmentId(), "rejected"));
        }

        @Override
        public int getItemCount() {
            return list.size();
        }

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