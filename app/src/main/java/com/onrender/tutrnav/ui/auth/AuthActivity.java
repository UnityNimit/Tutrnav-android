package com.onrender.tutrnav.ui.auth;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ArgbEvaluator;
import android.animation.ValueAnimator;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.interpolator.view.animation.FastOutSlowInInterpolator;
import androidx.recyclerview.widget.RecyclerView;
import androidx.transition.ChangeBounds;
import androidx.transition.Fade;
import androidx.transition.TransitionManager;
import androidx.transition.TransitionSet;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;
import com.onrender.tutrnav.R;
import com.onrender.tutrnav.ui.student.StudentHomeActivity;
import com.onrender.tutrnav.ui.teacher.TeacherHomeActivity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AuthActivity extends AppCompatActivity {

    // --- Dynamic Background & Engine ---
    private ConstraintLayout rootLayout;
    private OrganicBlobView organicBlob;
    private ViewPager2 viewPager;
    private final ArgbEvaluator colorEvaluator = new ArgbEvaluator();
    private final Handler sliderHandler = new Handler(Looper.getMainLooper());

    private final int[][] GRADIENT_COLORS = {
            {0xFF859AE0, 0xFF4F5CA4, 0xFF1C216A},
            {0xFFE0DE85, 0xFFA4994F, 0xFF6A5A1C},
            {0xFF85E0AC, 0xFF4FA471, 0xFF1C6A30},
            {0xFFDB85E0, 0xFFAA4FA0, 0xFF6A1C5A}
    };

    // --- Morphing UI ---
    private CardView bottomSheet;
    private LinearLayout layoutRoleSelection, layoutAuthSelection, layoutSignInForm, layoutSignUpForm;
    private TextView tvSelectedRole;

    // --- Inputs ---
    private EditText etSignInEmail, etSignInPass, etSignUpName, etSignUpEmail, etSignUpPass;

    // --- Firebase & Data ---
    private SharedPreferences prefs;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private GoogleSignInClient mGoogleSignInClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_auth_main);

        rootLayout = findViewById(R.id.main);
        ViewCompat.setOnApplyWindowInsetsListener(rootLayout, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        prefs = getSharedPreferences("TutrnavPrefs", MODE_PRIVATE);
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        initViews();
        setupInfiniteSlideshow();
        setupGoogleAuth();
        setupClickListeners();
        checkInitialState();
    }

    private void initViews() {
        organicBlob = findViewById(R.id.organicBlob);
        viewPager = findViewById(R.id.viewPager);
        bottomSheet = findViewById(R.id.bottomSheet);

        layoutRoleSelection = findViewById(R.id.layoutRoleSelection);
        layoutAuthSelection = findViewById(R.id.layoutAuthSelection);
        layoutSignInForm = findViewById(R.id.layoutSignInForm);
        layoutSignUpForm = findViewById(R.id.layoutSignUpForm);

        tvSelectedRole = findViewById(R.id.tvSelectedRole);

        etSignInEmail = findViewById(R.id.etSignInEmail);
        etSignInPass = findViewById(R.id.etSignInPass);

        etSignUpName = findViewById(R.id.etSignUpName);
        etSignUpEmail = findViewById(R.id.etSignUpEmail);
        etSignUpPass = findViewById(R.id.etSignUpPass);
    }

    // ==========================================
    //    DYNAMIC AESTHETIC SLIDESHOW ENGINE
    // ==========================================

    private void setupInfiniteSlideshow() {
        List<SlideItem> sliderItems = new ArrayList<>();
        sliderItems.add(new SlideItem("Find Tutors Nearby", "Map-based search for local teachers", R.drawable.pic_1));
        sliderItems.add(new SlideItem("Master New Skills", "From Music to Kung Fu", R.drawable.pic_2));
        sliderItems.add(new SlideItem("Expert Guidance", "Verified teachers for best results", R.drawable.pic_3));
        sliderItems.add(new SlideItem("Flexible Schedule", "Book classes at your convenience", R.drawable.pic_4));

        viewPager.setAdapter(new SlideAdapter(sliderItems));
        viewPager.setPageTransformer((page, position) -> {
            page.setTranslationX(-position * page.getWidth());
            page.setAlpha(1 - Math.abs(position));
        });

        int midPoint = Integer.MAX_VALUE / 2;
        viewPager.setCurrentItem(midPoint - (midPoint % sliderItems.size()), false);

        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
                int currentIndex = position % GRADIENT_COLORS.length;
                int nextIndex = (position + 1) % GRADIENT_COLORS.length;

                int startColor = (int) colorEvaluator.evaluate(positionOffset, GRADIENT_COLORS[currentIndex][0], GRADIENT_COLORS[nextIndex][0]);
                int centerColor = (int) colorEvaluator.evaluate(positionOffset, GRADIENT_COLORS[currentIndex][1], GRADIENT_COLORS[nextIndex][1]);
                int endColor = (int) colorEvaluator.evaluate(positionOffset, GRADIENT_COLORS[currentIndex][2], GRADIENT_COLORS[nextIndex][2]);

                GradientDrawable background = new GradientDrawable(GradientDrawable.Orientation.TR_BL, new int[]{startColor, centerColor, endColor});
                background.setDither(true);
                rootLayout.setBackground(background);

                if (organicBlob != null) {
                    organicBlob.setMorphState(position % 4, (position + 1) % 4, positionOffset);
                }
            }

            @Override
            public void onPageSelected(int position) {
                sliderHandler.removeCallbacks(sliderRunnable);
                sliderHandler.postDelayed(sliderRunnable, 3500);
            }
        });
    }

    private final Runnable sliderRunnable = this::slowSmoothScroll;

    private void slowSmoothScroll() {
        if (viewPager.isFakeDragging()) return;
        ValueAnimator animator = ValueAnimator.ofFloat(0, viewPager.getWidth());
        animator.setDuration(1200);
        animator.setInterpolator(new AccelerateDecelerateInterpolator());
        final float[] previousPx = {0f};
        animator.addUpdateListener(anim -> {
            float currentPx = (float) anim.getAnimatedValue();
            viewPager.fakeDragBy(-(currentPx - previousPx[0]));
            previousPx[0] = currentPx;
        });
        animator.addListener(new AnimatorListenerAdapter() {
            @Override public void onAnimationStart(Animator animation) { viewPager.beginFakeDrag(); }
            @Override public void onAnimationEnd(Animator animation) { viewPager.endFakeDrag(); }
        });
        animator.start();
    }

    @Override
    protected void onPause() { super.onPause(); sliderHandler.removeCallbacks(sliderRunnable); }
    @Override
    protected void onResume() { super.onResume(); sliderHandler.postDelayed(sliderRunnable, 3500); }

    // ==========================================
    //          STATE ANIMATION LOGIC
    // ==========================================

    private void checkInitialState() {
        String savedRole = prefs.getString("userType", "");
        if (!savedRole.isEmpty()) {
            tvSelectedRole.setText(savedRole.substring(0, 1).toUpperCase() + savedRole.substring(1) + " Login");
            switchState(layoutAuthSelection, false);
        } else {
            switchState(layoutRoleSelection, false);
        }
    }

    private void switchState(View targetLayout, boolean isForm) {
        TransitionSet set = new TransitionSet()
                .addTransition(new ChangeBounds())
                .addTransition(new Fade())
                .setDuration(400)
                .setOrdering(TransitionSet.ORDERING_TOGETHER)
                .setInterpolator(new FastOutSlowInInterpolator());

        TransitionManager.beginDelayedTransition(bottomSheet, set);

        // Hide all
        layoutRoleSelection.setVisibility(View.GONE);
        layoutAuthSelection.setVisibility(View.GONE);
        layoutSignInForm.setVisibility(View.GONE);
        layoutSignUpForm.setVisibility(View.GONE);

        // Show Target
        targetLayout.setVisibility(View.VISIBLE);

        // Aesthetic Focus Mode (Dims background if a form is open)
        float targetAlpha = isForm ? 0.1f : 1.0f;
        organicBlob.animate().alpha(targetAlpha).setDuration(400).start();
        viewPager.animate().alpha(targetAlpha).setDuration(400).start();
    }

    // ==========================================
    //          INTERACTIONS LOGIC
    // ==========================================

    private void setupClickListeners() {
        // Step 1: Role Selection to Auth Selection
        findViewById(R.id.btnRoleTeacher).setOnClickListener(v -> {
            prefs.edit().putString("userType", "teacher").apply();
            tvSelectedRole.setText("Teacher Login");
            switchState(layoutAuthSelection, false);
        });
        findViewById(R.id.btnRoleStudent).setOnClickListener(v -> {
            prefs.edit().putString("userType", "student").apply();
            tvSelectedRole.setText("Student Login");
            switchState(layoutAuthSelection, false);
        });

        // Backwards Navigation
        findViewById(R.id.btnBackToRole).setOnClickListener(v -> {
            prefs.edit().remove("userType").apply();
            switchState(layoutRoleSelection, false);
        });
        findViewById(R.id.btnBackToAuthFromSignIn).setOnClickListener(v -> switchState(layoutAuthSelection, false));
        findViewById(R.id.btnBackToAuthFromSignUp).setOnClickListener(v -> switchState(layoutAuthSelection, false));

        // State Transition (Buttons and Toggles)
        findViewById(R.id.btnGoToSignIn).setOnClickListener(v -> switchState(layoutSignInForm, true));
        findViewById(R.id.btnGoToSignUp).setOnClickListener(v -> switchState(layoutSignUpForm, true));
        findViewById(R.id.btnToggleToSignIn).setOnClickListener(v -> switchState(layoutSignInForm, true));
        findViewById(R.id.btnToggleToSignUp).setOnClickListener(v -> switchState(layoutSignUpForm, true));

        // Submit Buttons
        findViewById(R.id.btnDoSignIn).setOnClickListener(v -> handleSignIn());
        findViewById(R.id.btnDoSignUp).setOnClickListener(v -> handleSignUp());

        // Google Auth (Attach to all instances in XML to keep logic centralized)
        View.OnClickListener googleClick = v -> signInLauncher.launch(mGoogleSignInClient.getSignInIntent());
        findViewById(R.id.btnAuthGoogle).setOnClickListener(googleClick);
        findViewById(R.id.btnGoogleSignIn).setOnClickListener(googleClick);
        findViewById(R.id.btnGoogleSignUp).setOnClickListener(googleClick);
    }

    // ==========================================
    //           FIREBASE AUTHENTICATION
    // ==========================================

    private void handleSignIn() {
        String email = etSignInEmail.getText().toString().trim();
        String pass = etSignInPass.getText().toString().trim();

        if (email.isEmpty()) { Toast.makeText(this, "Enter Email", Toast.LENGTH_SHORT).show(); return; }
        if (pass.isEmpty()) { Toast.makeText(this, "Enter Password", Toast.LENGTH_SHORT).show(); return; }

        mAuth.signInWithEmailAndPassword(email, pass)
                .addOnSuccessListener(authResult -> navigateToHome())
                .addOnFailureListener(e -> Toast.makeText(this, "Login Failed", Toast.LENGTH_SHORT).show());
    }

    private void handleSignUp() {
        String name = etSignUpName.getText().toString().trim();
        String email = etSignUpEmail.getText().toString().trim();
        String pass = etSignUpPass.getText().toString().trim();

        if (name.isEmpty() || email.isEmpty()) { Toast.makeText(this, "Fill required fields", Toast.LENGTH_SHORT).show(); return; }
        if (pass.isEmpty()) { Toast.makeText(this, "Enter Password", Toast.LENGTH_SHORT).show(); return; }

        mAuth.createUserWithEmailAndPassword(email, pass)
                .addOnSuccessListener(result -> saveUserAndNavigate(result.getUser(), name, email))
                .addOnFailureListener(e -> Toast.makeText(this, "Sign Up Failed: " + e.getMessage(), Toast.LENGTH_LONG).show());
    }

    // --- Google Logic ---
    private void setupGoogleAuth() {
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();
        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);
    }

    private final ActivityResultLauncher<Intent> signInLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    try {
                        GoogleSignInAccount account = GoogleSignIn.getSignedInAccountFromIntent(result.getData()).getResult(ApiException.class);
                        if (account != null) {
                            mAuth.signInWithCredential(GoogleAuthProvider.getCredential(account.getIdToken(), null))
                                    .addOnSuccessListener(authResult -> {
                                        FirebaseUser u = authResult.getUser();
                                        saveUserAndNavigate(u, u.getDisplayName(), u.getEmail());
                                    }).addOnFailureListener(e -> Toast.makeText(this, "Auth Failed", Toast.LENGTH_SHORT).show());
                        }
                    } catch (ApiException e) { Toast.makeText(this, "Google Sign In Failed", Toast.LENGTH_SHORT).show(); }
                }
            }
    );

    // --- Database & Routing ---
    private void saveUserAndNavigate(FirebaseUser user, String name, String email) {
        if (user == null) return;
        if (name != null && !name.isEmpty()) {
            user.updateProfile(new UserProfileChangeRequest.Builder().setDisplayName(name).build());
        }

        String role = prefs.getString("userType", "student");
        Map<String, Object> userData = new HashMap<>();
        userData.put("uid", user.getUid());
        userData.put("role", role);
        userData.put("name", name != null ? name : "User");
        if (email != null && !email.isEmpty()) userData.put("email", email);
        if (user.getPhotoUrl() != null) userData.put("photoUrl", user.getPhotoUrl().toString());

        db.collection("users").document(user.getUid()).set(userData, SetOptions.merge())
                .addOnSuccessListener(v -> navigateToHome());
    }

    private void navigateToHome() {
        String role = prefs.getString("userType", "student");
        Intent intent = new Intent(this, "teacher".equals(role) ? TeacherHomeActivity.class : StudentHomeActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    // ==========================================
    //             INNER CLASSES
    // ==========================================

    private static class SlideItem {
        String title, desc; int imgRes;
        SlideItem(String t, String d, int i) { title = t; desc = d; imgRes = i; }
    }

    private static class SlideAdapter extends RecyclerView.Adapter<SlideAdapter.SlideVH> {
        List<SlideItem> items;
        SlideAdapter(List<SlideItem> items) { this.items = items; }

        @NonNull @Override
        public SlideVH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new SlideVH(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_auth_slide, parent, false));
        }

        @Override public void onBindViewHolder(@NonNull SlideVH holder, int position) {
            SlideItem item = items.get(position % items.size());
            holder.title.setText(item.title);
            holder.desc.setText(item.desc);
            holder.img.setImageResource(item.imgRes);
        }

        @Override public int getItemCount() { return Integer.MAX_VALUE; }

        static class SlideVH extends RecyclerView.ViewHolder {
            TextView title, desc; ImageView img;
            SlideVH(@NonNull View v) {
                super(v);
                title = v.findViewById(R.id.slideTitle);
                desc = v.findViewById(R.id.slideDesc);
                img = v.findViewById(R.id.slideImage);
            }
        }
    }
}