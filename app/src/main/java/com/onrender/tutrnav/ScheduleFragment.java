package com.onrender.tutrnav;

import android.content.Context;
import android.graphics.Color;
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
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Random;

public class ScheduleFragment extends Fragment {

    // --- Data & Firebase ---
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private List<EnrollmentModel> fullList = new ArrayList<>();
    private List<EnrollmentModel> displayList = new ArrayList<>();
    private StudentScheduleAdapter adapter;

    // --- UI Components ---
    private RecyclerView rvSchedule;
    private LinearLayout layoutEmptyState;
    private TextView tvDateNumber, tvMonthDay, tvClassCount;

    // Filter Chips
    private TextView chipAll, chipLive, chipUpcoming, chipHistory;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // Inflate the "Legendary" Fragment Layout
        View view = inflater.inflate(R.layout.fragment_schedule, container, false);

        // 1. Initialize Firebase
        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        // 2. Initialize Views
        initViews(view);

        // 3. Setup Header (Date & Time)
        setupDateHeader();

        // 4. Setup RecyclerView
        setupRecyclerView();

        // 5. Setup Filter Chips
        setupFilters();

        // 6. Fetch Data
        fetchSchedule();

        return view;
    }

    private void initViews(View view) {
        rvSchedule = view.findViewById(R.id.rvSchedule);
        layoutEmptyState = view.findViewById(R.id.layoutEmptyState);

        // Header Texts
        tvDateNumber = view.findViewById(R.id.tvDateNumber);
        tvMonthDay = view.findViewById(R.id.tvMonthDay);
        tvClassCount = view.findViewById(R.id.tvClassCount);

        // Chips
        chipAll = view.findViewById(R.id.chipAll);
        chipLive = view.findViewById(R.id.chipLive);
        chipUpcoming = view.findViewById(R.id.chipUpcoming);
        chipHistory = view.findViewById(R.id.chipHistory);
    }

    private void setupDateHeader() {
        Date date = new Date();
        // Example: "20"
        SimpleDateFormat dayFormat = new SimpleDateFormat("dd", Locale.getDefault());
        // Example: "FRIDAY, FEB 2026"
        SimpleDateFormat monthFormat = new SimpleDateFormat("EEEE, MMM yyyy", Locale.getDefault());

        tvDateNumber.setText(dayFormat.format(date));
        tvMonthDay.setText(monthFormat.format(date).toUpperCase());
    }

    private void setupRecyclerView() {
        rvSchedule.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new StudentScheduleAdapter(getContext(), displayList);
        rvSchedule.setAdapter(adapter);
    }

    // --- FIRESTORE DATA FETCHING ---

    private void fetchSchedule() {
        if (mAuth.getCurrentUser() == null) {
            updateEmptyState();
            return;
        }

        // Listen for user's approved enrollments
        db.collection("enrollments")
                .whereEqualTo("studentId", mAuth.getCurrentUser().getUid())
                .whereEqualTo("status", "approved")
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        Toast.makeText(getContext(), "Error loading schedule", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    if (value != null) {
                        fullList.clear();
                        for (DocumentSnapshot doc : value) {
                            EnrollmentModel model = doc.toObject(EnrollmentModel.class);
                            if (model != null) {
                                fullList.add(model);
                            }
                        }

                        // Default: Show All
                        filterList(R.id.chipAll);
                    }
                });
    }

    // --- FILTER LOGIC ---

    private void setupFilters() {
        View.OnClickListener filterListener = v -> {
            // 1. Reset Styles (Visuals)
            resetChipStyles();

            // 2. Highlight Clicked Chip
            TextView clickedChip = (TextView) v;
            clickedChip.setTextColor(Color.parseColor("#2D2B45")); // Dark Text
            clickedChip.setBackgroundResource(R.drawable.bg_chip_active); // Gold BG

            // 3. Filter Data
            filterList(clickedChip.getId());
        };

        chipAll.setOnClickListener(filterListener);
        chipLive.setOnClickListener(filterListener);
        chipUpcoming.setOnClickListener(filterListener);
        chipHistory.setOnClickListener(filterListener);
    }

    private void resetChipStyles() {
        TextView[] chips = {chipAll, chipLive, chipUpcoming, chipHistory};
        for (TextView chip : chips) {
            chip.setTextColor(Color.WHITE);
            chip.setBackgroundResource(R.drawable.bg_chip_inactive);
        }
    }

    private void filterList(int chipId) {
        displayList.clear();

        if (chipId == R.id.chipAll) {
            displayList.addAll(fullList);
        }
        // Note: Real filtering requires your Firestore model to have time/status fields.
        // For now, we simulate filtering or pass everything if the field doesn't exist.
        else if (chipId == R.id.chipLive) {
            // Logic: Add if status == "LIVE" (Placeholder logic)
            // for (EnrollmentModel m : fullList) if (m.isLive()) displayList.add(m);
        }
        else if (chipId == R.id.chipUpcoming) {
            displayList.addAll(fullList); // Showing all for upcoming for now
        }
        else if (chipId == R.id.chipHistory) {
            // Show history
        }

        // Always show all for demo if filter logic isn't strictly defined in EnrollmentModel yet
        if (displayList.isEmpty() && !fullList.isEmpty() && chipId != R.id.chipLive) {
            displayList.addAll(fullList);
        }

        adapter.notifyDataSetChanged();
        updateEmptyState();
    }

    private void updateEmptyState() {
        if (displayList.isEmpty()) {
            rvSchedule.setVisibility(View.GONE);
            layoutEmptyState.setVisibility(View.VISIBLE);
            tvClassCount.setText("0 Classes");
        } else {
            rvSchedule.setVisibility(View.VISIBLE);
            layoutEmptyState.setVisibility(View.GONE);
            tvClassCount.setText(displayList.size() + " Classes Scheduled");
        }
    }

    // --- THE LEGENDARY ADAPTER ---

    private static class StudentScheduleAdapter extends RecyclerView.Adapter<StudentScheduleAdapter.ViewHolder> {

        private final List<EnrollmentModel> list;
        private final Context context;

        public StudentScheduleAdapter(Context context, List<EnrollmentModel> list) {
            this.context = context;
            this.list = list;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            // Uses the "Legendary" item_schedule.xml
            View view = LayoutInflater.from(context).inflate(R.layout.item_schedule, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            EnrollmentModel item = list.get(position);

            // 1. Title & Topic
            // If EnrollmentModel doesn't have a 'tuitionTitle', fallback to 'tuitionId' or placeholder
            String title = item.getTuitionTitle() != null ? item.getTuitionTitle() : "Class Session";
            holder.tvSubjectName.setText(title);

            // Placeholder Topic (since EnrollmentModel might not have it yet)
            holder.tvTopic.setText("General Session • " + item.getStatus().toUpperCase());

            // 2. Time (Placeholder logic or fetch from model)
            // Ideally: item.getStartTime()
            holder.tvTimeStart.setText("TBA");
            holder.tvDuration.setText("(60 mins)");

            // 3. Teacher Info
            String tName = item.getTeacherId() != null ? item.getTeacherId() : "Instructor";
            // Make ID shorter for UI looks: "Instructor: 5f3a..."
            if(tName.length() > 5) tName = "ID: " + tName.substring(0, 5);
            holder.tvTutorName.setText(tName);

            // 4. Status Chip
            holder.tvStatus.setText("UPCOMING");
            holder.tvStatus.setTextColor(Color.parseColor("#FFCA28")); // Gold
            holder.tvStatus.setBackgroundResource(R.drawable.bg_status_chip);

            // 5. Dynamic Color Bar (Hash the subject name to get a consistent unique color)
            int color = getColorForSubject(title);
            holder.viewColorBar.setBackgroundColor(color);

            // 6. Image (Teacher)
            Glide.with(context)
                    .load(R.mipmap.ic_launcher) // Placeholder, replace with item.getTeacherImage()
                    .circleCrop()
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .into(holder.imgTutor);

            // 7. Location
            holder.tvLocation.setText("Online / Map");

            // 8. Button Click
            holder.btnAction.setOnClickListener(v -> {
                Toast.makeText(context, "Opening details for " + title, Toast.LENGTH_SHORT).show();
            });
        }

        @Override public int getItemCount() { return list.size(); }

        // Generate a cool color based on string
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

        static class ViewHolder extends RecyclerView.ViewHolder {
            // Matching IDs from item_schedule.xml
            TextView tvTimeStart, tvDuration, tvStatus, tvSubjectName, tvTopic, tvTutorName, tvLocation;
            View viewColorBar;
            ImageView imgTutor;
            MaterialButton btnAction;

            public ViewHolder(@NonNull View v) {
                super(v);
                tvTimeStart = v.findViewById(R.id.tvTimeStart);
                tvDuration = v.findViewById(R.id.tvDuration);
                tvStatus = v.findViewById(R.id.tvStatus);
                tvSubjectName = v.findViewById(R.id.tvSubjectName);
                tvTopic = v.findViewById(R.id.tvTopic);
                tvTutorName = v.findViewById(R.id.tvTutorName);
                tvLocation = v.findViewById(R.id.tvLocation);
                viewColorBar = v.findViewById(R.id.viewColorBar);
                imgTutor = v.findViewById(R.id.imgTutor);
                btnAction = v.findViewById(R.id.btnAction);
            }
        }
    }
}