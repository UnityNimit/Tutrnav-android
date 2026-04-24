package com.onrender.tutrnav.ui.student;

import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.onrender.tutrnav.R;
import com.onrender.tutrnav.models.EnrollmentModel;
import com.onrender.tutrnav.models.TuitionModel;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class StudentScheduleFragment extends Fragment {

    // --- Data & Firebase ---
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private List<EnrollmentModel> enrollmentList = new ArrayList<>();

    // Map to cache full tuition details (Teacher PFP, Time, Location)
    private Map<String, TuitionModel> tuitionDetailsMap = new HashMap<>();

    private StudentScheduleAdapter adapter;

    // --- UI Components ---
    private RecyclerView rvSchedule;
    private LinearLayout layoutEmptyState;
    private TextView tvDateNumber, tvMonthDay, tvClassCount;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_student_schedule, container, false);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        initViews(view);
        setupDateHeader();
        setupRecyclerView();
        fetchSchedule();

        return view;
    }

    private void initViews(View view) {
        rvSchedule = view.findViewById(R.id.rvSchedule);
        layoutEmptyState = view.findViewById(R.id.layoutEmptyState);
        tvDateNumber = view.findViewById(R.id.tvDateNumber);
        tvMonthDay = view.findViewById(R.id.tvMonthDay);
        tvClassCount = view.findViewById(R.id.tvClassCount);
    }

    private void setupDateHeader() {
        Date date = new Date();
        SimpleDateFormat dayFormat = new SimpleDateFormat("dd", Locale.getDefault());
        SimpleDateFormat monthFormat = new SimpleDateFormat("EEEE, MMM yyyy", Locale.getDefault());

        tvDateNumber.setText(dayFormat.format(date));
        tvMonthDay.setText(monthFormat.format(date).toUpperCase());
    }

    private void setupRecyclerView() {
        rvSchedule.setLayoutManager(new LinearLayoutManager(getContext()));
        // Pass the map to the adapter so it can look up details
        adapter = new StudentScheduleAdapter(getContext(), enrollmentList, tuitionDetailsMap);
        rvSchedule.setAdapter(adapter);
    }

    // --- FIRESTORE DATA FETCHING ---
    private void fetchSchedule() {
        if (mAuth.getCurrentUser() == null) {
            updateEmptyState();
            return;
        }

        // 1. Get Enrollments (Where status == approved)
        db.collection("enrollments")
                .whereEqualTo("studentId", mAuth.getCurrentUser().getUid())
                .whereEqualTo("status", "approved")
                .addSnapshotListener((value, error) -> {
                    if (error != null || value == null) return;

                    enrollmentList.clear();
                    List<String> tuitionIdsToFetch = new ArrayList<>();

                    for (DocumentSnapshot doc : value) {
                        EnrollmentModel model = doc.toObject(EnrollmentModel.class);
                        if (model != null) {
                            enrollmentList.add(model);
                            tuitionIdsToFetch.add(model.getTuitionId());
                        }
                    }

                    // 2. If we have enrollments, fetch their full details (Time, Location, Teacher PFP)
                    if (!tuitionIdsToFetch.isEmpty()) {
                        fetchTuitionDetails(tuitionIdsToFetch);
                    } else {
                        updateEmptyState();
                    }
                });
    }

    private void fetchTuitionDetails(List<String> ids) {
        // Firestore 'in' query supports up to 10 items. For production with >10 classes, batch this.
        // For now, this is perfect for a student schedule.
        db.collection("tuitions")
                .whereIn("tuitionId", ids)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    tuitionDetailsMap.clear();
                    for (DocumentSnapshot doc : queryDocumentSnapshots) {
                        TuitionModel t = doc.toObject(TuitionModel.class);
                        if (t != null) {
                            tuitionDetailsMap.put(t.getTuitionId(), t);
                        }
                    }
                    adapter.notifyDataSetChanged();
                    updateEmptyState();
                });
    }

    private void updateEmptyState() {
        if (enrollmentList.isEmpty()) {
            rvSchedule.setVisibility(View.GONE);
            layoutEmptyState.setVisibility(View.VISIBLE);
            tvClassCount.setText("0 Classes");
        } else {
            rvSchedule.setVisibility(View.VISIBLE);
            layoutEmptyState.setVisibility(View.GONE);
            tvClassCount.setText(enrollmentList.size() + " Classes Today");
        }
    }

    // --- THE LEGENDARY ADAPTER ---
    private static class StudentScheduleAdapter extends RecyclerView.Adapter<StudentScheduleAdapter.ViewHolder> {

        private final List<EnrollmentModel> list;
        private final Map<String, TuitionModel> detailsMap;
        private final Context context;

        // Keeps track of which hearts are toggled locally
        private final Map<Integer, Boolean> heartState = new HashMap<>();

        public StudentScheduleAdapter(Context context, List<EnrollmentModel> list, Map<String, TuitionModel> detailsMap) {
            this.context = context;
            this.list = list;
            this.detailsMap = detailsMap;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(context).inflate(R.layout.item_student_schedule_card, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            EnrollmentModel enrollment = list.get(position);

            // Get detailed info from our map
            TuitionModel details = detailsMap.get(enrollment.getTuitionId());

            // 1. Title & Timing
            holder.tvSubjectName.setText(enrollment.getTuitionTitle());

            if (details != null) {
                holder.tvTimeStart.setText(details.getTime() != null ? details.getTime() : "Time TBD");
                holder.tvTutorName.setText(details.getTeacherName() != null ? details.getTeacherName() : "Instructor");
                holder.tvLocation.setText(details.getAddress() != null ? details.getAddress() : "Location Online");

                // Load Teacher PFP
                if (details.getTeacherPhoto() != null && !details.getTeacherPhoto().isEmpty()) {
                    Glide.with(context)
                            .load(details.getTeacherPhoto())
                            .circleCrop()
                            .placeholder(R.mipmap.ic_launcher)
                            .into(holder.imgTutor);
                } else {
                    holder.imgTutor.setImageResource(R.mipmap.ic_launcher);
                }
            } else {
                // Fallback if details loading
                holder.tvTimeStart.setText("Loading...");
                holder.tvTutorName.setText("Loading...");
                holder.tvLocation.setText("...");
            }

            // 2. Heart Toggle Logic
            boolean isLiked = heartState.getOrDefault(position, false);
            updateHeartUI(holder.iconHeart, isLiked);

            holder.btnHeart.setOnClickListener(v -> {
                boolean newState = !heartState.getOrDefault(position, false);
                heartState.put(position, newState);

                // Animate Heart Pop
                animateHeart(holder.iconHeart, newState);
            });
        }

        private void updateHeartUI(ImageView icon, boolean isLiked) {
            if (isLiked) {
                icon.setColorFilter(Color.parseColor("#E91E63")); // Pink
            } else {
                icon.setColorFilter(Color.WHITE); // White
            }
        }

        private void animateHeart(ImageView icon, boolean isLiked) {
            // Scale Down
            ObjectAnimator scaleDownX = ObjectAnimator.ofFloat(icon, "scaleX", 0.7f);
            ObjectAnimator scaleDownY = ObjectAnimator.ofFloat(icon, "scaleY", 0.7f);
            scaleDownX.setDuration(100);
            scaleDownY.setDuration(100);

            // Scale Up (Overshoot for pop effect)
            ObjectAnimator scaleUpX = ObjectAnimator.ofFloat(icon, "scaleX", 1.2f, 1.0f);
            ObjectAnimator scaleUpY = ObjectAnimator.ofFloat(icon, "scaleY", 1.2f, 1.0f);
            scaleUpX.setDuration(300);
            scaleUpY.setDuration(300);
            scaleUpX.setInterpolator(new AccelerateDecelerateInterpolator());

            scaleDownX.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(android.animation.Animator animation) {
                    updateHeartUI(icon, isLiked);
                    scaleUpX.start();
                    scaleUpY.start();
                }
            });

            scaleDownX.start();
            scaleDownY.start();
        }

        @Override public int getItemCount() { return list.size(); }

        static class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvSubjectName, tvTimeStart, tvTutorName, tvLocation;
            ShapeableImageView imgTutor;
            CardView btnHeart;
            ImageView iconHeart;

            public ViewHolder(@NonNull View v) {
                super(v);
                tvSubjectName = v.findViewById(R.id.tvSubjectName);
                tvTimeStart = v.findViewById(R.id.tvTimeStart);
                tvTutorName = v.findViewById(R.id.tvTutorName);
                tvLocation = v.findViewById(R.id.tvLocation);
                imgTutor = v.findViewById(R.id.imgTutor);

                // Heart Button
                btnHeart = v.findViewById(R.id.btnHeart);
                iconHeart = v.findViewById(R.id.iconHeart);
            }
        }
    }
}