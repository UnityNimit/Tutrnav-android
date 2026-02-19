package com.onrender.tutrnav;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.PickVisualMediaRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.CenterCrop;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;
import com.cloudinary.android.MediaManager;
import com.cloudinary.android.callback.ErrorInfo;
import com.cloudinary.android.callback.UploadCallback;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static android.app.Activity.RESULT_OK;

public class TeacherTuitionFragment extends Fragment {

    // --- Views ---
    private LinearLayout layoutList;
    private ScrollView layoutForm;
    private RecyclerView rvMyTuitions;
    private ExtendedFloatingActionButton fabAddNew;
    private ImageView btnBackToRecycler;

    // --- Form Views ---
    private ImageView imgBanner, btnPickLocation;
    private LinearLayout btnUploadImage;
    private EditText etTitle, etTagInput, etFee, etMaxStudents, etDesc, etLat, etLng;
    private ChipGroup chipGroupTags;
    private CheckBox cbConsent;
    private MaterialButton btnSave, btnDelete;
    private TextView tvFormTitle;

    // --- Data Variables ---
    private Uri selectedImageUri;
    private List<String> currentTags = new ArrayList<>();
    private String editingTuitionId = null; // Null = CREATE, Value = UPDATE
    private String existingBannerUrl = null;

    // --- Firebase & Utils ---
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private ProgressDialog progressDialog;

    // --- Adapter ---
    private MyTuitionAdapter adapter;
    private List<TuitionModel> myTuitionsList = new ArrayList<>();

