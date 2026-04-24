package com.onrender.tutrnav.ui.teacher;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.location.Address;
import android.location.Geocoder;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.Button; // <--- FLAWLESS FIX: Standard Button
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
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.android.material.timepicker.MaterialTimePicker;
import com.google.android.material.timepicker.TimeFormat;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;
import com.onrender.tutrnav.R;
import com.onrender.tutrnav.models.TuitionModel;
import com.onrender.tutrnav.ui.common.LocationPickerActivity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import static android.app.Activity.RESULT_OK;

public class TeacherClassesFragment extends Fragment {

    // --- Views ---
    private LinearLayout layoutList;
    private ScrollView layoutForm;
    private RecyclerView rvMyTuitions;
    private ExtendedFloatingActionButton fabAddNew;
    private ImageView btnBackToRecycler;

    // --- Form Views ---
    private ImageView imgBanner, btnPickLocation;
    private LinearLayout btnUploadImage;
    private EditText etTitle, etTime, etTagInput, etFee, etMaxStudents, etDesc, etAddress;
    private ChipGroup chipGroupTags;
    private CheckBox cbConsent;

    // <--- FLAWLESS FIX: Changed from MaterialButton to Button
    private Button btnSave, btnDelete;
    private TextView tvFormTitle;

    // --- Data Variables ---
    private Uri selectedImageUri;
    private List<String> currentTags = new ArrayList<>();
    private String editingTuitionId = null; // Null = CREATE, Value = UPDATE
    private String existingBannerUrl = null;

    // Storing precise coordinates behind the scenes
    private double currentLat = 0.0;
    private double currentLng = 0.0;

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
        View view = inflater.inflate(R.layout.fragment_teacher_classes, container, false);

        // Initialize Firebase
        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        progressDialog = new ProgressDialog(getContext());
        progressDialog.setMessage("Processing...");
        progressDialog.setCancelable(false);

        initViews(view);
        setupLaunchers();
        setupTagSystem();
        setupListLogic();

        loadMyTuitions();

