package com.onrender.tutrnav.ui.common;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.text.InputType;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.PickVisualMediaRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.bumptech.glide.Glide;
import com.cloudinary.android.MediaManager;
import com.cloudinary.android.callback.ErrorInfo;
import com.cloudinary.android.callback.UploadCallback;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;
import com.onrender.tutrnav.R;
import com.onrender.tutrnav.ui.auth.AuthActivity; // <-- Imported the unified AuthActivity

import java.util.HashMap;
import java.util.Map;

public class ProfileActivity extends AppCompatActivity {

    // Cloudinary Config - Ensure this preset exists in your Cloudinary Dashboard!
    private static final String UPLOAD_PRESET = "tutornav_preset";

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private ActivityResultLauncher<PickVisualMediaRequest> pickMedia;

    // --- UI Components ---
    private ImageView imgProfile, btnViewPass;
    private CardView btnBack;
    private TextView tvProfileName, tvProfileEmail, tvProfilePhone, tvProfileExperience, tvProfilePass;
    private MaterialButton btnPolicies, btnSettings, btnSupport, btnShare, btnSignOut, btnDeleteAccount;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_common_profile);

        setupWindowInsets();

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        initViews();
        setupImagePicker();
        setupClickListeners();
    }

    @Override
    protected void onStart() {
        super.onStart();
        // Load user data every time the screen becomes visible to ensure it's fresh
        loadExtendedUserInfo();
    }

    private void setupWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    private void initViews() {
        btnBack = findViewById(R.id.btnBack);
        imgProfile = findViewById(R.id.imgProfile);

        tvProfileName = findViewById(R.id.tvProfileName);
        tvProfileEmail = findViewById(R.id.tvProfileEmail);
        tvProfilePhone = findViewById(R.id.tvProfilePhone);
        tvProfileExperience = findViewById(R.id.tvProfileExperience);
        tvProfilePass = findViewById(R.id.tvProfilePass);
        btnViewPass = findViewById(R.id.btnViewPass);

        btnPolicies = findViewById(R.id.btnPolicies);
        btnSettings = findViewById(R.id.btnSettings);
        btnSupport = findViewById(R.id.btnSupport);
        btnShare = findViewById(R.id.btnShare);

        btnSignOut = findViewById(R.id.btnSignOut);
        btnDeleteAccount = findViewById(R.id.btnDeleteAccount);
    }

    private void setupClickListeners() {
        // Navigation
        btnBack.setOnClickListener(v -> finish());

        // Profile Image Upload
        imgProfile.setOnClickListener(v -> pickMedia.launch(
                new PickVisualMediaRequest.Builder()
                        .setMediaType(ActivityResultContracts.PickVisualMedia.ImageOnly.INSTANCE)
                        .build()
        ));

        // Editable Fields
        tvProfileName.setOnClickListener(v -> showEditDialog("Update Name", "name", tvProfileName, InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_WORDS));
        tvProfilePhone.setOnClickListener(v -> showEditDialog("Update Phone Number", "phone", tvProfilePhone, InputType.TYPE_CLASS_PHONE));
        tvProfileExperience.setOnClickListener(v -> showEditDialog("Update Experience / Bio", "experience", tvProfileExperience, InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES));

        // Password Handling
        btnViewPass.setOnClickListener(v -> handlePasswordClick());
        tvProfilePass.setOnClickListener(v -> handlePasswordClick());

        // Grid Actions
        btnPolicies.setOnClickListener(v -> showInfoDialog("Policies", "Your data is strictly secured using standard encryption protocols."));
        btnSettings.setOnClickListener(v -> openAppSettings());
        btnSupport.setOnClickListener(v -> contactSupport());
        btnShare.setOnClickListener(v -> shareApp());

        // Account Actions
        btnSignOut.setOnClickListener(v -> showLogoutDialog());
        btnDeleteAccount.setOnClickListener(v -> showDeleteAccountDialog());
    }

    // ==========================================
    //      DATA LOADING
    // ==========================================

    private void loadExtendedUserInfo() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) return;

        tvProfileName.setText(user.getDisplayName() != null ? user.getDisplayName() : "Set Name");

        // Email Verification UI
        if (user.getEmail() != null && !user.getEmail().isEmpty()) {
            if (user.isEmailVerified()) {
                tvProfileEmail.setText(user.getEmail() + " (Verified)");
                tvProfileEmail.setTextColor(getResources().getColor(android.R.color.holo_green_light, getTheme()));
                tvProfileEmail.setOnClickListener(null); // Remove listener if verified
            } else {
                tvProfileEmail.setText(user.getEmail() + "\n(Unverified - Tap to Verify)");
                tvProfileEmail.setTextColor(getResources().getColor(android.R.color.holo_red_light, getTheme()));
                tvProfileEmail.setOnClickListener(v -> sendEmailVerification(user));
            }
        } else {
            tvProfileEmail.setText("Signed in via Phone Number");
        }

        // Google Auth check for Password UI
        boolean isGoogle = user.getProviderData().stream().anyMatch(p -> "google.com".equals(p.getProviderId()));
        if (isGoogle) {
            tvProfilePass.setText("Signed in via Google");
            btnViewPass.setAlpha(0.5f);
        } else {
            tvProfilePass.setText("**************");
        }

        // Fetch remaining data from Firestore
        db.collection("users").document(user.getUid()).get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        String name = doc.getString("name");
                        String phone = doc.getString("phone");
                        String exp = doc.getString("experience");
                        String photo = doc.getString("photoUrl");

                        if (name != null && !name.isEmpty()) tvProfileName.setText(name);
                        if (exp != null && !exp.isEmpty()) tvProfileExperience.setText(exp);

                        if (phone != null && !phone.isEmpty()) {
                            tvProfilePhone.setText(phone);
                        } else if (user.getPhoneNumber() != null && !user.getPhoneNumber().isEmpty()) {
                            tvProfilePhone.setText(user.getPhoneNumber());
                            saveToFirestore("phone", user.getPhoneNumber());
                        }

                        String imageUrl = (photo != null && !photo.isEmpty()) ? photo : (user.getPhotoUrl() != null ? user.getPhotoUrl().toString() : null);
                        if (imageUrl != null) {
                            Glide.with(this).load(imageUrl).placeholder(R.mipmap.ic_launcher).circleCrop().into(imgProfile);
                        }
                    }
                });
    }

    // ==========================================
    //      EDITING & SAVING
    // ==========================================

    private void showEditDialog(String title, String fieldKey, TextView targetView, int inputType) {
        EditText input = new EditText(this);
        input.setInputType(inputType);
        input.setPadding(50, 40, 50, 40);

        String currentText = targetView.getText().toString();
        if (!currentText.startsWith("Not Set") && !currentText.startsWith("Add ") && !currentText.startsWith("Loading")) {
            input.setText(currentText);
        }

        new AlertDialog.Builder(this)
                .setTitle(title)
                .setView(input)
                .setPositiveButton("Save", (d, w) -> {
                    String val = input.getText().toString().trim();
                    if (!val.isEmpty()) {
                        targetView.setText(val);
                        saveToFirestore(fieldKey, val);

                        if ("name".equals(fieldKey) && mAuth.getCurrentUser() != null) {
                            mAuth.getCurrentUser().updateProfile(
                                    new UserProfileChangeRequest.Builder().setDisplayName(val).build()
                            );
                        }
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void saveToFirestore(String key, String value) {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) return;

        Map<String, Object> data = new HashMap<>();
        data.put(key, value);

        db.collection("users").document(user.getUid())
                .set(data, SetOptions.merge())
                .addOnFailureListener(e -> Toast.makeText(this, "Failed to save data.", Toast.LENGTH_SHORT).show());
    }

    // ==========================================
    //      IMAGE UPLOAD LOGIC
    // ==========================================

    private void setupImagePicker() {
        pickMedia = registerForActivityResult(new ActivityResultContracts.PickVisualMedia(), uri -> {
            if (uri != null) uploadToCloudinary(uri);
        });
    }

    private void uploadToCloudinary(Uri fileUri) {
        Toast.makeText(this, "Uploading image...", Toast.LENGTH_SHORT).show();

        MediaManager.get().upload(fileUri)
                .unsigned(UPLOAD_PRESET)
                .callback(new UploadCallback() {
                    @Override public void onStart(String requestId) {}
                    @Override public void onProgress(String requestId, long bytes, long totalBytes) {}

                    @Override
                    public void onSuccess(String requestId, Map resultData) {
                        String downloadUrl = (String) resultData.get("secure_url");
                        saveToFirestore("photoUrl", downloadUrl);

                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null) {
                            user.updateProfile(new UserProfileChangeRequest.Builder().setPhotoUri(Uri.parse(downloadUrl)).build());
                        }
                        runOnUiThread(() -> Glide.with(ProfileActivity.this).load(downloadUrl).circleCrop().into(imgProfile));
                    }

                    @Override
                    public void onError(String requestId, ErrorInfo error) {
                        runOnUiThread(() -> Toast.makeText(ProfileActivity.this, "Upload Failed", Toast.LENGTH_LONG).show());
                    }

                    @Override public void onReschedule(String requestId, ErrorInfo error) {}
                }).dispatch();
    }

    // ==========================================
    //      HELPER ACTIONS
    // ==========================================

    private void sendEmailVerification(FirebaseUser user) {
        user.sendEmailVerification().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                Toast.makeText(this, "Verification email sent. Please check your inbox.", Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(this, "Failed to send email.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void handlePasswordClick() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) return;

        boolean isGoogle = user.getProviderData().stream().anyMatch(p -> "google.com".equals(p.getProviderId()));
        if (isGoogle) {
            Toast.makeText(this, "Google account passwords are managed by Google.", Toast.LENGTH_LONG).show();
        } else if (user.getEmail() != null) {
            new AlertDialog.Builder(this)
                    .setTitle("Reset Password")
                    .setMessage("Send password reset email to " + user.getEmail() + "?")
                    .setPositiveButton("Send", (d, w) -> {
                        mAuth.sendPasswordResetEmail(user.getEmail());
                        Toast.makeText(this, "Reset Email Sent", Toast.LENGTH_SHORT).show();
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
        } else {
            Toast.makeText(this, "No email attached to this account.", Toast.LENGTH_SHORT).show();
        }
    }

    private void shareApp() {
        Intent sendIntent = new Intent(Intent.ACTION_SEND);
        sendIntent.putExtra(Intent.EXTRA_TEXT, "Hey! Check out TutorNav, the best app to find local tuitions!");
        sendIntent.setType("text/plain");
        startActivity(Intent.createChooser(sendIntent, "Share TutorNav"));
    }

    private void openAppSettings() {
        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        intent.setData(Uri.fromParts("package", getPackageName(), null));
        startActivity(intent);
    }

    private void contactSupport() {
        Intent intent = new Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:support@tutornav.com"));
        try {
            startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(this, "No email app found", Toast.LENGTH_SHORT).show();
        }
    }

    private void showInfoDialog(String title, String msg) {
        new AlertDialog.Builder(this).setTitle(title).setMessage(msg).setPositiveButton("OK", null).show();
    }

    // ==========================================
    //      ACCOUNT DESTRUCTION & ROUTING
    // ==========================================

    private void showLogoutDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Sign Out")
                .setMessage("Are you sure you want to log out?")
                .setPositiveButton("Yes", (d, w) -> {
                    mAuth.signOut();
                    navigateToAuth(); // <-- PERFECTED
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showDeleteAccountDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Delete Account")
                .setMessage("This cannot be undone. All your data will be permanently erased.")
                .setPositiveButton("Delete", (d, w) -> {
                    FirebaseUser user = mAuth.getCurrentUser();
                    if (user != null) {
                        db.collection("users").document(user.getUid()).delete().addOnCompleteListener(task -> {
                            user.delete().addOnSuccessListener(v -> navigateToAuth()); // <-- PERFECTED
                        });
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    /**
     * Completely resets the app state, clears the saved role, and forces the user
     * back to the newly unified AuthActivity.
     */
    private void navigateToAuth() {
        // CRITICAL FIX: Clear the saved role so the next person logging in doesn't get the wrong dashboard!
        SharedPreferences prefs = getSharedPreferences("TutrnavPrefs", MODE_PRIVATE);
        prefs.edit().remove("userType").apply();

        // Route to the new unified Auth screen
        Intent intent = new Intent(this, AuthActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}