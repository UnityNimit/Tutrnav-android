package com.onrender.tutrnav.ui.student;

import android.Manifest;
import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.CenterCrop;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.onrender.tutrnav.R;
import com.onrender.tutrnav.models.TuitionModel;

import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.XYTileSource;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.FolderOverlay;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.infowindow.InfoWindow;
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider;
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class StudentMapsFragment extends Fragment {

    // --- Core Components ---
    private MapView map;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private BottomSheetBehavior<FrameLayout> bottomSheetBehavior;
    private StudentSharedViewModel viewModel;

    // --- UI Views ---
    private TextView tvSheetPrice, tvSheetTitle, tvSheetTags, tvSheetDesc;
    private TextView tvTeacherName, tvTeacherExp;
    private ImageView btnSheetToggle, imgSheetCallIcon, imgTeacher;
    private CardView btnMyLocation, btnSheetCall, btnSheetToggleCard, btnSearchMap;
    private MaterialButton btnEnroll, btnReport;

    // --- Data & Map Overlays ---
    private List<TuitionModel> allTuitions = new ArrayList<>();
    private GeoPoint userLocation;
    private FolderOverlay tuitionMarkersOverlay;
    private MyLocationNewOverlay locationOverlay;

    private static final int LOCATION_REQUEST_CODE = 1001;

    // Holds the FETCHED teacher's phone number (not the student's!)
    private String currentTeacherPhone = "";

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Context ctx = requireContext();
        Configuration.getInstance().load(ctx, PreferenceManager.getDefaultSharedPreferences(ctx));
        Configuration.getInstance().setUserAgentValue(ctx.getPackageName());
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_student_maps, container, false);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        initViews(view);
        setupMapStyle();
        setupBottomSheet();
        checkLocationPermission();
        fetchTuitions();

        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        try {
            viewModel = new ViewModelProvider(requireActivity()).get(StudentSharedViewModel.class);
            viewModel.getSelected().observe(getViewLifecycleOwner(), tuition -> {
                if (tuition != null && map != null) {
                    GeoPoint target = new GeoPoint(tuition.getLatitude(), tuition.getLongitude());
                    map.getController().animateTo(target);
                    map.getController().setZoom(17.0);
                    populateBottomSheet(tuition);
                    bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
                }
            });
        } catch (Exception ignored) {}
    }

    private void initViews(View v) {
        map = v.findViewById(R.id.map);
        FrameLayout bottomSheet = v.findViewById(R.id.bottomSheet);
        bottomSheetBehavior = BottomSheetBehavior.from(bottomSheet);

        // Texts
        tvSheetPrice = v.findViewById(R.id.tvSheetPrice);
        tvSheetTitle = v.findViewById(R.id.tvSheetTitle);
        tvSheetTags = v.findViewById(R.id.tvSheetTags);
        tvSheetDesc = v.findViewById(R.id.tvSheetDesc);
        tvTeacherName = v.findViewById(R.id.tvTeacherName);
        tvTeacherExp = v.findViewById(R.id.tvTeacherExp);

        // Images & Buttons
        imgTeacher = v.findViewById(R.id.imgTeacher);
        btnSheetToggle = v.findViewById(R.id.btnSheetToggle);
        btnSheetToggleCard = v.findViewById(R.id.btnSheetToggleCard);
        btnMyLocation = v.findViewById(R.id.btnMyLocation);
        btnSheetCall = v.findViewById(R.id.btnSheetCall);
        imgSheetCallIcon = v.findViewById(R.id.imgSheetCallIcon);
        btnSearchMap = v.findViewById(R.id.btnSearchMap);

        btnEnroll = v.findViewById(R.id.btnEnroll);
        btnReport = v.findViewById(R.id.btnReport);

        btnSearchMap.setOnClickListener(view -> Toast.makeText(getContext(), "Search Filters coming soon!", Toast.LENGTH_SHORT).show());

        btnReport.setOnClickListener(view -> Toast.makeText(getContext(), "User reported to admin.", Toast.LENGTH_SHORT).show());
    }

    private void startRingingAnimation() {
        if (imgSheetCallIcon == null) return;
        imgSheetCallIcon.clearAnimation();
        PropertyValuesHolder pvhRotate = PropertyValuesHolder.ofFloat("rotation", 0f, 15f, -15f, 15f, -15f, 0f);
        ObjectAnimator animator = ObjectAnimator.ofPropertyValuesHolder(imgSheetCallIcon, pvhRotate);
        animator.setDuration(1000);
        animator.setRepeatCount(ObjectAnimator.INFINITE);
        animator.setRepeatMode(ObjectAnimator.RESTART);
        animator.setStartDelay(500);
        animator.start();
    }

    @SuppressLint("ClickableViewAccessibility")
    private void setupMapStyle() {
        XYTileSource cleanWhiteSource = new XYTileSource(
                "CartoDB_Light_No_Labels", 1, 20, 256, ".png",
                new String[] {"https://a.basemaps.cartocdn.com/light_nolabels/"},
                "© OpenStreetMap contributors"
        );

        map.setTileSource(cleanWhiteSource);
        map.setBuiltInZoomControls(false);
        map.setMultiTouchControls(true);

        float[] matrix = {
                1f, 0f, 0f, 0f, 0,
                0f, 1f, 0f, 0f, 0,
                0f, 0f, 1.8f, 0f, 0,
                0f, 0f, 0f, 1f, 0
        };
        map.getOverlayManager().getTilesOverlay().setColorFilter(new ColorMatrixColorFilter(matrix));

        // Close InfoWindows if tapping map
        map.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                InfoWindow.closeAllInfoWindowsOn(map);
                v.getParent().requestDisallowInterceptTouchEvent(true);
            } else if (event.getAction() == MotionEvent.ACTION_UP) {
                v.getParent().requestDisallowInterceptTouchEvent(false);
            }
            return false;
        });

        tuitionMarkersOverlay = new FolderOverlay();
        map.getOverlays().add(tuitionMarkersOverlay);
        map.getController().setZoom(15.0);
    }

    private void fetchTuitions() {
        db.collection("tuitions").get().addOnSuccessListener(query -> {
            allTuitions.clear();
            for (DocumentSnapshot doc : query) {
                try {
                    TuitionModel t = doc.toObject(TuitionModel.class);
                    if(t != null) allTuitions.add(t);
                } catch (Exception ignored) {}
            }
            displayMarkers();
        });
    }

    private void displayMarkers() {
        if (tuitionMarkersOverlay == null) return;
        tuitionMarkersOverlay.getItems().clear();

        if (userLocation != null && !allTuitions.isEmpty()) {
            Collections.sort(allTuitions, (t1, t2) -> {
                float[] r1 = new float[1]; float[] r2 = new float[1];
                Location.distanceBetween(userLocation.getLatitude(), userLocation.getLongitude(), t1.getLatitude(), t1.getLongitude(), r1);
                Location.distanceBetween(userLocation.getLatitude(), userLocation.getLongitude(), t2.getLatitude(), t2.getLongitude(), r2);
                return Float.compare(r1[0], r2[0]);
            });
        }

        Bitmap iconBitmap = getBitmapFromVectorDrawable(requireContext(), R.drawable.ic_pin, "#FFCA28", 48, 48);

        for (TuitionModel t : allTuitions) {
            if(Math.abs(t.getLatitude()) < 0.1) continue;

            Marker marker = new Marker(map);
            marker.setPosition(new GeoPoint(t.getLatitude(), t.getLongitude()));
            marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);

            if(iconBitmap != null) {
                marker.setIcon(new android.graphics.drawable.BitmapDrawable(getResources(), iconBitmap));
            }

            // Set Custom InfoWindow (The Bubble)
            marker.setInfoWindow(new CustomTuitionInfoWindow(map, t));

            marker.setOnMarkerClickListener((m, mapView) -> {
                InfoWindow.closeAllInfoWindowsOn(map);
                m.showInfoWindow();
                map.getController().animateTo(m.getPosition());
                populateBottomSheet(t);
                return true;
            });

            tuitionMarkersOverlay.add(marker);
        }
        map.invalidate();
    }

    // --- CUSTOM POPUP WINDOW CLASS ---
    private class CustomTuitionInfoWindow extends InfoWindow {
        private final TuitionModel t;

        public CustomTuitionInfoWindow(MapView mapView, TuitionModel tuition) {
            super(R.layout.item_student_maps_popup, mapView);
            this.t = tuition;
        }

        @Override
        public void onOpen(Object item) {
            ImageView img = mView.findViewById(R.id.imgPopBanner);
            TextView title = mView.findViewById(R.id.tvPopTitle);

            title.setText(t.getTitle());
            Glide.with(mView.getContext())
                    .load(t.getBannerUrl())
                    .placeholder(R.drawable.bg_gradient_overlay)
                    .transform(new CenterCrop(), new RoundedCorners(16))
                    .into(img);

            mView.setOnClickListener(v -> {
                populateBottomSheet(t);
                bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
            });
        }
        @Override public void onClose() {}
    }

    // =========================================================
    //        POPULATE BOTTOM SHEET (THE CRITICAL FIX)
    // =========================================================
    private void populateBottomSheet(TuitionModel t) {
        // 1. Set Class Details first (Static Info)
        tvSheetPrice.setText("₹" + (t.getFee() != null ? t.getFee() : "0"));
        tvSheetTitle.setText(t.getTitle());
        tvSheetDesc.setText(t.getDescription());

        // Process Tags
        if (t.getTags() != null && !t.getTags().isEmpty()) {
            tvSheetTags.setText(TextUtils.join(" • ", t.getTags()));
        } else {
            tvSheetTags.setText(t.getSubject() != null ? t.getSubject() : "General Class");
        }

        startRingingAnimation();
        checkEnrollmentStatus(t);

        // 2. FETCH TEACHER DATA FRESH FROM DATABASE
        // This ensures we get the *actual* teacher's data, not the student's data.

        // Reset to Loading State / Defaults first
        tvTeacherName.setText("Loading...");
        tvTeacherExp.setText("Checking details...");
        imgTeacher.setImageResource(R.mipmap.ic_launcher);
        currentTeacherPhone = ""; // Clear old phone

        db.collection("users").document(t.getTeacherId()).get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        String teacherName = doc.getString("name");
                        String teacherExp = doc.getString("experience");
                        String phone = doc.getString("phone");
                        String photoUrl = doc.getString("photoUrl");

                        // FALLBACK LOGIC
                        tvTeacherName.setText((teacherName != null && !teacherName.isEmpty()) ? teacherName : "Teacher");
                        tvTeacherExp.setText((teacherExp != null && !teacherExp.isEmpty()) ? teacherExp : "Good Teacher");
                        currentTeacherPhone = (phone != null) ? phone : "";

                        // PFP Logic
                        if (photoUrl != null && !photoUrl.isEmpty()) {
                            Glide.with(this).load(photoUrl).circleCrop().into(imgTeacher);
                        } else {
                            imgTeacher.setImageResource(R.mipmap.ic_launcher); // Default PFP
                        }
                    } else {
                        // Document doesn't exist (Rare case)
                        tvTeacherName.setText("Teacher");
                        tvTeacherExp.setText("Good Teacher");
                    }
                })
                .addOnFailureListener(e -> {
                    tvTeacherName.setText("Teacher");
                    tvTeacherExp.setText("Good Teacher");
                });

        // 3. CALL BUTTON LOGIC
        btnSheetCall.setOnClickListener(v -> {
            if (currentTeacherPhone != null && !currentTeacherPhone.isEmpty()) {
                Intent intent = new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + currentTeacherPhone));
                startActivity(intent);
            } else {
                Toast.makeText(getContext(), "Phone number hidden by teacher.", Toast.LENGTH_SHORT).show();
            }
        });

        // 4. Enroll Action
        btnEnroll.setOnClickListener(v -> requestEnrollment(t));
    }

    // --- ENROLLMENT STATUS ---
    private void checkEnrollmentStatus(TuitionModel t) {
        FirebaseUser user = mAuth.getCurrentUser();
        if(user == null) return;

        // Reset to default
        btnEnroll.setText("Enroll Now");
        btnEnroll.setBackgroundTintList(ContextCompat.getColorStateList(requireContext(), R.color.orange));
        btnEnroll.setEnabled(true);

        db.collection("enrollments")
                .whereEqualTo("studentId", user.getUid())
                .whereEqualTo("tuitionId", t.getTuitionId())
                .addSnapshotListener((snapshots, e) -> {
                    if (e != null || snapshots == null || snapshots.isEmpty()) {
                        btnEnroll.setText("Enroll Now");
                        btnEnroll.setBackgroundTintList(ContextCompat.getColorStateList(requireContext(), R.color.orange));
                        btnEnroll.setEnabled(true);
                        return;
                    }

                    DocumentSnapshot doc = snapshots.getDocuments().get(0);
                    String status = doc.getString("status");

                    if ("pending".equals(status)) {
                        btnEnroll.setText("Pending Approval");
                        btnEnroll.setBackgroundTintList(ContextCompat.getColorStateList(requireContext(), android.R.color.darker_gray));
                        btnEnroll.setEnabled(false);
                    } else if ("approved".equals(status)) {
                        btnEnroll.setText("Already Enrolled");
                        btnEnroll.setBackgroundTintList(ContextCompat.getColorStateList(requireContext(), android.R.color.holo_green_dark));
                        btnEnroll.setEnabled(false);
                    }
                });
    }

    private void requestEnrollment(TuitionModel t) {
        FirebaseUser user = mAuth.getCurrentUser();
        if(user == null) {
            Toast.makeText(getContext(), "Please sign in to enroll.", Toast.LENGTH_SHORT).show();
            return;
        }

        btnEnroll.setText("Sending...");
        btnEnroll.setEnabled(false);

        Map<String, Object> enrollment = new HashMap<>();
        enrollment.put("enrollmentId", java.util.UUID.randomUUID().toString());
        enrollment.put("studentId", user.getUid());
        enrollment.put("studentName", user.getDisplayName());
        enrollment.put("studentPhoto", (user.getPhotoUrl() != null) ? user.getPhotoUrl().toString() : "");
        enrollment.put("teacherId", t.getTeacherId());
        enrollment.put("tuitionId", t.getTuitionId());
        enrollment.put("tuitionTitle", t.getTitle());
        enrollment.put("status", "pending");
        enrollment.put("timestamp", System.currentTimeMillis());

        db.collection("enrollments").add(enrollment)
                .addOnSuccessListener(docRef -> {
                    Toast.makeText(getContext(), "Request Sent to Teacher!", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "Network Failed", Toast.LENGTH_SHORT).show();
                    btnEnroll.setText("Enroll Now");
                    btnEnroll.setEnabled(true);
                });
    }

    // --- BOTTOM SHEET LOGIC ---
    private void setupBottomSheet() {
        View.OnClickListener toggleAction = v -> {
            if (bottomSheetBehavior.getState() == BottomSheetBehavior.STATE_EXPANDED) {
                bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
            } else {
                bottomSheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
            }
        };

        btnSheetToggle.setOnClickListener(toggleAction);
        btnSheetToggleCard.setOnClickListener(toggleAction);

        bottomSheetBehavior.addBottomSheetCallback(new BottomSheetBehavior.BottomSheetCallback() {
            @Override public void onStateChanged(@NonNull View bottomSheet, int newState) {}
            @Override public void onSlide(@NonNull View bottomSheet, float slideOffset) {
                btnSheetToggle.setRotation(slideOffset * 180);
            }
        });
    }

    private void checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_REQUEST_CODE);
        } else {
            setupUserLocation();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_REQUEST_CODE && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            setupUserLocation();
        }
    }

    private void setupUserLocation() {
        locationOverlay = new MyLocationNewOverlay(new GpsMyLocationProvider(requireContext()), map);
        locationOverlay.enableMyLocation();

        Bitmap personIcon = getBitmapFromVectorDrawable(requireContext(), R.drawable.ic_locator, "#00E5FF", 40, 40);
        if (personIcon != null) {
            locationOverlay.setPersonIcon(personIcon);
            locationOverlay.setDirectionIcon(personIcon);
        }

        map.getOverlays().add(locationOverlay);

        locationOverlay.runOnFirstFix(() -> {
            if(getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    userLocation = locationOverlay.getMyLocation();
                    if(userLocation != null) {
                        map.getController().animateTo(userLocation);
                        displayMarkers();
                    }
                });
            }
        });

        btnMyLocation.setOnClickListener(v -> {
            if(locationOverlay.getMyLocation() != null) {
                map.getController().animateTo(locationOverlay.getMyLocation());
                map.getController().setZoom(17.0);
            } else {
                Toast.makeText(getContext(), "Waiting for GPS signal...", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private Bitmap getBitmapFromVectorDrawable(Context context, int drawableId, String colorHex, int widthDp, int heightDp) {
        Drawable drawable = ContextCompat.getDrawable(context, drawableId);
        if (drawable == null) return null;

        drawable.setTint(Color.parseColor(colorHex));
        float density = context.getResources().getDisplayMetrics().density;
        Bitmap bitmap = Bitmap.createBitmap((int) (widthDp * density), (int) (heightDp * density), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        drawable.draw(canvas);
        return bitmap;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (map != null) map.onResume();
        if (imgSheetCallIcon != null) startRingingAnimation();
    }

    @Override
    public void onPause() {
        super.onPause();
        if (map != null) map.onPause();
    }
}