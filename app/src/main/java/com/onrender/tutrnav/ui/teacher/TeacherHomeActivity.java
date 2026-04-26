package com.onrender.tutrnav.ui.teacher;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.onrender.tutrnav.R;
import com.onrender.tutrnav.ui.common.ProfileActivity;

public class TeacherHomeActivity extends AppCompatActivity {

    // --- UI Components ---
    private FrameLayout navDashboard, navMyTuition, navSchedule;
    private ImageView iconDash, iconTuition, iconSchedule;

    private ViewPager2 viewPager;
    private TextView tvGreeting, tvStatus;
    private ImageView imgProfileSmall;
    private View cardProfile;

    // --- Configuration ---
    private final int COLOR_ACTIVE = Color.parseColor("#FFCA28"); // Golden Yellow
    private final int COLOR_INACTIVE = Color.parseColor("#9FA8DA"); // Muted Indigo
    private String firstName = "Teacher"; // Dynamic name storage

    // --- Data Engine ---
    private TeacherViewModel viewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_teacher_main);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, 0);
            return insets;
        });

        viewModel = new ViewModelProvider(this).get(TeacherViewModel.class);

        initViews();
        setupViewPager();
        loadProfile();
        setupSmartBackPress();

        if (cardProfile != null) {
            cardProfile.setOnClickListener(v -> startActivity(new Intent(this, ProfileActivity.class)));
        }
    }

    private void initViews() {
        navDashboard = findViewById(R.id.navDashboard);
        navMyTuition = findViewById(R.id.navMyTuition);
        navSchedule = findViewById(R.id.navSchedule);

        iconDash = findViewById(R.id.iconDash);
        iconTuition = findViewById(R.id.iconTuition);
        iconSchedule = findViewById(R.id.iconSchedule);

        viewPager = findViewById(R.id.viewPager);
        imgProfileSmall = findViewById(R.id.imgProfileSmall);
        tvGreeting = findViewById(R.id.tvGreeting);
        tvStatus = findViewById(R.id.tvStatus);
        cardProfile = findViewById(R.id.cardProfile);

        navDashboard.setOnClickListener(v -> viewPager.setCurrentItem(0, true));
        navMyTuition.setOnClickListener(v -> viewPager.setCurrentItem(1, true));
        navSchedule.setOnClickListener(v -> viewPager.setCurrentItem(2, true));
    }

    private void setupViewPager() {
        TeacherPagerAdapter adapter = new TeacherPagerAdapter(this);
        viewPager.setAdapter(adapter);
        viewPager.setOffscreenPageLimit(3);
        viewPager.getChildAt(0).setOverScrollMode(View.OVER_SCROLL_NEVER);

        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                updateNavUI(position);
            }
        });
    }

    private void updateNavUI(int pos) {
        animateIcon(iconDash, pos == 0);
        animateIcon(iconTuition, pos == 1);
        animateIcon(iconSchedule, pos == 2);

        // Dynamically update Top Texts based on Fragment Index
        updateHeaderText(pos);
    }

    private void updateHeaderText(int pos) {
        if (tvGreeting == null || tvStatus == null) return;
        switch (pos) {
            case 0: // Dashboard
                tvGreeting.setText("Hi " + firstName + "!");
                tvStatus.setText("Dashboard");
                break;
            case 1: // My Tuitions
                tvGreeting.setText("Your Tuitions");
                tvStatus.setText("Manage your tuitions here");
                break;
            case 2: // Students (Schedule)
                tvGreeting.setText("Your Students");
                tvStatus.setText("Manage your students here");
                break;
        }
    }

    private void animateIcon(ImageView icon, boolean isActive) {
        icon.setColorFilter(isActive ? COLOR_ACTIVE : COLOR_INACTIVE);
        icon.animate()
                .scaleX(isActive ? 1.25f : 1.0f)
                .scaleY(isActive ? 1.25f : 1.0f)
                .setDuration(250)
                .start();
    }

    private void loadProfile() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            String name = user.getDisplayName();
            firstName = (name != null && !name.trim().isEmpty()) ? name.split(" ")[0] : (user.getPhoneNumber() != null ? user.getPhoneNumber().split(" ")[0] : "Teacher");

            // Apply immediately to current Tab
            updateHeaderText(viewPager.getCurrentItem());

            if (user.getPhotoUrl() != null && imgProfileSmall != null) {
                Glide.with(this)
                        .load(user.getPhotoUrl())
                        .circleCrop()
                        .placeholder(R.mipmap.ic_launcher)
                        .into(imgProfileSmall);
            }
        } else {
            updateHeaderText(viewPager.getCurrentItem());
        }
    }

    private void setupSmartBackPress() {
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (viewPager.getCurrentItem() != 0) {
                    viewPager.setCurrentItem(0, true);
                } else {
                    setEnabled(false);
                    getOnBackPressedDispatcher().onBackPressed();
                }
            }
        });
    }

    private static class TeacherPagerAdapter extends FragmentStateAdapter {
        public TeacherPagerAdapter(@NonNull FragmentActivity fa) {
            super(fa);
        }

        @NonNull
        @Override
        public Fragment createFragment(int position) {
            switch (position) {
                case 0: return new TeacherDashboardFragment();
                case 1: return new TeacherClassesFragment();
                case 2: return new TeacherStudentsFragment();
                default: return new TeacherDashboardFragment();
            }
        }

        @Override
        public int getItemCount() {
            return 3;
        }
    }
}