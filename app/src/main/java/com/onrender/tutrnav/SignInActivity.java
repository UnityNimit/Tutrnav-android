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
import com.google.firebase.FirebaseException;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.auth.PhoneAuthCredential;
import com.google.firebase.auth.PhoneAuthOptions;
import com.google.firebase.auth.PhoneAuthProvider;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class SignInActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private GoogleSignInClient mGoogleSignInClient;

    // UI Elements
    private EditText etEmailOrPhone, etPassword;
    private EditText otp1, otp2, otp3, otp4, otp5, otp6;
    private Button btnSignIn;
    private ImageView btnBack;
    private TextView btnGoogle, tvForgot, btnGetOtp;

    // Phone Auth Variables
    private String mVerificationId;
    private PhoneAuthProvider.ForceResendingToken mResendToken;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_sign_in);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Initialize Firebase
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        initViews();
        setupOtpInputs();
        setupGoogleSignIn();
        setupListeners();
    }

    private void initViews() {
        etEmailOrPhone = findViewById(R.id.etEmail); // IDs match your XML exactly
        etPassword = findViewById(R.id.etPassword);
        btnSignIn = findViewById(R.id.btnSignIn);
        btnBack = findViewById(R.id.btnBack);
        btnGoogle = findViewById(R.id.btnGoogle);
        tvForgot = findViewById(R.id.tvForgot);
        btnGetOtp = findViewById(R.id.btnGetOtp);

        otp1 = findViewById(R.id.otp1);
        otp2 = findViewById(R.id.otp2);
        otp3 = findViewById(R.id.otp3);
        otp4 = findViewById(R.id.otp4);
        otp5 = findViewById(R.id.otp5);
        otp6 = findViewById(R.id.otp6);
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
                        // Jump to next box
                        otps[currentIndex + 1].requestFocus();
                    } else if (s.length() == 0 && currentIndex > 0) {
                        // Jump to previous box on delete
                        otps[currentIndex - 1].requestFocus();
                    }
                }
                @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                @Override public void afterTextChanged(Editable s) {}
            });
        }
    }

    private void setupGoogleSignIn() {
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();
        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);
    }

    private void setupListeners() {
        btnBack.setOnClickListener(v -> finish());

        btnGetOtp.setOnClickListener(v -> handleGetOtp());

        btnSignIn.setOnClickListener(v -> handleSignIn());

        btnGoogle.setOnClickListener(v -> {
            Intent signInIntent = mGoogleSignInClient.getSignInIntent();
            googleSignInLauncher.launch(signInIntent);
        });

        tvForgot.setOnClickListener(v -> {
            String identifier = etEmailOrPhone.getText().toString().trim();
            if (TextUtils.isEmpty(identifier) || !identifier.contains("@")) {
                Toast.makeText(this, "Please enter your Email address first to reset password", Toast.LENGTH_LONG).show();
            } else {
                mAuth.sendPasswordResetEmail(identifier)
                        .addOnCompleteListener(task -> {
                            if(task.isSuccessful()) {
                                Toast.makeText(this, "Reset link sent to " + identifier, Toast.LENGTH_SHORT).show();
                            } else {
                                Toast.makeText(this, "Error: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                            }
                        });
            }
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
                        // Instant verification (if auto-detected)
                        signInWithCredential(credential);
                    }

                    @Override
                    public void onVerificationFailed(@NonNull FirebaseException e) {
                        Toast.makeText(SignInActivity.this, "OTP Failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    }

                    @Override
                    public void onCodeSent(@NonNull String verificationId, @NonNull PhoneAuthProvider.ForceResendingToken token) {
                        mVerificationId = verificationId;
                        mResendToken = token;
                        Toast.makeText(SignInActivity.this, "OTP Sent! Check your SMS.", Toast.LENGTH_SHORT).show();
                    }
                })
                .build();
        PhoneAuthProvider.verifyPhoneNumber(options);
    }

    private void handleSignIn() {
        String identifier = etEmailOrPhone.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        if (TextUtils.isEmpty(identifier)) {
            Toast.makeText(this, "Please enter Email or Phone Number", Toast.LENGTH_SHORT).show();
            return;
        }

        if (identifier.contains("@")) {
            // --- EMAIL SIGN IN ---
            if (TextUtils.isEmpty(password)) {
                Toast.makeText(this, "Please enter your password", Toast.LENGTH_SHORT).show();
                return;
            }
            mAuth.signInWithEmailAndPassword(identifier, password)
                    .addOnCompleteListener(this, task -> {
                        if (task.isSuccessful()) {
                            syncRoleAndNavigate();
                        } else {
                            Toast.makeText(SignInActivity.this, "Invalid Email or Password", Toast.LENGTH_SHORT).show();
                        }
                    });

        } else {
            // --- PHONE OTP SIGN IN ---
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
            signInWithCredential(credential);
        }
    }

    private void signInWithCredential(AuthCredential credential) {
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        syncRoleAndNavigate();
                    } else {
                        Toast.makeText(SignInActivity.this, "Invalid OTP or Credentials", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    // ==========================================
    //        GOOGLE SIGN IN
    // ==========================================

    private final ActivityResultLauncher<Intent> googleSignInLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(result.getData());
                    try {
                        GoogleSignInAccount account = task.getResult(ApiException.class);
                        if (account != null) {
                            AuthCredential credential = GoogleAuthProvider.getCredential(account.getIdToken(), null);
                            signInWithCredential(credential);
                        }
                    } catch (ApiException e) {
                        Toast.makeText(this, "Google Sign In Failed", Toast.LENGTH_SHORT).show();
                    }
                }
            }
    );

    // ==========================================
    //    DATA SYNC & ROUTING (PRODUCTION LEVEL)
    // ==========================================

    /**
     * Cross-Device Logic:
     * When a user logs in, we shouldn't trust SharedPreferences because they might
     * have signed up as a Teacher on another device. We check Firestore first.
     */
    private void syncRoleAndNavigate() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) return;

        db.collection("users").document(user.getUid()).get()
                .addOnSuccessListener(documentSnapshot -> {
                    String role = "student"; // default fallback

                    if (documentSnapshot.exists() && documentSnapshot.getString("role") != null) {
                        // User exists in DB, fetch their exact role
                        role = documentSnapshot.getString("role");

                        // Update local preferences to match server
                        SharedPreferences prefs = getSharedPreferences("TutrnavPrefs", MODE_PRIVATE);
                        prefs.edit().putString("userType", role).apply();

                    } else {
                        // Rare case: User logged in via Google but it's their very first time.
                        // We use the role they selected on Onboarding.
                        SharedPreferences prefs = getSharedPreferences("TutrnavPrefs", MODE_PRIVATE);
                        role = prefs.getString("userType", "student");

                        // Create their DB entry silently
                        Map<String, Object> userData = new HashMap<>();
                        userData.put("uid", user.getUid());
                        userData.put("name", user.getDisplayName());
                        userData.put("email", user.getEmail());
                        userData.put("role", role);
                        if (user.getPhoneNumber() != null) userData.put("phone", user.getPhoneNumber());
                        if (user.getPhotoUrl() != null) userData.put("photoUrl", user.getPhotoUrl().toString());

                        db.collection("users").document(user.getUid()).set(userData, SetOptions.merge());
                    }

                    Toast.makeText(this, "Welcome Back!", Toast.LENGTH_SHORT).show();
                    navigateToHome(role);
                })
                .addOnFailureListener(e -> {
                    // Network failure, fallback to local memory
                    SharedPreferences prefs = getSharedPreferences("TutrnavPrefs", MODE_PRIVATE);
                    navigateToHome(prefs.getString("userType", "student"));
                });
    }

    private void navigateToHome(String userType) {
        Intent intent;
        if ("teacher".equals(userType)) {
            intent = new Intent(SignInActivity.this, TeacherHomeActivity.class);
        } else {
            intent = new Intent(SignInActivity.this, StudentHomeActivity.class);
        }

        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}