        return view;
    }

    private void initViews(View v) {
        layoutList = v.findViewById(R.id.layoutList);
        layoutForm = v.findViewById(R.id.layoutForm);

        rvMyTuitions = v.findViewById(R.id.rvMyTuitions);
        fabAddNew = v.findViewById(R.id.fabAddNew);

        btnBackToRecycler = v.findViewById(R.id.btnBackToRecycler);
        tvFormTitle = v.findViewById(R.id.tvFormTitle);
        imgBanner = v.findViewById(R.id.imgBanner);
        btnUploadImage = v.findViewById(R.id.btnUploadImage);

        etTitle = v.findViewById(R.id.etTitle);
        etTime = v.findViewById(R.id.etTime);
        etFee = v.findViewById(R.id.etFee);
        etMaxStudents = v.findViewById(R.id.etMaxStudents);
        etTagInput = v.findViewById(R.id.etTagInput);
        chipGroupTags = v.findViewById(R.id.chipGroupTags);
        etAddress = v.findViewById(R.id.etAddress);
        btnPickLocation = v.findViewById(R.id.btnPickLocation);
        etDesc = v.findViewById(R.id.etDesc);

        cbConsent = v.findViewById(R.id.cbConsent);

        // <--- FLAWLESS FIX: Standard findViewById matching the XML Button
        btnSave = v.findViewById(R.id.btnSave);
        btnDelete = v.findViewById(R.id.btnDelete);

        // --- Click Listeners ---
        fabAddNew.setOnClickListener(view -> showForm(null));
        btnBackToRecycler.setOnClickListener(view -> showList());

        btnUploadImage.setOnClickListener(view -> pickMedia.launch(new PickVisualMediaRequest.Builder()
                .setMediaType(ActivityResultContracts.PickVisualMedia.ImageOnly.INSTANCE)
                .build()));

        // Aesthetic Time Picker Trigger
        etTime.setOnClickListener(view -> showAestheticTimePicker());

        // Location Picker Triggers
        btnPickLocation.setOnClickListener(view -> {
            Intent intent = new Intent(getContext(), LocationPickerActivity.class);
            pickLocationLauncher.launch(intent);
        });
        etAddress.setOnClickListener(view -> btnPickLocation.performClick());

        btnSave.setOnClickListener(view -> validateAndSave());
        btnDelete.setOnClickListener(view -> confirmDelete());
    }

    // ==========================================
    //           AESTHETIC TIME PICKER
    // ==========================================
    private void showAestheticTimePicker() {
        MaterialTimePicker picker = new MaterialTimePicker.Builder()
                .setTimeFormat(TimeFormat.CLOCK_12H)
                .setHour(16) // Default to 4 PM
                .setMinute(0)
                .setTitleText("Select Class Time")
                .setInputMode(MaterialTimePicker.INPUT_MODE_CLOCK)
                .build();

        picker.addOnPositiveButtonClickListener(dialog -> {
            int h = picker.getHour();
            int m = picker.getMinute();
            String amPm = (h >= 12) ? "PM" : "AM";
            int displayHour = (h == 0 || h == 12) ? 12 : h % 12;

            String formattedTime = String.format(Locale.getDefault(), "%02d:%02d %s", displayHour, m, amPm);
            etTime.setText(formattedTime);
        });

        picker.show(getParentFragmentManager(), "TIME_PICKER");
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

        db.collection("tuitions")
                .whereEqualTo("teacherId", mAuth.getCurrentUser().getUid())
                .get()
                .addOnSuccessListener(snapshots -> {
                    if (!isAdded()) return;
                    myTuitionsList.clear();
                    for(DocumentSnapshot doc : snapshots) {
                        TuitionModel t = doc.toObject(TuitionModel.class);
                        if(t != null) myTuitionsList.add(t);
                    }
                    adapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e -> {
                    if (isAdded()) Toast.makeText(getContext(), "Failed to load classes", Toast.LENGTH_SHORT).show();
                });
    }

    // ==========================================
    //              FORM LOGIC
    // ==========================================

    private void showList() {
        layoutForm.setVisibility(View.GONE);
        layoutList.setVisibility(View.VISIBLE);
        loadMyTuitions();
    }

    private void showForm(@Nullable TuitionModel model) {
        layoutList.setVisibility(View.GONE);
        layoutForm.setVisibility(View.VISIBLE);
        layoutForm.scrollTo(0, 0);

        chipGroupTags.removeAllViews();
        currentTags.clear();
        selectedImageUri = null;
        cbConsent.setChecked(false);

        if (model == null) {
            // CREATE MODE
            editingTuitionId = null;
            existingBannerUrl = null;
            currentLat = 0.0;
            currentLng = 0.0;

            tvFormTitle.setText("Create New Class");
            btnSave.setText("Publish Class");
            btnDelete.setVisibility(View.GONE);

            etTitle.setText("");
            etTime.setText("");
            etFee.setText("");
            etMaxStudents.setText("");
            etDesc.setText("");
            etAddress.setText("");
            imgBanner.setImageResource(R.drawable.bg_gradient_overlay);

        } else {
            // EDIT MODE
            editingTuitionId = model.getTuitionId();
            existingBannerUrl = model.getBannerUrl();
            currentLat = model.getLatitude();
            currentLng = model.getLongitude();

            tvFormTitle.setText("Edit Class Details");
            btnSave.setText("Update Class");
            btnDelete.setVisibility(View.VISIBLE);

            etTitle.setText(model.getTitle());
            etTime.setText(model.getTime() != null ? model.getTime() : "");
            etFee.setText(model.getFee());
            etMaxStudents.setText(String.valueOf(model.getMaxStudents()));
            etDesc.setText(model.getDescription());

            fetchAddressFromCoords(currentLat, currentLng);

            if (model.getBannerUrl() != null && !model.getBannerUrl().isEmpty()) {
                Glide.with(this).load(model.getBannerUrl()).centerCrop().into(imgBanner);
            }

            if (model.getTags() != null) {
                for(String tag : model.getTags()) {
                    addTagChip(tag);
                }
            }
        }
    }

    // ==========================================
    //              GEOCODING LOGIC
    // ==========================================

    private void fetchAddressFromCoords(double lat, double lng) {
        if (lat == 0.0 && lng == 0.0) return;

        try {
            Geocoder geocoder = new Geocoder(requireContext(), Locale.getDefault());
            List<Address> addresses = geocoder.getFromLocation(lat, lng, 1);
            if (addresses != null && !addresses.isEmpty()) {
                Address address = addresses.get(0);

                StringBuilder readableAddress = new StringBuilder();
                if (address.getThoroughfare() != null) readableAddress.append(address.getThoroughfare()).append(", ");
                if (address.getLocality() != null) readableAddress.append(address.getLocality()).append(", ");
                if (address.getAdminArea() != null) readableAddress.append(address.getAdminArea());

                String finalAddress = readableAddress.toString().replaceAll(", $", "");
                etAddress.setText(finalAddress.isEmpty() ? "Location Selected" : finalAddress);
            } else {
                etAddress.setText("Location Selected");
            }
        } catch (Exception e) {
            etAddress.setText("Location Selected");
        }
    }

    // ==========================================
    //              SAVE & DEEP DELETE
    // ==========================================

    private void validateAndSave() {
        if (TextUtils.isEmpty(etTitle.getText().toString().trim()) ||
                TextUtils.isEmpty(etTime.getText().toString().trim()) ||
                TextUtils.isEmpty(etFee.getText().toString().trim()) ||
                TextUtils.isEmpty(etMaxStudents.getText().toString().trim())) {

            Toast.makeText(getContext(), "Title, Timings, Fee, and Max Students are required.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!cbConsent.isChecked()) {
            Toast.makeText(getContext(), "You must acknowledge the public display terms.", Toast.LENGTH_LONG).show();
            return;
        }

        progressDialog.setMessage("Publishing Class...");
        progressDialog.show();
        btnSave.setEnabled(false);
        btnDelete.setEnabled(false);

        if (selectedImageUri != null) {
            uploadImage();
        } else {
            saveToFirestore(existingBannerUrl);
        }
    }

    private void confirmDelete() {
        if (editingTuitionId == null) return;

        new AlertDialog.Builder(getContext())
                .setTitle("Delete Class?")
                .setMessage("This will permanently delete this class, including all student enrollments and messages tied to it. This cannot be undone.")
                .setPositiveButton("Delete Everything", (dialog, which) -> deleteClassFromFirestoreDeep())
                .setNegativeButton("Cancel", null)
                .show();
    }

    // --- THE DEEP DELETE LOGIC ---
    private void deleteClassFromFirestoreDeep() {
        progressDialog.setMessage("Erasing Class Data...");
        progressDialog.show();

        // 1. Delete all Enrollments tied to this class
        db.collection("enrollments").whereEqualTo("tuitionId", editingTuitionId).get()
                .addOnSuccessListener(query -> {
                    for (DocumentSnapshot doc : query) doc.getReference().delete();
                });

        // 2. Delete all Messages tied to this class
        db.collection("messages").whereEqualTo("tuitionId", editingTuitionId).get()
                .addOnSuccessListener(query -> {
                    for (DocumentSnapshot doc : query) doc.getReference().delete();
                });

        // 3. Delete the Class Document itself
        db.collection("tuitions").document(editingTuitionId).delete()
                .addOnSuccessListener(aVoid -> {
                    if (isAdded()) {
                        progressDialog.dismiss();
                        Toast.makeText(getContext(), "Class and all related data deleted.", Toast.LENGTH_LONG).show();
                        showList();
                    }
                })
                .addOnFailureListener(e -> {
                    if (isAdded()) {
                        progressDialog.dismiss();
                        Toast.makeText(getContext(), "Error deleting class.", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void uploadImage() {
        MediaManager.get().upload(selectedImageUri)
                .unsigned("tutornav_preset")
                .callback(new UploadCallback() {
                    @Override public void onStart(String requestId) {}
                    @Override public void onProgress(String requestId, long bytes, long totalBytes) {}
                    @Override public void onSuccess(String requestId, Map resultData) {
                        String url = (String) resultData.get("secure_url");
                        if(isAdded() && getActivity() != null) {
                            getActivity().runOnUiThread(() -> saveToFirestore(url));
                        }
                    }
                    @Override public void onError(String requestId, ErrorInfo error) {
                        if(isAdded() && getActivity() != null) {
                            getActivity().runOnUiThread(() -> {
                                progressDialog.dismiss();
                                btnSave.setEnabled(true);
                                btnDelete.setEnabled(true);
                                Toast.makeText(getContext(), "Upload Failed", Toast.LENGTH_SHORT).show();
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
            btnSave.setEnabled(true);
            btnDelete.setEnabled(true);
            return;
        }

        String tuitionId = (editingTuitionId == null) ? UUID.randomUUID().toString() : editingTuitionId;
        int maxStuds = 0;
        try { maxStuds = Integer.parseInt(etMaxStudents.getText().toString()); } catch (NumberFormatException ignored) {}

        final int finalMaxStuds = maxStuds;

        db.collection("users").document(user.getUid()).get().addOnCompleteListener(task -> {
            String phone = "";
            String profilePhoto = user.getPhotoUrl() != null ? user.getPhotoUrl().toString() : "";

            if (task.isSuccessful() && task.getResult().exists()) {
                DocumentSnapshot userDoc = task.getResult();
                if (userDoc.getString("phone") != null) phone = userDoc.getString("phone");
                if (userDoc.getString("photoUrl") != null) profilePhoto = userDoc.getString("photoUrl");
            }

            Map<String, Object> data = new HashMap<>();
            data.put("tuitionId", tuitionId);
            data.put("teacherId", user.getUid());
            data.put("title", etTitle.getText().toString().trim());
            data.put("time", etTime.getText().toString().trim());
            data.put("fee", etFee.getText().toString().trim());
            data.put("maxStudents", finalMaxStuds);
            data.put("description", etDesc.getText().toString().trim());
            data.put("bannerUrl", (bannerUrl != null) ? bannerUrl : "");

            data.put("latitude", currentLat);
            data.put("longitude", currentLng);
            data.put("address", etAddress.getText().toString().trim());

            data.put("teacherName", user.getDisplayName());
            data.put("teacherPhoto", profilePhoto);
            data.put("teacherPhone", phone);

            data.put("tags", currentTags);

            db.collection("tuitions").document(tuitionId)
                    .set(data, SetOptions.merge())
                    .addOnSuccessListener(v -> {
                        if (isAdded()) {
                            progressDialog.dismiss();
                            btnSave.setEnabled(true);
                            btnDelete.setEnabled(true);
                            Toast.makeText(getContext(), "Class saved successfully!", Toast.LENGTH_SHORT).show();
                            showList();
                        }
                    });
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

        chip.setChipBackgroundColor(ColorStateList.valueOf(Color.parseColor("#6555C0")));
        chip.setTextColor(Color.WHITE);
        chip.setCloseIconTint(ColorStateList.valueOf(Color.WHITE));

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
                currentLat = result.getData().getDoubleExtra("lat", 0);
                currentLng = result.getData().getDoubleExtra("lng", 0);

                // Fetch Human-Readable Address immediately
                fetchAddressFromCoords(currentLat, currentLng);
            }
        });
    }

    // ==========================================
    //          RECYCLER ADAPTER
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
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_teacher_classes_card, parent, false);
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(@NonNull VH holder, int position) {
            TuitionModel t = list.get(position);

            holder.title.setText(t.getTitle());
            holder.fee.setText("₹" + t.getFee() + "/mo");
            holder.students.setText("Max: " + t.getMaxStudents());

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
                title = v.findViewById(R.id.tvCardTitle);
                tags = v.findViewById(R.id.tvCardTags);
                fee = v.findViewById(R.id.tvCardFee);
                students = v.findViewById(R.id.tvCardStudents);
                img = v.findViewById(R.id.imgCardThumb);
            }
        }
    }
}