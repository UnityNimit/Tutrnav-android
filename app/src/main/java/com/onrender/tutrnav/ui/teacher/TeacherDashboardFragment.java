package com.onrender.tutrnav.ui.teacher;

import android.annotation.SuppressLint;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.os.Bundle;
import android.text.TextUtils;
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
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.CircleCrop;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.firestore.FirebaseFirestore;
import com.onrender.tutrnav.R;
import com.onrender.tutrnav.models.EnrollmentModel;
import com.onrender.tutrnav.models.TuitionModel;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class TeacherDashboardFragment extends Fragment {

    // --- UI Components ---
    private TextView tvTotalEarnings, tvActiveStudents;
    private LinearLayout emptyStateView, emptyClassesView;
    private RecyclerView rvRequests, rvTodayClasses;

    // --- Adapters & Data ---
    private RequestAdapter requestAdapter;
    private MiniClassAdapter classesAdapter;
    private TeacherViewModel viewModel;

    private List<EnrollmentModel> pendingRequests = new ArrayList<>();
    private List<TuitionModel> todayClasses = new ArrayList<>();

    // Maps Class ID to the number of *approved* students in it
    private Map<String, Integer> classStudentCountMap = new HashMap<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_teacher_dashboard, container, false);

        initViews(view);

        // Bind Shared ViewModel to observe Real-time Firestore data
        viewModel = new ViewModelProvider(requireActivity()).get(TeacherViewModel.class);
        observeData();

        return view;
    }

    private void initViews(View v) {
        // Overview Section
        tvTotalEarnings = v.findViewById(R.id.tvTotalEarnings);
        tvActiveStudents = v.findViewById(R.id.tvActiveStudents);

        // Empty States
        emptyStateView = v.findViewById(R.id.emptyStateView);
        emptyClassesView = v.findViewById(R.id.emptyClassesView);

        // Recycler Views
        rvRequests = v.findViewById(R.id.rvRequests);
        rvTodayClasses = v.findViewById(R.id.rvTodayClasses);

        // Setup Requests Recycler
        rvRequests.setLayoutManager(new LinearLayoutManager(getContext()));
        rvRequests.setNestedScrollingEnabled(false);
        requestAdapter = new RequestAdapter(pendingRequests);
        rvRequests.setAdapter(requestAdapter);
        setupSwipeToAct();

        // Setup Today's Classes Recycler
        rvTodayClasses.setLayoutManager(new LinearLayoutManager(getContext()));
        rvTodayClasses.setNestedScrollingEnabled(false);
        classesAdapter = new MiniClassAdapter(todayClasses);
        rvTodayClasses.setAdapter(classesAdapter);
    }

    private void observeData() {
        // 1. Observe Enrollments (This drives the Math and the Requests)
        viewModel.getEnrollments().observe(getViewLifecycleOwner(), enrollments -> {
            pendingRequests.clear();
            classStudentCountMap.clear();

            int totalActiveStudents = 0;
            double totalIncome = 0.0;

            for (EnrollmentModel e : enrollments) {
                if ("pending".equals(e.getStatus())) {
                    pendingRequests.add(e);
                }
                else if ("approved".equals(e.getStatus())) {
                    totalActiveStudents++;

                    // Tally up the students for each specific class
                    int currentCount = classStudentCountMap.getOrDefault(e.getTuitionId(), 0);
                    classStudentCountMap.put(e.getTuitionId(), currentCount + 1);

                    // Calculate Income perfectly by mapping the student to the class fee
                    TuitionModel t = viewModel.getTuitionById(e.getTuitionId());
                    if (t != null && t.getFee() != null) {
                        try {
                            // Strip any non-numeric characters (like '₹', ',', '/mo') before parsing
                            String cleanFee = t.getFee().replaceAll("[^\\d.]", "");
                            if(!cleanFee.isEmpty()){
                                totalIncome += Double.parseDouble(cleanFee);
                            }
                        } catch (NumberFormatException ignored) {}
                    }
                }
            }

            // Update Requests UI
            requestAdapter.notifyDataSetChanged();
            emptyStateView.setVisibility(pendingRequests.isEmpty() ? View.VISIBLE : View.GONE);
            rvRequests.setVisibility(pendingRequests.isEmpty() ? View.GONE : View.VISIBLE);

            // Update Overview Stats UI
            tvActiveStudents.setText(String.valueOf(totalActiveStudents));

            NumberFormat format = NumberFormat.getCurrencyInstance(new Locale("en", "IN"));
            format.setMaximumFractionDigits(0);
            tvTotalEarnings.setText(format.format(totalIncome));

            // Tell the Classes adapter to refresh so the student count updates on the mini cards
            if(classesAdapter != null) classesAdapter.notifyDataSetChanged();
        });

        // 2. Observe Classes List
        viewModel.getTuitions().observe(getViewLifecycleOwner(), tuitions -> {
            todayClasses.clear();
            if (tuitions != null) {
                // For now, we show all active classes. 
                // Later you can filter by day of week if you add that feature.
                todayClasses.addAll(tuitions);
            }

            classesAdapter.notifyDataSetChanged();
            emptyClassesView.setVisibility(todayClasses.isEmpty() ? View.VISIBLE : View.GONE);
            rvTodayClasses.setVisibility(todayClasses.isEmpty() ? View.GONE : View.VISIBLE);
        });
    }

    // ==========================================
    //      LEGENDARY SWIPE-TO-ACT LOGIC
    // ==========================================
    private void setupSwipeToAct() {
        ItemTouchHelper.SimpleCallback callback = new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {
            @Override
            public boolean onMove(@NonNull RecyclerView rv, @NonNull RecyclerView.ViewHolder vh, @NonNull RecyclerView.ViewHolder target) { return false; }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                int position = viewHolder.getAdapterPosition();
                EnrollmentModel target = pendingRequests.get(position);
                String newStatus = (direction == ItemTouchHelper.RIGHT) ? "approved" : "rejected";

                // Optimistic UI Update (Makes the app feel instant)
                pendingRequests.remove(position);
                requestAdapter.notifyItemRemoved(position);
                emptyStateView.setVisibility(pendingRequests.isEmpty() ? View.VISIBLE : View.GONE);

                // Update Database
                FirebaseFirestore.getInstance().collection("enrollments").document(target.getEnrollmentId())
                        .update("status", newStatus)
                        .addOnSuccessListener(aVoid -> {
                            Snackbar.make(requireView(), target.getStudentName() + " " + newStatus, Snackbar.LENGTH_SHORT).show();
                        })
                        .addOnFailureListener(e -> {
                            // Revert on network failure
                            pendingRequests.add(position, target);
                            requestAdapter.notifyItemInserted(position);
                            Toast.makeText(getContext(), "Network Error", Toast.LENGTH_SHORT).show();
                        });
            }

            @Override
            public void onChildDraw(@NonNull Canvas c, @NonNull RecyclerView rv, @NonNull RecyclerView.ViewHolder vh,
                                    float dX, float dY, int actionState, boolean isCurrentlyActive) {
                View itemView = vh.itemView;
                float cornerRadius = 35f * getResources().getDisplayMetrics().density; // 35dp to pixels
                Paint paint = new Paint();

                if (dX > 0) {
                    // Swiping Right (Approve) - Green
                    paint.setColor(Color.parseColor("#4CAF50"));
                    RectF background = new RectF((float) itemView.getLeft(), (float) itemView.getTop(), dX, (float) itemView.getBottom());
                    c.drawRoundRect(background, cornerRadius, cornerRadius, paint);
                } else if (dX < 0) {
                    // Swiping Left (Reject) - Red
                    paint.setColor(Color.parseColor("#F44336"));
                    RectF background = new RectF((float) itemView.getRight() + dX, (float) itemView.getTop(), (float) itemView.getRight(), (float) itemView.getBottom());
                    c.drawRoundRect(background, cornerRadius, cornerRadius, paint);
                }
                super.onChildDraw(c, rv, vh, dX, dY, actionState, isCurrentlyActive);
            }
        };
        new ItemTouchHelper(callback).attachToRecyclerView(rvRequests);
    }

    // ==========================================
    //      ADAPTER 1: TODAY'S CLASSES (MINI CARD)
    // ==========================================
    private class MiniClassAdapter extends RecyclerView.Adapter<MiniClassAdapter.MiniVH> {
        List<TuitionModel> list;
        public MiniClassAdapter(List<TuitionModel> list) { this.list = list; }

        @NonNull @Override
        public MiniVH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new MiniVH(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_teacher_dashboard_class_mini, parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull MiniVH holder, int position) {
            TuitionModel item = list.get(position);

            holder.tvTitle.setText(item.getTitle() != null ? item.getTitle() : "Class Session");
            holder.tvTime.setText(item.getTime() != null ? item.getTime() : "TBD");

            if (item.getTags() != null && !item.getTags().isEmpty()) {
                holder.tvTags.setText(TextUtils.join(" • ", item.getTags()));
            } else {
                holder.tvTags.setText("General Subject");
            }

            // Fetch dynamic student count computed in observeData()
            int studentCount = classStudentCountMap.getOrDefault(item.getTuitionId(), 0);
            holder.tvStudents.setText(String.valueOf(studentCount));
        }

        @Override public int getItemCount() { return list.size(); }

        class MiniVH extends RecyclerView.ViewHolder {
            TextView tvTitle, tvTime, tvTags, tvStudents;
            public MiniVH(@NonNull View v) {
                super(v);
                tvTitle = v.findViewById(R.id.tvMiniTitle);
                tvTime = v.findViewById(R.id.tvMiniTime);
                tvTags = v.findViewById(R.id.tvMiniTags);
                tvStudents = v.findViewById(R.id.tvMiniStudents);
            }
        }
    }

    // ==========================================
    //      ADAPTER 2: PENDING REQUESTS
    // ==========================================
    private class RequestAdapter extends RecyclerView.Adapter<RequestAdapter.RequestVH> {
        List<EnrollmentModel> list;
        public RequestAdapter(List<EnrollmentModel> list) { this.list = list; }

        @NonNull @Override
        public RequestVH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new RequestVH(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_teacher_dashboard_request_row, parent, false));
        }

        @SuppressLint("SetTextI18n")
        @Override
        public void onBindViewHolder(@NonNull RequestVH holder, int position) {
            EnrollmentModel item = list.get(position);

            holder.tvName.setText(item.getStudentName() != null ? item.getStudentName() : "Unknown Student");

            TuitionModel classInfo = viewModel.getTuitionById(item.getTuitionId());
            holder.tvClass.setText("Wants to join: " + (classInfo != null ? classInfo.getTitle() : "Your Class"));

            // Safely Load Student Profile Picture
            if (item.getStudentPhoto() != null && !item.getStudentPhoto().isEmpty()) {
                Glide.with(holder.itemView.getContext())
                        .load(item.getStudentPhoto())
                        .transform(new CircleCrop())
                        .placeholder(R.mipmap.ic_launcher)
                        .error(R.mipmap.ic_launcher)
                        .into(holder.imgProfile);
            } else {
                holder.imgProfile.setImageResource(R.mipmap.ic_launcher);
            }

            // Buttons hidden because we are using our legendary Swipe-To-Act UX!
            if(holder.btnApprove != null) holder.btnApprove.setVisibility(View.GONE);
            if(holder.btnDecline != null) holder.btnDecline.setVisibility(View.GONE);
        }

        @Override public int getItemCount() { return list.size(); }

        class RequestVH extends RecyclerView.ViewHolder {
            TextView tvName, tvClass;
            ImageView imgProfile;
            MaterialButton btnApprove, btnDecline;

            public RequestVH(@NonNull View v) {
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