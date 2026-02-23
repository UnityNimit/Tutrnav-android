package com.onrender.tutrnav;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.FirebaseException;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.auth.PhoneAuthCredential;
import com.google.firebase.auth.PhoneAuthOptions;
import com.google.firebase.auth.PhoneAuthProvider;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class SignUpActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private GoogleSignInClient mGoogleSignInClient;

    // UI Elements
    private EditText etName, etEmailOrPhone, etPassword;
    private EditText otp1, otp2, otp3, otp4, otp5, otp6;
    private TextView btnGetOtp, btnGoogle;
    private Button btnSignUp;
    private ImageView btnBack;
    private MaterialButton btnGoToSignIn;

    // Phone Auth Variables
    private String mVerificationId;
    private PhoneAuthProvider.ForceResendingToken mResendToken;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_sign_up);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        initViews();
        setupOtpInputs();
        setupGoogleSignIn();
        setupClickListeners();
    }

    private void initViews() {
        etName = findViewById(R.id.etName);
        etEmailOrPhone = findViewById(R.id.etEmail); // ID is etEmail in XML, but acts as both
        etPassword = findViewById(R.id.etPassword);

        otp1 = findViewById(R.id.otp1);
        otp2 = findViewById(R.id.otp2);
        otp3 = findViewById(R.id.otp3);
        otp4 = findViewById(R.id.otp4);
        otp5 = findViewById(R.id.otp5);
        otp6 = findViewById(R.id.otp6);

        btnGetOtp = findViewById(R.id.btnGetOtp);
        btnSignUp = findViewById(R.id.btnSignUp);
        btnBack = findViewById(R.id.btnBack);
        btnGoToSignIn = findViewById(R.id.btnGoToSignIn);
        btnGoogle = findViewById(R.id.btnGoogle);
    }

    // --- SMART OTP AUTO-FOCUS LOGIC ---
    private void setupOtpInputs() {
        EditText[] otps = {otp1, otp2, otp3, otp4, otp5, otp6};
        for (int i = 0; i < otps.length; i++) {
            final int currentIndex = i;
            otps[i].addTextChangedListener(new TextWatcher() {
                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    if (s.length() == 1 && currentIndex < otps.length - 1) {
                        // Move to next box when a number is typed
                        otps[currentIndex + 1].requestFocus();
                    } else if (s.length() == 0 && currentIndex > 0) {
                        // Move to previous box when deleted
                        otps[currentIndex - 1].requestFocus();
                    }
                }
                @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                @Override public void afterTextChanged(Editable s) {}
            });
        }
    }

    private void setupClickListeners() {
        btnBack.setOnClickListener(v -> finish());

        btnGoToSignIn.setOnClickListener(v -> {
            startActivity(new Intent(SignUpActivity.this, SignInActivity.class));
            finish();
        });

        btnGetOtp.setOnClickListener(v -> handleGetOtp());

        btnSignUp.setOnClickListener(v -> handleSignUp());

        btnGoogle.setOnClickListener(v -> {
            Intent signInIntent = mGoogleSignInClient.getSignInIntent();
            googleSignInLauncher.launch(signInIntent);
        });
    }

    // ==========================================
    //               AUTH LOGIC
    // ==========================================

    private void handleGetOtp() {
        String identifier = etEmailOrPhone.getText().toString().trim();

        if (identifier.isEmpty()) {
            Toast.makeText(this, "Enter a valid Phone Number", Toast.LENGTH_SHORT).show();
            return;
        }

        if (identifier.contains("@")) {
            Toast.makeText(this, "OTP is for Phone Numbers. Use Password for Email.", Toast.LENGTH_LONG).show();
            return;
        }

        // Format Phone Number (Assume India +91 if no country code provided)
        if (!identifier.startsWith("+")) {
            identifier = "+91" + identifier;
        }

        Toast.makeText(this, "Sending OTP...", Toast.LENGTH_SHORT).show();

        PhoneAuthOptions options = PhoneAuthOptions.newBuilder(mAuth)
                .setPhoneNumber(identifier)
                .setTimeout(60L, TimeUnit.SECONDS)
                .setActivity(this)
                .setCallbacks(new PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                    @Override
                    public void onVerificationCompleted(@NonNull PhoneAuthCredential credential) {
                        // Auto-retrieval successful
                        signInWithPhoneCredential(credential);
                    }

                    @Override
                    public void onVerificationFailed(@NonNull FirebaseException e) {
                        Toast.makeText(SignUpActivity.this, "OTP Failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    }

                    @Override
                    public void onCodeSent(@NonNull String verificationId, @NonNull PhoneAuthProvider.ForceResendingToken token) {
                        mVerificationId = verificationId;
                        mResendToken = token;
                        Toast.makeText(SignUpActivity.this, "OTP Sent Successfully!", Toast.LENGTH_SHORT).show();
                    }
                })
                .build();
        PhoneAuthProvider.verifyPhoneNumber(options);
    }

    private void handleSignUp() {
        String name = etName.getText().toString().trim();
        String identifier = etEmailOrPhone.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        if (TextUtils.isEmpty(name) || TextUtils.isEmpty(identifier)) {
            Toast.makeText(this, "Name and Email/Phone are required", Toast.LENGTH_SHORT).show();
            return;
        }

        if (identifier.contains("@")) {
            // EMAIL SIGN UP
            if (TextUtils.isEmpty(password)) {
                Toast.makeText(this, "Password is required for Email Sign Up", Toast.LENGTH_SHORT).show();
                return;
            }
            signUpWithEmail(name, identifier, password);

        } else {
            // PHONE SIGN UP
            String otpCode = otp1.getText().toString() + otp2.getText().toString() +
                    otp3.getText().toString() + otp4.getText().toString() +
                    otp5.getText().toString() + otp6.getText().toString();

            if (otpCode.length() < 6) {
                Toast.makeText(this, "Please enter the complete 6-digit OTP", Toast.LENGTH_SHORT).show();
                return;
            }
            if (mVerificationId == null) {
                Toast.makeText(this, "Please click 'Get OTP' first", Toast.LENGTH_SHORT).show();
                return;
            }

            PhoneAuthCredential credential = PhoneAuthProvider.getCredential(mVerificationId, otpCode);
            signInWithPhoneCredential(credential);
        }
    }

    private void signUpWithEmail(String name, String email, String password) {
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        saveUserToFirestoreAndNavigate(user, name, email, "");
                    } else {
                        Toast.makeText(SignUpActivity.this, "Sign Up Failed: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void signInWithPhoneCredential(PhoneAuthCredential credential) {
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        String name = etName.getText().toString().trim();
                        String phone = etEmailOrPhone.getText().toString().trim();
                        if (!phone.startsWith("+")) phone = "+91" + phone;

                        saveUserToFirestoreAndNavigate(user, name, "", phone);
                    } else {
                        Toast.makeText(SignUpActivity.this, "Invalid OTP", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    // ==========================================
    //        GOOGLE SIGN IN (FROM AUTH)
    // ==========================================

    private void setupGoogleSignIn() {
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();
        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);
    }

    private final ActivityResultLauncher<Intent> googleSignInLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(result.getData());
                    try {
                        GoogleSignInAccount account = task.getResult(ApiException.class);
                        if (account != null) {
                            AuthCredential credential = GoogleAuthProvider.getCredential(account.getIdToken(), null);
                            mAuth.signInWithCredential(credential).addOnCompleteListener(this, authTask -> {
                                if (authTask.isSuccessful()) {
                                    FirebaseUser user = mAuth.getCurrentUser();
                                    saveUserToFirestoreAndNavigate(user, user.getDisplayName(), user.getEmail(), "");
                                } else {
                                    Toast.makeText(this, "Google Auth Failed", Toast.LENGTH_SHORT).show();
                                }
                            });
                        }
                    } catch (ApiException e) {
                        Toast.makeText(this, "Google Sign In Cancelled", Toast.LENGTH_SHORT).show();
                    }
                }
            }
    );

    // ==========================================
    //        DATABASE & NAVIGATION LOGIC
    // ==========================================

    private void saveUserToFirestoreAndNavigate(FirebaseUser user, String name, String email, String phone) {
        if (user == null) return;

        // 1. Update Profile internally in Firebase Auth
        if (name != null && !name.isEmpty()) {
            UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                    .setDisplayName(name).build();
            user.updateProfile(profileUpdates);
        }

        // 2. Fetch selected Role from Onboarding
        SharedPreferences prefs = getSharedPreferences("TutrnavPrefs", MODE_PRIVATE);
        String userType = prefs.getString("userType", "student");

        // 3. Save extended profile details to Firestore "users" collection
        Map<String, Object> userData = new HashMap<>();
        userData.put("uid", user.getUid());
        userData.put("name", name != null ? name : "User");
        userData.put("role", userType);

        if (!email.isEmpty()) userData.put("email", email);
        if (!phone.isEmpty()) userData.put("phone", phone);
        if (user.getPhotoUrl() != null) userData.put("photoUrl", user.getPhotoUrl().toString());

        // SetOptions.merge() prevents overwriting existing data if a user re-authenticates
        db.collection("users").document(user.getUid())
                .set(userData, SetOptions.merge())
                .addOnCompleteListener(task -> {
                    Toast.makeText(this, "Welcome " + name + "!", Toast.LENGTH_SHORT).show();
                    navigateToHome(userType);
                });
    }

    private void navigateToHome(String userType) {
        Intent intent;
        if ("teacher".equals(userType)) {
            intent = new Intent(SignUpActivity.this, TeacherHomeActivity.class);
        } else {
            intent = new Intent(SignUpActivity.this, StudentHomeActivity.class);
        }

        // Clear stack so user can't press back to return to Sign Up
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}