    // --- Launchers ---
    private ActivityResultLauncher<PickVisualMediaRequest> pickMedia;
    private ActivityResultLauncher<Intent> pickLocationLauncher;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_teacher_tuition, container, false);

        // Initialize Firebase
        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        // Initialize Progress Dialog
        progressDialog = new ProgressDialog(getContext());
        progressDialog.setMessage("Processing...");
        progressDialog.setCancelable(false);

        initViews(view);
        setupLaunchers();
        setupTagSystem();
        setupListLogic();

        // Load data on startup
        loadMyTuitions();

        return view;
    }

    private void initViews(View v) {
        // Layouts
        layoutList = v.findViewById(R.id.layoutList);
        layoutForm = v.findViewById(R.id.layoutForm);

        // List Screen
        rvMyTuitions = v.findViewById(R.id.rvMyTuitions);
        fabAddNew = v.findViewById(R.id.fabAddNew);

        // Form Screen
        btnBackToRecycler = v.findViewById(R.id.btnBackToRecycler);
        tvFormTitle = v.findViewById(R.id.tvFormTitle);
        imgBanner = v.findViewById(R.id.imgBanner);
        btnUploadImage = v.findViewById(R.id.btnUploadImage);

        etTitle = v.findViewById(R.id.etTitle);
        etFee = v.findViewById(R.id.etFee);
        etMaxStudents = v.findViewById(R.id.etMaxStudents); // NEW
        etTagInput = v.findViewById(R.id.etTagInput);
        chipGroupTags = v.findViewById(R.id.chipGroupTags);
        etLat = v.findViewById(R.id.etLat);
        etLng = v.findViewById(R.id.etLng);
        btnPickLocation = v.findViewById(R.id.btnPickLocation);
        etDesc = v.findViewById(R.id.etDesc);

        cbConsent = v.findViewById(R.id.cbConsent); // NEW
        btnSave = v.findViewById(R.id.btnSave);
        btnDelete = v.findViewById(R.id.btnDelete); // NEW

        // --- Click Listeners ---
        fabAddNew.setOnClickListener(view -> showForm(null));
        btnBackToRecycler.setOnClickListener(view -> showList());

        btnUploadImage.setOnClickListener(view -> pickMedia.launch(new PickVisualMediaRequest.Builder()
                .setMediaType(ActivityResultContracts.PickVisualMedia.ImageOnly.INSTANCE)
                .build()));

        btnPickLocation.setOnClickListener(view -> {
            Intent intent = new Intent(getContext(), PickLocationActivity.class);
            pickLocationLauncher.launch(intent);
        });

        btnSave.setOnClickListener(view -> validateAndSave());

        btnDelete.setOnClickListener(view -> confirmDelete());
    }

    // ==========================================
    //              LIST LOGIC
    // ==========================================

    private void setupListLogic() {
        rvMyTuitions.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new MyTuitionAdapter(myTuitionsList, this::showForm);
        rvMyTuitions.setAdapter(adapter);
    }

    private void loadMyTuitions() {
        if (mAuth.getCurrentUser() == null) return;

        // Order by date or title if needed
        db.collection("tuitions")
                .whereEqualTo("teacherId", mAuth.getCurrentUser().getUid())
                .get()
                .addOnSuccessListener(snapshots -> {
                    myTuitionsList.clear();
                    for(DocumentSnapshot doc : snapshots) {
                        TuitionModel t = doc.toObject(TuitionModel.class);
                        if(t != null) myTuitionsList.add(t);
                    }
                    adapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e -> Toast.makeText(getContext(), "Failed to load classes", Toast.LENGTH_SHORT).show());
    }

    // ==========================================
    //              FORM LOGIC
    // ==========================================

    private void showList() {
        layoutForm.setVisibility(View.GONE);
        layoutList.setVisibility(View.VISIBLE);
        // Refresh list to show updates
        loadMyTuitions();
    }

    private void showForm(@Nullable TuitionModel model) {
        layoutList.setVisibility(View.GONE);
        layoutForm.setVisibility(View.VISIBLE);

        // Reset Common Fields
        chipGroupTags.removeAllViews();
        currentTags.clear();
        selectedImageUri = null;
        cbConsent.setChecked(false); // Always reset consent

        if (model == null) {
            // --- CREATE MODE ---
            editingTuitionId = null;
            existingBannerUrl = null;

            tvFormTitle.setText("Create New Class");
            btnSave.setText("Publish Class");
            btnDelete.setVisibility(View.GONE); // Hide delete button

            etTitle.setText("");
            etFee.setText("");
            etMaxStudents.setText("");
            etDesc.setText("");
            etLat.setText("");
            etLng.setText("");
            imgBanner.setImageResource(R.drawable.bg_gradient_overlay);

        } else {
            // --- EDIT MODE ---
            editingTuitionId = model.getTuitionId();
            existingBannerUrl = model.getBannerUrl();

            tvFormTitle.setText("Edit Class Details");
            btnSave.setText("Update Class");
            btnDelete.setVisibility(View.VISIBLE); // Show delete button

            etTitle.setText(model.getTitle());
            etFee.setText(model.getFee());
            etMaxStudents.setText(String.valueOf(model.getMaxStudents()));
            etDesc.setText(model.getDescription());
            etLat.setText(String.valueOf(model.getLatitude()));
            etLng.setText(String.valueOf(model.getLongitude()));

            if (model.getBannerUrl() != null && !model.getBannerUrl().isEmpty()) {
                Glide.with(this).load(model.getBannerUrl()).centerCrop().into(imgBanner);
            }

            // Restore Tags
            if (model.getTags() != null) {
                for(String tag : model.getTags()) {
                    addTagChip(tag);
                }
            }
        }
    }

    // ==========================================
    //              SAVE & DELETE
    // ==========================================

    private void validateAndSave() {
        // 1. Basic Validation
        if (etTitle.getText().toString().isEmpty() ||
                etFee.getText().toString().isEmpty() ||
                etMaxStudents.getText().toString().isEmpty()) {
            Toast.makeText(getContext(), "Title, Fee, and Max Students are required.", Toast.LENGTH_SHORT).show();
            return;
        }

        // 2. Consent Validation
        if (!cbConsent.isChecked()) {
            Toast.makeText(getContext(), "You must acknowledge the public display terms.", Toast.LENGTH_LONG).show();
            return;
        }

        progressDialog.show();

        // 3. Image Upload Logic
        if (selectedImageUri != null) {
            uploadImage();
        } else {
            saveToFirestore(existingBannerUrl);
        }
    }

    private void confirmDelete() {
        new AlertDialog.Builder(getContext())
                .setTitle("Delete Class?")
                .setMessage("Are you sure you want to delete this class? This action cannot be undone.")
                .setPositiveButton("Delete", (dialog, which) -> deleteClassFromFirestore())
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void deleteClassFromFirestore() {
        if (editingTuitionId == null) return;
        progressDialog.setMessage("Deleting...");
        progressDialog.show();

        db.collection("tuitions").document(editingTuitionId)
                .delete()
                .addOnSuccessListener(aVoid -> {
                    progressDialog.dismiss();
                    Toast.makeText(getContext(), "Class deleted successfully", Toast.LENGTH_SHORT).show();
                    showList();
                })
                .addOnFailureListener(e -> {
                    progressDialog.dismiss();
                    Toast.makeText(getContext(), "Error deleting class", Toast.LENGTH_SHORT).show();
                });
    }

    private void uploadImage() {
        // Using Cloudinary
        MediaManager.get().upload(selectedImageUri)
                .unsigned("tutornav_preset") // Ensure this preset exists in your Cloudinary
                .callback(new UploadCallback() {
                    @Override public void onStart(String requestId) {}
                    @Override public void onProgress(String requestId, long bytes, long totalBytes) {}
                    @Override public void onSuccess(String requestId, Map resultData) {
                        String url = (String) resultData.get("secure_url");
                        if(getActivity() != null) {
                            getActivity().runOnUiThread(() -> saveToFirestore(url));
                        }
                    }
                    @Override public void onError(String requestId, ErrorInfo error) {
                        if(getActivity() != null) {
                            getActivity().runOnUiThread(() -> {
                                progressDialog.dismiss();
                                Toast.makeText(getContext(), "Image Upload Failed: " + error.getDescription(), Toast.LENGTH_SHORT).show();
                            });
                        }
                    }
                    @Override public void onReschedule(String requestId, ErrorInfo error) {}
                }).dispatch();
    }

    private void saveToFirestore(String bannerUrl) {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) {
            progressDialog.dismiss();
            return;
        }

        String tuitionId = (editingTuitionId == null) ? UUID.randomUUID().toString() : editingTuitionId;

        // Parse Numbers Safely
        double lat = 0.0, lng = 0.0;
        int maxStuds = 0;
        try {
            if(!etLat.getText().toString().isEmpty()) lat = Double.parseDouble(etLat.getText().toString());
            if(!etLng.getText().toString().isEmpty()) lng = Double.parseDouble(etLng.getText().toString());
            maxStuds = Integer.parseInt(etMaxStudents.getText().toString());
        } catch (NumberFormatException e) {
            // handle error
        }

        // Create Map to avoid Constructor issues or use updated Model
        // NOTE: Your TuitionModel MUST have a 'maxStudents' field now.
        Map<String, Object> data = new HashMap<>();
        data.put("tuitionId", tuitionId);
        data.put("teacherId", user.getUid());
        data.put("title", etTitle.getText().toString().trim());
        data.put("fee", etFee.getText().toString().trim());
        data.put("maxStudents", maxStuds);
        data.put("description", etDesc.getText().toString().trim());
        data.put("bannerUrl", (bannerUrl != null) ? bannerUrl : "");
        data.put("latitude", lat);
        data.put("longitude", lng);
        data.put("teacherName", user.getDisplayName());
        data.put("teacherPhoto", (user.getPhotoUrl() != null) ? user.getPhotoUrl().toString() : "");
        data.put("tags", currentTags);

        // Using set() with Merge option or just set() since we build full object
        db.collection("tuitions").document(tuitionId)
                .set(data)
                .addOnSuccessListener(v -> {
                    progressDialog.dismiss();
                    Toast.makeText(getContext(), "Class saved successfully!", Toast.LENGTH_SHORT).show();
                    showList();
                })
                .addOnFailureListener(e -> {
                    progressDialog.dismiss();
                    Toast.makeText(getContext(), "Database Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    // ==========================================
    //              TAG SYSTEM
    // ==========================================

    private void setupTagSystem() {
        etTagInput.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE ||
                    (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER && event.getAction() == KeyEvent.ACTION_DOWN)) {
                String tag = etTagInput.getText().toString().trim();
                if (!tag.isEmpty()) {
                    addTagChip(tag);
                    etTagInput.setText("");
                }
                return true;
            }
            return false;
        });
    }

    private void addTagChip(String tag) {
        if (currentTags.contains(tag)) return;
        currentTags.add(tag);

        Chip chip = new Chip(getContext());
        chip.setText(tag);
        chip.setCloseIconVisible(true);
        chip.setTextColor(getResources().getColor(android.R.color.white));
        chip.setChipBackgroundColorResource(R.color.colorPrimary); // Make sure you have this or use a hex color
        chip.setOnCloseIconClickListener(v -> {
            chipGroupTags.removeView(chip);
            currentTags.remove(tag);
        });
        chipGroupTags.addView(chip);
    }

    // ==========================================
    //              LAUNCHERS
    // ==========================================

    private void setupLaunchers() {
        pickMedia = registerForActivityResult(new ActivityResultContracts.PickVisualMedia(), uri -> {
            if (uri != null) {
                selectedImageUri = uri;
                Glide.with(this).load(uri).transform(new CenterCrop(), new RoundedCorners(20)).into(imgBanner);
            }
        });

        pickLocationLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
            if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                double lat = result.getData().getDoubleExtra("lat", 0);
                double lng = result.getData().getDoubleExtra("lng", 0);
                etLat.setText(String.valueOf(lat));
                etLng.setText(String.valueOf(lng));
            }
        });
    }

    // ==========================================
    //          RECYCLER ADAPTER (UPDATED)
    // ==========================================

    private static class MyTuitionAdapter extends RecyclerView.Adapter<MyTuitionAdapter.VH> {
        List<TuitionModel> list;
        interface OnEditListener { void onEdit(TuitionModel t); }
        OnEditListener listener;

        public MyTuitionAdapter(List<TuitionModel> list, OnEditListener listener) {
            this.list = list;
            this.listener = listener;
        }

        @NonNull @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            // Updated to use the new Horizontal Card Layout
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_tuition_card, parent, false);
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(@NonNull VH holder, int position) {
            TuitionModel t = list.get(position);

            holder.title.setText(t.getTitle());
            holder.fee.setText("₹" + t.getFee() + "/mo");

            // Format Max Students
            holder.students.setText("Max: " + t.getMaxStudents());

            // Format Tags (Join first 2 tags with bullet)
            if(t.getTags() != null && !t.getTags().isEmpty()){
                String tagStr = TextUtils.join(" • ", t.getTags());
                holder.tags.setText(tagStr);
            } else {
                holder.tags.setText("General Class");
            }

            Glide.with(holder.itemView.getContext())
                    .load(t.getBannerUrl())
                    .placeholder(R.drawable.bg_gradient_overlay)
                    .transform(new CenterCrop(), new RoundedCorners(16))
                    .into(holder.img);

            holder.itemView.setOnClickListener(v -> listener.onEdit(t));
        }

        @Override public int getItemCount() { return list.size(); }

        static class VH extends RecyclerView.ViewHolder {
            TextView title, tags, fee, students;
            ImageView img;

            public VH(@NonNull View v) {
                super(v);
                // Bind to IDs from item_tuition_card.xml
                title = v.findViewById(R.id.tvCardTitle);
                tags = v.findViewById(R.id.tvCardTags);
                fee = v.findViewById(R.id.tvCardFee);
                students = v.findViewById(R.id.tvCardStudents);
                img = v.findViewById(R.id.imgCardThumb);
            }
        }
    }
}