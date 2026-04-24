package com.onrender.tutrnav.ui.teacher;

import android.annotation.SuppressLint;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button; // <--- FLAWLESS FIX: Standard Button
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.onrender.tutrnav.R;
import com.onrender.tutrnav.models.EnrollmentModel;
import com.onrender.tutrnav.models.TuitionModel;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TeacherStudentsFragment extends Fragment {

    // --- UI Components ---
    private RecyclerView rvSchedule;
    private ChipGroup chipGroup;
    private ExtendedFloatingActionButton fabBroadcast;
    private LinearLayout layoutEmptyState, layoutStandardHeader, layoutSelectionMode;
    private TextView tvSelectionCount, tvEmptyText;
    private EditText etSearchStudent;
    private ImageView btnCloseSelection, btnSelectAll;

    // --- Data & Logic ---
    private TeacherViewModel viewModel;
    private StudentListAdapter adapter;

    // Source of Truth
    private final List<EnrollmentModel> activeStudents = new ArrayList<>();
    // Filtered Output
    private final List<EnrollmentModel> displayList = new ArrayList<>();

    // --- State Variables ---
    private String selectedTuitionId = "ALL";
    private String selectedTuitionTitle = "All Students";
    private String currentSearchQuery = "";
    private boolean isSelectionMode = false;
    private final List<EnrollmentModel> selectedStudents = new ArrayList<>();

    public interface OnStudentInteractListener {
        void onMessageClick(EnrollmentModel student);
        void onLongPress(EnrollmentModel student);
        void onTap(EnrollmentModel student);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_teacher_students, container, false);

        initViews(view);
        setupRecyclerView();
        setupSearchAndListeners();

        viewModel = new ViewModelProvider(requireActivity()).get(TeacherViewModel.class);
        observeData();

        return view;
    }

    private void initViews(View v) {
        rvSchedule = v.findViewById(R.id.rvTeacherSchedule);
        chipGroup = v.findViewById(R.id.chipGroupTuitions);
        fabBroadcast = v.findViewById(R.id.fabBroadcast);
        layoutEmptyState = v.findViewById(R.id.layoutEmptyState);
        tvEmptyText = v.findViewById(R.id.tvEmptyText);

        layoutStandardHeader = v.findViewById(R.id.layoutStandardHeader);
        layoutSelectionMode = v.findViewById(R.id.layoutSelectionMode);
        tvSelectionCount = v.findViewById(R.id.tvSelectionCount);
        btnCloseSelection = v.findViewById(R.id.btnCloseSelection);
        btnSelectAll = v.findViewById(R.id.btnSelectAll);
        etSearchStudent = v.findViewById(R.id.etSearchStudent);
    }

    private void setupSearchAndListeners() {
        etSearchStudent.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                currentSearchQuery = s.toString().toLowerCase().trim();
                applyFilters();
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        btnCloseSelection.setOnClickListener(v -> toggleSelectionMode(false));

        btnSelectAll.setOnClickListener(v -> {
            if (selectedStudents.size() == displayList.size()) {
                selectedStudents.clear();
            } else {
                selectedStudents.clear();
                selectedStudents.addAll(displayList);
            }
            updateSelectionUI();
            adapter.notifyDataSetChanged();
        });

        // SMART CONTEXT BROADCAST
        fabBroadcast.setOnClickListener(v -> {
            if (isSelectionMode) {
                if (selectedStudents.isEmpty()) {
                    Toast.makeText(getContext(), "Select at least 1 student", Toast.LENGTH_SHORT).show();
                    return;
                }
                openBottomSheetMessage(selectedStudents, "Selected Group", false);
            } else {
                if (displayList.isEmpty()) {
                    Toast.makeText(getContext(), "No students to message.", Toast.LENGTH_SHORT).show();
                    return;
                }
                openBottomSheetMessage(displayList, selectedTuitionTitle, false);
            }
        });
    }

    private void setupRecyclerView() {
        rvSchedule.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new StudentListAdapter(displayList, new OnStudentInteractListener() {
            @Override
            public void onMessageClick(EnrollmentModel student) {
                List<EnrollmentModel> target = new ArrayList<>();
                target.add(student);
                openBottomSheetMessage(target, student.getStudentName(), true);
            }

            @Override
            public void onLongPress(EnrollmentModel student) {
                if (!isSelectionMode) toggleSelectionMode(true);
                toggleStudentSelection(student);
            }

            @Override
            public void onTap(EnrollmentModel student) {
                if (isSelectionMode) toggleStudentSelection(student);
            }
        });
        rvSchedule.setAdapter(adapter);
    }

    private void observeData() {
        viewModel.getEnrollments().observe(getViewLifecycleOwner(), enrollments -> {
            activeStudents.clear();
            for (EnrollmentModel e : enrollments) {
                if ("approved".equals(e.getStatus())) {
                    activeStudents.add(e);
                }
            }
            buildFilterChips();
            applyFilters();
        });
    }

    private void buildFilterChips() {
        chipGroup.removeAllViews();
        if (!activeStudents.isEmpty()) addChip("ALL", "All Students");

        List<TuitionModel> classes = viewModel.getTuitions().getValue();
        if (classes != null) {
            for (TuitionModel t : classes) {
                if (t.getTuitionId() == null) continue;

                boolean hasStudents = false;
                for (EnrollmentModel e : activeStudents) {
                    if (t.getTuitionId().equals(e.getTuitionId())) {
                        hasStudents = true;
                        break;
                    }
                }

                if (hasStudents) addChip(t.getTuitionId(), t.getTitle() != null ? t.getTitle() : "Class");
            }
        }
    }

    private void addChip(String id, String label) {
        Chip chip = new Chip(requireContext());
        chip.setText(label);
        chip.setCheckable(true);
        chip.setClickable(true);
        chip.setChipBackgroundColorResource(android.R.color.white);
        chip.setTextColor(Color.parseColor("#2E2345"));

        chip.setOnCheckedChangeListener((btn, isChecked) -> {
            if (isChecked) {
                selectedTuitionId = id;
                selectedTuitionTitle = label;
                applyFilters();
            }
        });
        if (id.equals(selectedTuitionId)) chip.setChecked(true);
        chipGroup.addView(chip);
    }

    @SuppressLint("NotifyDataSetChanged")
    private void applyFilters() {
        displayList.clear();
        for (EnrollmentModel m : activeStudents) {
            boolean matchesClass = "ALL".equals(selectedTuitionId) || selectedTuitionId.equals(m.getTuitionId());
            boolean matchesSearch = currentSearchQuery.isEmpty() ||
                    (m.getStudentName() != null && m.getStudentName().toLowerCase().contains(currentSearchQuery));
            if (matchesClass && matchesSearch) displayList.add(m);
        }

        if (!isSelectionMode) {
            fabBroadcast.setText("ALL".equals(selectedTuitionId) ? "Broadcast All" : "Message " + selectedTuitionTitle);
        }

        adapter.notifyDataSetChanged();

        if (displayList.isEmpty()) {
            layoutEmptyState.setVisibility(View.VISIBLE);
            tvEmptyText.setText(currentSearchQuery.isEmpty() ? "No students in this class" : "No results found");
            rvSchedule.setVisibility(View.GONE);
        } else {
            layoutEmptyState.setVisibility(View.GONE);
            rvSchedule.setVisibility(View.VISIBLE);
        }
    }

    private void toggleSelectionMode(boolean active) {
        isSelectionMode = active;
        selectedStudents.clear();
        adapter.notifyDataSetChanged();

        layoutStandardHeader.setVisibility(active ? View.GONE : View.VISIBLE);
        layoutSelectionMode.setVisibility(active ? View.VISIBLE : View.GONE);

        if (active) fabBroadcast.setText("Message (0)");
        else applyFilters();

        updateSelectionUI();
    }

    private void toggleStudentSelection(EnrollmentModel student) {
        if (selectedStudents.contains(student)) selectedStudents.remove(student);
        else selectedStudents.add(student);

        if (selectedStudents.isEmpty() && isSelectionMode) toggleSelectionMode(false);
        else updateSelectionUI();

        adapter.notifyDataSetChanged();
    }

    private void updateSelectionUI() {
        tvSelectionCount.setText(selectedStudents.size() + " Selected");
        fabBroadcast.setText("Message (" + selectedStudents.size() + ")");
    }

    // ==========================================
    //      PERFECT BULK/PRIVATE MESSAGING
    // ==========================================
    @SuppressLint("SetTextI18n")
    private void openBottomSheetMessage(List<EnrollmentModel> targets, String titleOverride, boolean isPrivate) {
        BottomSheetDialog dialog = new BottomSheetDialog(requireContext());
        View sheetView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_teacher_message_bottom_sheet, null);
        dialog.setContentView(sheetView);

        if (sheetView.getParent() != null) {
            ((View) sheetView.getParent()).setBackgroundColor(Color.TRANSPARENT);
        }

        TextView tvTitle = sheetView.findViewById(R.id.tvSheetTitle);
        TextView tvSub = sheetView.findViewById(R.id.tvSheetSubtitle);
        EditText etMsg = sheetView.findViewById(R.id.etMessageBody);

        // <--- FLAWLESS FIX: Changed to Button, mapped to R.id.btnSend
        Button btnSend = sheetView.findViewById(R.id.btnSend);

        // Fetch the perfectly working Type group
        ChipGroup chipGroupType = sheetView.findViewById(R.id.chipGroupType);

        sheetView.findViewById(R.id.chipQuick1).setOnClickListener(v -> etMsg.setText(((Chip)v).getText()));
        sheetView.findViewById(R.id.chipQuick2).setOnClickListener(v -> etMsg.setText(((Chip)v).getText()));
        sheetView.findViewById(R.id.chipQuick3).setOnClickListener(v -> etMsg.setText(((Chip)v).getText()));

        if (isPrivate) {
            tvTitle.setText("Message " + targets.get(0).getStudentName());
            tvSub.setText("Private Conversation");
        } else {
            tvTitle.setText("Broadcast to " + titleOverride);
            tvSub.setText("Sending to " + targets.size() + " students");
        }

        btnSend.setOnClickListener(v -> {
            String txt = etMsg.getText().toString().trim();
            if (txt.isEmpty()) { etMsg.setError("Cannot be empty"); return; }

            // Extract EXACT Message Type chosen by the Teacher
            String messageType = "NORMAL";
            int checkedId = chipGroupType.getCheckedChipId();
            if (checkedId == R.id.chipFee) messageType = "FEE";
            else if (checkedId == R.id.chipImportant) messageType = "IMPORTANT";

            if (isPrivate) {
                EnrollmentModel target = targets.get(0);
                dispatchMessageToDatabase(target.getTuitionId(), target.getTuitionTitle(), target.getStudentId(), txt, messageType, false);
            }
            else if (isSelectionMode) {
                for (EnrollmentModel target : targets) {
                    dispatchMessageToDatabase(target.getTuitionId(), target.getTuitionTitle(), target.getStudentId(), txt, messageType, false);
                }
            }
            else {
                Map<String, String> uniqueTuitions = new HashMap<>();
                for (EnrollmentModel e : targets) {
                    uniqueTuitions.put(e.getTuitionId(), e.getTuitionTitle());
                }
                for (Map.Entry<String, String> entry : uniqueTuitions.entrySet()) {
                    dispatchMessageToDatabase(entry.getKey(), entry.getValue(), "ALL", txt, messageType, true);
                }
            }

            Toast.makeText(getContext(), "Message Sent Successfully!", Toast.LENGTH_SHORT).show();
            if (isSelectionMode) toggleSelectionMode(false);
            dialog.dismiss();
        });

        dialog.show();
    }

    private void dispatchMessageToDatabase(String tuitionId, String tuitionTitle, String studentId, String text, String type, boolean isBroadcast) {
        String senderId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        String senderName = FirebaseAuth.getInstance().getCurrentUser().getDisplayName();
        String senderPhoto = "";
        if (FirebaseAuth.getInstance().getCurrentUser().getPhotoUrl() != null) {
            senderPhoto = FirebaseAuth.getInstance().getCurrentUser().getPhotoUrl().toString();
        }

        Map<String, Object> msg = new HashMap<>();
        msg.put("text", text);
        msg.put("senderId", senderId);
        msg.put("senderName", senderName != null ? senderName : "Teacher");
        msg.put("teacherPhoto", senderPhoto);
        msg.put("studentId", studentId);
        msg.put("tuitionId", tuitionId);
        msg.put("tuitionTitle", tuitionTitle);
        msg.put("type", type);
        msg.put("isBroadcast", isBroadcast);
        msg.put("timestamp", new Date());

        FirebaseFirestore.getInstance().collection("messages").add(msg);
    }

    // --- RECYCLER ADAPTER ---
    public class StudentListAdapter extends RecyclerView.Adapter<StudentListAdapter.ViewHolder> {
        private final List<EnrollmentModel> items;
        private final OnStudentInteractListener listener;

        public StudentListAdapter(List<EnrollmentModel> items, OnStudentInteractListener listener) {
            this.items = items;
            this.listener = listener;
        }

        @NonNull @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new ViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_teacher_students_row, parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            EnrollmentModel item = items.get(position);

            holder.tvName.setText(item.getStudentName() != null ? item.getStudentName() : "Unknown");

            if (item.getStudentPhoto() != null && !item.getStudentPhoto().isEmpty()) {
                Glide.with(holder.itemView.getContext()).load(item.getStudentPhoto()).circleCrop().into(holder.imgStudent);
            } else {
                holder.imgStudent.setImageResource(R.mipmap.ic_launcher);
            }

            boolean isSelected = selectedStudents.contains(item);
            holder.itemView.setBackgroundColor(isSelected ? Color.parseColor("#33FFCA28") : Color.TRANSPARENT);
            holder.itemView.setAlpha(isSelectionMode && !isSelected ? 0.5f : 1.0f);

            if (isSelectionMode) {
                holder.btnMessage.setVisibility(View.GONE);
            } else {
                holder.btnMessage.setVisibility(View.VISIBLE);
                holder.btnMessage.setOnClickListener(v -> {
                    int pos = holder.getAdapterPosition();
                    if (pos != RecyclerView.NO_POSITION) listener.onMessageClick(items.get(pos));
                });
            }

            holder.itemView.setOnLongClickListener(v -> {
                int pos = holder.getAdapterPosition();
                if (pos != RecyclerView.NO_POSITION) listener.onLongPress(items.get(pos));
                return true;
            });
            holder.itemView.setOnClickListener(v -> {
                int pos = holder.getAdapterPosition();
                if (pos != RecyclerView.NO_POSITION) listener.onTap(items.get(pos));
            });
        }

        @Override public int getItemCount() { return items.size(); }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvName;
            ImageView imgStudent;
            CardView btnMessage;

            public ViewHolder(@NonNull View v) {
                super(v);
                tvName = v.findViewById(R.id.tvStudentName);
                imgStudent = v.findViewById(R.id.imgStudent);
                btnMessage = v.findViewById(R.id.btnMessage);
            }
        }
    }
}