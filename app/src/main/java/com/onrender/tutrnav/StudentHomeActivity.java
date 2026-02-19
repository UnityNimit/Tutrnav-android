package com.onrender.tutrnav;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class StudentHomeActivity extends AppCompatActivity {

    // Firebase
    private FirebaseAuth mAuth;
    private String currentUserName = "Student"; // Default fallback name

    // UI Components
    private TextView tvHi, tvName, tvSubtitle; // Added tvHi & tvSubtitle
    private ImageView imgProfileSmall;
    private CardView profileCard;
    private ImageView navHome, navSchedule, navMap, navNotif;
    private ViewPager2 viewPager;

    // Colors
    private final int COLOR_ACTIVE = Color.parseColor("#FFCA28"); // Gold/Yellow
    private final int COLOR_INACTIVE = Color.parseColor("#5C6BC0"); // Muted Purple

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_student_home);

        setupWindowInsets();

        // 1. Init Firebase
        mAuth = FirebaseAuth.getInstance();

        // 2. Init Views
        initViews();

        // 3. Setup ViewPager (Tabs)
        setupViewPager();

        // 4. Setup Click Listeners (Navigation & Profile)
        setupClickListeners();

        // 5. Set Initial State
        // We load user data first, which will then trigger the UI update
        loadUserData();
    }

    /**
     * Called every time the activity comes to the foreground.
     */
    @Override
    protected void onResume() {
        super.onResume();
        loadUserData();
    }

    private void setupWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, 0);
            return insets;
        });
    }

    private void initViews() {
        // Text Views
        tvHi = findViewById(R.id.tvHi);
        tvName = findViewById(R.id.tvName);
        tvSubtitle = findViewById(R.id.tvSubtitle);

        // Profile & Nav
        profileCard = findViewById(R.id.profileCard);
        imgProfileSmall = findViewById(R.id.imgProfileSmall);
        navHome = findViewById(R.id.navHome);
        navSchedule = findViewById(R.id.navSchedule);
        navMap = findViewById(R.id.navMap);
        navNotif = findViewById(R.id.navNotif);
        viewPager = findViewById(R.id.viewPager);
    }

    private void setupViewPager() {
        ViewPagerAdapter adapter = new ViewPagerAdapter(this);
        viewPager.setAdapter(adapter);
        viewPager.setOffscreenPageLimit(3); // Keep tabs in memory

        // Sync Swipe with Icons & Header Text
        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                updateNavUI(position);
            }
        });
    }

    private void setupClickListeners() {
        // Navigation Clicks (Smooth Scroll)
        navHome.setOnClickListener(v -> viewPager.setCurrentItem(0, true));
        navSchedule.setOnClickListener(v -> viewPager.setCurrentItem(1, true));
        navMap.setOnClickListener(v -> viewPager.setCurrentItem(2, true));
        navNotif.setOnClickListener(v -> viewPager.setCurrentItem(3, true));

        // Profile Card Click
        profileCard.setOnClickListener(v -> {
            Intent intent = new Intent(StudentHomeActivity.this, ProfileActivity.class);
            startActivity(intent);
        });
    }

    // --- DATA LOADING ---

    private void loadUserData() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            // 1. Get Name
            String fullName = user.getDisplayName();
            if (fullName != null && !fullName.isEmpty()) {
                // Split "John Doe" -> "John!"
                currentUserName = fullName.split(" ")[0] + "!";
            } else {
                currentUserName = "Student!";
            }

            // 2. Load Image
            if (user.getPhotoUrl() != null) {
                Glide.with(this)
                        .load(user.getPhotoUrl())
                        .placeholder(R.mipmap.ic_launcher)
                        .error(R.mipmap.ic_launcher)
                        .centerCrop()
                        .diskCacheStrategy(DiskCacheStrategy.ALL)
                        .into(imgProfileSmall);
            }
        } else {
            currentUserName = "Guest!";
            imgProfileSmall.setImageResource(R.mipmap.ic_launcher);
        }

        // Force update the text based on current tab immediately after loading name
        updateHeader(viewPager.getCurrentItem());
    }

    // --- UI ANIMATIONS & TEXT LOGIC ---

    private void updateNavUI(int position) {
        // 1. Reset all icons
        animateIcon(navHome, false);
        animateIcon(navSchedule, false);
        animateIcon(navMap, false);
        animateIcon(navNotif, false);

        // 2. Highlight selected & Update Text
        switch (position) {
            case 0: animateIcon(navHome, true); break;
            case 1: animateIcon(navSchedule, true); break;
            case 2: animateIcon(navMap, true); break;
            case 3: animateIcon(navNotif, true); break;
        }

        // 3. Update the funny texts
        updateHeader(position);
    }

    /**
     * This is where the magic happens!
     * Customizes the text based on which fragment is showing.
     */
    private void updateHeader(int position) {
        tvHi.setVisibility(View.VISIBLE); // Ensure it's visible by default

        switch (position) {
            case 0: // HOME
                tvHi.setText("Hi ");
                tvName.setText(currentUserName); // Shows "Hi John!"
                tvSubtitle.setText("Ready to get that big brain energy? 🧠");
                break;

            case 1: // SCHEDULE
                tvHi.setText("The ");
                tvName.setText("Plan");
                tvSubtitle.setText("Classes, chaos, and caffeine. ☕");
                break;

            case 2: // MAP
                tvHi.setText("Zone ");
                tvName.setText("Scout");
                tvSubtitle.setText("Dora ain't got nothing on you. 🗺️");
                break;

            case 3: // NOTIFICATIONS
                tvHi.setText("News ");
                tvName.setText("Flash");
                tvSubtitle.setText("Tea spilled? Check the updates. 🐸☕");
                break;
        }
    }

    private void animateIcon(ImageView icon, boolean isActive) {
        if (isActive) {
            icon.setColorFilter(COLOR_ACTIVE);
            icon.animate().scaleX(1.3f).scaleY(1.3f).setDuration(200).start();
        } else {
            icon.setColorFilter(COLOR_INACTIVE);
            icon.animate().scaleX(1.0f).scaleY(1.0f).setDuration(200).start();
        }
    }

    // --- FRAGMENT ADAPTER ---

    private static class ViewPagerAdapter extends FragmentStateAdapter {

        public ViewPagerAdapter(@NonNull FragmentActivity fragmentActivity) {
            super(fragmentActivity);
        }

        @NonNull
        @Override
        public Fragment createFragment(int position) {
            switch (position) {
                case 0: return new HomeFragment();
                case 1: return new ScheduleFragment();
                case 2: return new MapsFragment();
                case 3: return new NotificationsFragment();
                default: return new HomeFragment();
            }
        }

        @Override
        public int getItemCount() {
            return 4;
        }
    }
}