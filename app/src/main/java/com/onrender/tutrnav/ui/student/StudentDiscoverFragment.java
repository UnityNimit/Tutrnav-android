package com.onrender.tutrnav.ui.student;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.CompositePageTransformer;
import androidx.viewpager2.widget.MarginPageTransformer;
import androidx.viewpager2.widget.ViewPager2;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.onrender.tutrnav.R;
import com.onrender.tutrnav.adapters.StudentDiscoverAdapter;
import com.onrender.tutrnav.models.TuitionModel;

import java.util.ArrayList;
import java.util.List;

public class StudentDiscoverFragment extends Fragment {

    private ViewPager2 vpDiscover;
    private final Handler sliderHandler = new Handler(Looper.getMainLooper());
    private StudentSharedViewModel viewModel;

    // AESTHETIC CONFIGURATION
    private static final int AUTO_SLIDE_DURATION = 3500;
    private static final float SCALE_CENTER = 1.0f;
    private static final float SCALE_SIDE = 0.90f;
    private static final float ALPHA_SIDE = 0.7f;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_student_discover, container, false);

        viewModel = new ViewModelProvider(requireActivity()).get(StudentSharedViewModel.class);
        vpDiscover = view.findViewById(R.id.vpDiscover);

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
        StudentDiscoverAdapter adapter = new StudentDiscoverAdapter(list, model -> {
            viewModel.select(model);
            if (getActivity() instanceof StudentHomeActivity) {
                ViewPager2 parentVP = getActivity().findViewById(R.id.viewPager);
                if (parentVP != null) parentVP.setCurrentItem(2, true);
            }
        });

        vpDiscover.setAdapter(adapter);

        // Infinite Scroll Math: Start perfectly in the middle
        int midPoint = Integer.MAX_VALUE / 2;
        int startPosition = midPoint - (midPoint % list.size());
        vpDiscover.setCurrentItem(startPosition, false);

        setupViewPagerAesthetics();
        setupSmartTouchHandling();

        // Start Auto Slider
        sliderHandler.postDelayed(sliderRunnable, AUTO_SLIDE_DURATION);
    }

    @SuppressLint("ClickableViewAccessibility")
    private void setupViewPagerAesthetics() {
        vpDiscover.setClipToPadding(false);
        vpDiscover.setClipChildren(false);
        vpDiscover.setOffscreenPageLimit(3);

        // Visual Transformations (Scaling and Fading side cards)
        CompositePageTransformer transformer = new CompositePageTransformer();
        transformer.addTransformer(new MarginPageTransformer(20));
        transformer.addTransformer((page, position) -> {
            float r = 1 - Math.abs(position);
            page.setScaleY(SCALE_SIDE + r * (SCALE_CENTER - SCALE_SIDE));
            page.setAlpha(ALPHA_SIDE + r * (1 - ALPHA_SIDE));
        });
        vpDiscover.setPageTransformer(transformer);
    }

    // --- THE LEGENDARY FIX FOR SCROLLVIEW + VIEWPAGER2 CONFLICT ---
    private void setupSmartTouchHandling() {
        View child = vpDiscover.getChildAt(0);
        if (child instanceof RecyclerView) {
            RecyclerView rv = (RecyclerView) child;
            rv.setOverScrollMode(RecyclerView.OVER_SCROLL_NEVER);

            rv.addOnItemTouchListener(new RecyclerView.OnItemTouchListener() {
                private float startX;
                private float startY;

                @Override
                public boolean onInterceptTouchEvent(@NonNull RecyclerView rv, @NonNull MotionEvent e) {
                    switch (e.getAction()) {
                        case MotionEvent.ACTION_DOWN:
                            startX = e.getX();
                            startY = e.getY();
                            // 1. Instantly lock the parent ScrollView so it doesn't steal the touch
                            vpDiscover.getParent().requestDisallowInterceptTouchEvent(true);
                            // 2. Pause the auto-slider so it doesn't fight your finger
                            sliderHandler.removeCallbacks(sliderRunnable);
                            break;

                        case MotionEvent.ACTION_MOVE:
                            float dx = Math.abs(e.getX() - startX);
                            float dy = Math.abs(e.getY() - startY);

                            // If the user is swiping UP/DOWN, unlock the parent ScrollView so the page scrolls
                            if (dy > dx) {
                                vpDiscover.getParent().requestDisallowInterceptTouchEvent(false);
                            } else {
                                // If swiping LEFT/RIGHT, keep the parent locked so the ViewPager swipes perfectly
                                vpDiscover.getParent().requestDisallowInterceptTouchEvent(true);
                            }
                            break;

                        case MotionEvent.ACTION_UP:
                        case MotionEvent.ACTION_CANCEL:
                            // Unlock parent and resume auto-slider when user lets go
                            vpDiscover.getParent().requestDisallowInterceptTouchEvent(false);
                            sliderHandler.removeCallbacks(sliderRunnable);
                            sliderHandler.postDelayed(sliderRunnable, AUTO_SLIDE_DURATION);
                            break;
                    }

                    // CRITICAL: Must return false so the inner RecyclerView still processes the drag!
                    return false;
                }

                @Override
                public void onTouchEvent(@NonNull RecyclerView rv, @NonNull MotionEvent e) {}
                @Override
                public void onRequestDisallowInterceptTouchEvent(boolean disallowIntercept) {}
            });
        }
    }

    private final Runnable sliderRunnable = new Runnable() {
        @Override
        public void run() {
            if (vpDiscover != null && vpDiscover.getAdapter() != null) {
                // Smoothly slide to the next item
                vpDiscover.setCurrentItem(vpDiscover.getCurrentItem() + 1, true);
                sliderHandler.postDelayed(this, AUTO_SLIDE_DURATION);
            }
        }
    };

    @Override
    public void onPause() {
        super.onPause();
        sliderHandler.removeCallbacks(sliderRunnable);
    }

    @Override
    public void onResume() {
        super.onResume();
        sliderHandler.removeCallbacks(sliderRunnable);
        sliderHandler.postDelayed(sliderRunnable, AUTO_SLIDE_DURATION);
    }
}