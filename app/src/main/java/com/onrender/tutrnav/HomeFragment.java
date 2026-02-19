package com.onrender.tutrnav;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewParent;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.CompositePageTransformer;
import androidx.viewpager2.widget.MarginPageTransformer;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.tabs.TabLayout;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

public class HomeFragment extends Fragment {

    private ViewPager2 vpDiscover;
    private TabLayout tabLayout;
    private Handler sliderHandler = new Handler(Looper.getMainLooper());
    private SharedTuitionViewModel viewModel;

    // AESTHETIC CONFIGURATION
    private static final int AUTO_SLIDE_DURATION = 3500;
    private static final float SCALE_CENTER = 1.0f;
    private static final float SCALE_SIDE = 0.90f;
    private static final float ALPHA_SIDE = 0.7f;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        viewModel = new ViewModelProvider(requireActivity()).get(SharedTuitionViewModel.class);
        vpDiscover = view.findViewById(R.id.vpDiscover);
        tabLayout = view.findViewById(R.id.tabLayout);

        fetchTuitionsFromFirestore();

        return view;
    }

    private void fetchTuitionsFromFirestore() {
        FirebaseFirestore.getInstance().collection("tuitions")
                .limit(10)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<TuitionModel> list = new ArrayList<>();
                    for (DocumentSnapshot doc : queryDocumentSnapshots) {
                        TuitionModel model = doc.toObject(TuitionModel.class);
                        if (model != null) list.add(model);
                    }

                    if (!list.isEmpty()) {
                        setupAdapter(list);
                    }
                });
    }

    private void setupAdapter(List<TuitionModel> list) {
        DiscoverAdapter adapter = new DiscoverAdapter(list, model -> {
            viewModel.select(model);
            if (getActivity() instanceof StudentHomeActivity) {
                ViewPager2 parentVP = getActivity().findViewById(R.id.viewPager);
                parentVP.setCurrentItem(2, true);
            }
        });

        vpDiscover.setAdapter(adapter);

        // Infinite Scroll Math
        int midPoint = Integer.MAX_VALUE / 2;
        int startPosition = midPoint - (midPoint % list.size());
        vpDiscover.setCurrentItem(startPosition, false);

        setupViewPagerAesthetics();
        setupInfiniteDots(list.size());

        sliderHandler.postDelayed(sliderRunnable, AUTO_SLIDE_DURATION);
    }

    @SuppressLint("ClickableViewAccessibility")
    private void setupViewPagerAesthetics() {
        vpDiscover.setClipToPadding(false);
        vpDiscover.setClipChildren(false);
        vpDiscover.setOffscreenPageLimit(3);

        CompositePageTransformer transformer = new CompositePageTransformer();
        transformer.addTransformer(new MarginPageTransformer(20));
        transformer.addTransformer((page, position) -> {
            float r = 1 - Math.abs(position);
            page.setScaleY(SCALE_SIDE + r * (SCALE_CENTER - SCALE_SIDE));
            page.setAlpha(ALPHA_SIDE + r * (1 - ALPHA_SIDE));
        });
        vpDiscover.setPageTransformer(transformer);

        // --- THE FIX ---
        View child = vpDiscover.getChildAt(0);
        if (child instanceof RecyclerView) {
            child.setOverScrollMode(RecyclerView.OVER_SCROLL_NEVER);

            child.setOnTouchListener((v, event) -> {
                int action = event.getAction();

                if (action == MotionEvent.ACTION_DOWN) {
                    sliderHandler.removeCallbacks(sliderRunnable);
                    // Force the ScrollView (and all parents) to stop intercepting
                    notifyParentsToDisallowIntercept(v, true);

                } else if (action == MotionEvent.ACTION_MOVE) {
                    // Keep forcing it during the move
                    notifyParentsToDisallowIntercept(v, true);

                } else if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
                    sliderHandler.removeCallbacks(sliderRunnable);
                    sliderHandler.postDelayed(sliderRunnable, AUTO_SLIDE_DURATION);
                    // Release the lock
                    notifyParentsToDisallowIntercept(v, false);
                }

                return false; // Allow ViewPager to handle the actual scroll
            });
        }
    }

    // Helper method to walk up the tree and tell EVERYONE to hands off
    private void notifyParentsToDisallowIntercept(View view, boolean disallow) {
        ViewParent parent = view.getParent();
        while (parent != null) {
            parent.requestDisallowInterceptTouchEvent(disallow);
            parent = parent.getParent();
        }
    }

    private void setupInfiniteDots(int realCount) {
        tabLayout.removeAllTabs();
        for (int i = 0; i < realCount; i++) {
            tabLayout.addTab(tabLayout.newTab());
        }

        vpDiscover.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                int realPosition = position % realCount;
                TabLayout.Tab tab = tabLayout.getTabAt(realPosition);
                if (tab != null && !tab.isSelected()) {
                    tab.select();
                }
            }
        });

        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                if (vpDiscover.getScrollState() == ViewPager2.SCROLL_STATE_IDLE) {
                    int currentVPPos = vpDiscover.getCurrentItem();
                    int currentRealPos = currentVPPos % realCount;
                    int targetRealPos = tab.getPosition();
                    int diff = targetRealPos - currentRealPos;
                    if (diff != 0) {
                        vpDiscover.setCurrentItem(currentVPPos + diff, true);
                    }
                }
            }
            @Override public void onTabUnselected(TabLayout.Tab tab) {}
            @Override public void onTabReselected(TabLayout.Tab tab) {}
        });
    }

    private Runnable sliderRunnable = new Runnable() {
        @Override
        public void run() {
            if (vpDiscover != null) {
                vpDiscover.setCurrentItem(vpDiscover.getCurrentItem() + 1, true);
                sliderHandler.postDelayed(this, AUTO_SLIDE_DURATION);
            }
        }
    };

    @Override public void onPause() { super.onPause(); sliderHandler.removeCallbacks(sliderRunnable); }
    @Override public void onResume() { super.onResume(); sliderHandler.postDelayed(sliderRunnable, AUTO_SLIDE_DURATION); }
}