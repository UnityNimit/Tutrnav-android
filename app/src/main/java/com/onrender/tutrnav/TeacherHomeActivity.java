package com.onrender.tutrnav;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
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
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class TeacherHomeActivity extends AppCompatActivity {

    private ImageView navDashboard, navMyTuition, navSchedule, imgProfile;
    private ViewPager2 viewPager;
    private TextView tvHi;
    private final int COLOR_ACTIVE = Color.parseColor("#FFCA28");
    private final int COLOR_INACTIVE = Color.parseColor("#5C6BC0");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_teacher_home);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, 0);
            return insets;
        });

        initViews();
        setupViewPager();
        loadProfile();

        findViewById(R.id.profileCard).setOnClickListener(v -> startActivity(new Intent(this, ProfileActivity.class)));
    }

    private void initViews() {
        navDashboard = findViewById(R.id.navDashboard);
        navMyTuition = findViewById(R.id.navMyTuition);
        navSchedule = findViewById(R.id.navSchedule);
        viewPager = findViewById(R.id.viewPager);
        imgProfile = findViewById(R.id.imgProfileSmall);
        tvHi = findViewById(R.id.tvHi);

        navDashboard.setOnClickListener(v -> viewPager.setCurrentItem(0));
        navMyTuition.setOnClickListener(v -> viewPager.setCurrentItem(1));
        navSchedule.setOnClickListener(v -> viewPager.setCurrentItem(2));
    }

    private void setupViewPager() {
        TeacherPagerAdapter adapter = new TeacherPagerAdapter(this);
        viewPager.setAdapter(adapter);
        viewPager.setOffscreenPageLimit(3);
        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                updateNavUI(position);
            }
        });
    }

    private void updateNavUI(int pos) {
        animateIcon(navDashboard, pos == 0);
        animateIcon(navMyTuition, pos == 1);
        animateIcon(navSchedule, pos == 2);
    }

    private void animateIcon(ImageView icon, boolean isActive) {
        icon.setColorFilter(isActive ? COLOR_ACTIVE : COLOR_INACTIVE);
        icon.animate().scaleX(isActive ? 1.2f : 1f).scaleY(isActive ? 1.2f : 1f).setDuration(200).start();
    }

    private void loadProfile() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            String name = user.getDisplayName();
            tvHi.setText("Hi " + (name != null ? name.split(" ")[0] : "Teacher") + "!");
            if (user.getPhotoUrl() != null) Glide.with(this).load(user.getPhotoUrl()).circleCrop().into(imgProfile);
        }
    }

    // ADAPTER
    private static class TeacherPagerAdapter extends FragmentStateAdapter {
        public TeacherPagerAdapter(@NonNull FragmentActivity fa) { super(fa); }
        @NonNull @Override public Fragment createFragment(int position) {
            switch (position) {
                case 0: return new TeacherDashboardFragment();
                case 1: return new TeacherTuitionFragment(); // The main upload screen
                case 2: return new TeacherScheduleFragment();
                default: return new TeacherDashboardFragment();
            }
        }
        @Override public int getItemCount() { return 3; }
    }
}