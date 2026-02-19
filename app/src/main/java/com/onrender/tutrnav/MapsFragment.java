package com.onrender.tutrnav;

import android.Manifest;
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
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout; // CRITICAL IMPORT
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.XYTileSource;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.FolderOverlay;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider;
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MapsFragment extends Fragment {

    // --- Core Components ---
    private MapView map;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private BottomSheetBehavior<FrameLayout> bottomSheetBehavior;
    private SharedTuitionViewModel viewModel;

    // --- UI Views ---
    private TextView tvSheetPrice, tvSheetTitle, tvSheetSubject, tvSheetDesc;
    private ImageView btnSheetToggle;
    private CardView btnMyLocation, btnSheetCall, btnSheetToggleCard, btnSearchMap;
    private MaterialButton btnEnroll, btnReport;

    // --- Data & Map Overlays ---
    private List<TuitionModel> allTuitions = new ArrayList<>();
    private GeoPoint userLocation;
    private FolderOverlay tuitionMarkersOverlay;
    private MyLocationNewOverlay locationOverlay;

    private static final int LOCATION_REQUEST_CODE = 1001;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Load OSMDroid Config
        Context ctx = requireContext();
        Configuration.getInstance().load(ctx, PreferenceManager.getDefaultSharedPreferences(ctx));
        Configuration.getInstance().setUserAgentValue(ctx.getPackageName());
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_maps, container, false);

        // Init Firebase
        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        initViews(view);
        setupMinimalistMap(); // CHANGED: New Clean Map Setup
        setupBottomSheet();
        checkLocationPermission();
        fetchTuitions();

        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        try {
            viewModel = new ViewModelProvider(requireActivity()).get(SharedTuitionViewModel.class);
            viewModel.getSelected().observe(getViewLifecycleOwner(), tuition -> {
                if (tuition != null && map != null) {
                    GeoPoint target = new GeoPoint(tuition.getLatitude(), tuition.getLongitude());
                    map.getController().animateTo(target);
                    map.getController().setZoom(16.0);
                    populateBottomSheet(tuition);
                    bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
                }
            });
        } catch (Exception e) {
            // ViewModel safety check
        }
    }

    private void initViews(View v) {
        map = v.findViewById(R.id.map);

        // Bottom Sheet Setup
        FrameLayout bottomSheet = v.findViewById(R.id.bottomSheet);
        bottomSheetBehavior = BottomSheetBehavior.from(bottomSheet);

        // Text Views
        tvSheetPrice = v.findViewById(R.id.tvSheetPrice);
        tvSheetTitle = v.findViewById(R.id.tvSheetTitle);
        tvSheetSubject = v.findViewById(R.id.tvSheetSubject);
        tvSheetDesc = v.findViewById(R.id.tvSheetDesc);

        // Buttons
        btnSheetToggle = v.findViewById(R.id.btnSheetToggle);
        btnSheetToggleCard = v.findViewById(R.id.btnSheetToggleCard);
        btnMyLocation = v.findViewById(R.id.btnMyLocation);
        btnSheetCall = v.findViewById(R.id.btnSheetCall);
        btnSearchMap = v.findViewById(R.id.btnSearchMap);

        btnEnroll = v.findViewById(R.id.btnEnroll);
        btnReport = v.findViewById(R.id.btnReport);

        btnSearchMap.setOnClickListener(view ->
                Toast.makeText(getContext(), "Search Filters coming soon!", Toast.LENGTH_SHORT).show());
    }

    /**
     * THE LEGENDARY CLEAN MAP SETUP
     * Uses CartoDB Light tiles (No clutter) + Invert Matrix = Sleek Dark Road Map
     */
    /**
     * THE PURPLE ACCENT MAP SETUP
     */
    /**
     * HIGH CONTRAST NEON SETUP
     * Land = Pitch Black
     * Roads = Glowing Electric Purple
     */
    private void setupMinimalistMap() {
        XYTileSource darkCleanSource = new XYTileSource(
                "CartoDB_Dark_No_Labels",
                1, 20, 256, ".png",
                new String[] {
                        "https://a.basemaps.cartocdn.com/dark_nolabels/",
                        "https://b.basemaps.cartocdn.com/dark_nolabels/",
                        "https://c.basemaps.cartocdn.com/dark_nolabels/"
                },
                "© OpenStreetMap contributors, © CARTO"
        );

        map.setTileSource(darkCleanSource);
        map.setBuiltInZoomControls(false);
        map.setMultiTouchControls(true);

        // =================================================================
        // THE "ULTRA CONTRAST" MATRIX
        // By increasing the scales significantly and keeping offsets near 0,
        // we "clip" the dark values to black and "max out" the bright values.
        // =================================================================
        float[] matrix = {
                // R-Scale, G-Scale, B-Scale, A-Scale, Offset
                2.8f, 0,    0,    0,   -10, // Red: Very high scale, negative offset to kill darks
                0,    0.2f, 0,    0,   -10, // Green: Squashed to near zero for pure purple
                0,    0,    5.0f, 0,   -10, // Blue: Extreme boost to make roads "Burn" blue-purple
                0,    0,    0,    1,    0   // Alpha
        };

        ColorMatrixColorFilter filter = new ColorMatrixColorFilter(matrix);
        map.getOverlayManager().getTilesOverlay().setColorFilter(filter);

        // Map Overlays
        tuitionMarkersOverlay = new FolderOverlay();
        map.getOverlays().add(tuitionMarkersOverlay);

        map.getController().setZoom(15.0); // Roads look better when zoomed in slightly more
    }

    // --- DATA LOADING ---
    private void fetchTuitions() {
        db.collection("tuitions").get().addOnSuccessListener(queryDocumentSnapshots -> {
            allTuitions.clear();
            for (DocumentSnapshot doc : queryDocumentSnapshots) {
                try {
                    TuitionModel t = doc.toObject(TuitionModel.class);
                    if(t != null) allTuitions.add(t);
                } catch (Exception e) {
                    // Ignore malformed
                }
            }
            displayMarkers();
        });
    }

    private void displayMarkers() {
        if (tuitionMarkersOverlay == null) return;
        tuitionMarkersOverlay.getItems().clear();

        if (userLocation != null && !allTuitions.isEmpty()) {
            Collections.sort(allTuitions, (t1, t2) -> {
                float[] res1 = new float[1];
                float[] res2 = new float[1];
                Location.distanceBetween(userLocation.getLatitude(), userLocation.getLongitude(), t1.getLatitude(), t1.getLongitude(), res1);
                Location.distanceBetween(userLocation.getLatitude(), userLocation.getLongitude(), t2.getLatitude(), t2.getLongitude(), res2);
                return Float.compare(res1[0], res2[0]);
            });
        }

        int count = 0;
        for(TuitionModel t : allTuitions) {
            if(count++ > 30) break;
            if(Math.abs(t.getLatitude()) < 0.1) continue;

            Marker marker = new Marker(map);
            marker.setPosition(new GeoPoint(t.getLatitude(), t.getLongitude()));
            marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
            marker.setTitle(t.getTitle());

            // Gold Pin for contrast against dark map
            Drawable icon = ContextCompat.getDrawable(requireContext(), android.R.drawable.ic_menu_myplaces);
            if(icon != null) {
                icon.setTint(Color.parseColor("#FFCA28"));
                marker.setIcon(icon);
            }

            marker.setOnMarkerClickListener((m, mapView) -> {
                map.getController().animateTo(m.getPosition());
                populateBottomSheet(t);
                bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
                return true;
            });

            tuitionMarkersOverlay.add(marker);
        }
        map.invalidate();
    }

    // --- BOTTOM SHEET UI LOGIC ---
    private void populateBottomSheet(TuitionModel t) {
        tvSheetPrice.setText("₹" + t.getFee());
        tvSheetTitle.setText(t.getTitle());

        String sub = (t.getTags() != null && !t.getTags().isEmpty())
                ? t.getTags().get(0)
                : (t.getSubject() != null ? t.getSubject() : "General");
        tvSheetSubject.setText(sub);
        tvSheetDesc.setText(t.getDescription());

        btnSheetCall.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_DIAL);
            intent.setData(Uri.parse("tel:1234567890"));
            startActivity(intent);
        });

        checkEnrollmentStatus(t);

        btnEnroll.setOnClickListener(v -> requestEnrollment(t));

        btnReport.setOnClickListener(v ->
                Toast.makeText(getContext(), "Report submitted.", Toast.LENGTH_SHORT).show());
    }

    private void checkEnrollmentStatus(TuitionModel t) {
        FirebaseUser user = mAuth.getCurrentUser();
        if(user == null) return;

        btnEnroll.setText("Enroll Now");
        btnEnroll.setBackgroundTintList(ContextCompat.getColorStateList(requireContext(), R.color.orange));
        btnEnroll.setEnabled(true);

        db.collection("enrollments")
                .whereEqualTo("studentId", user.getUid())
                .whereEqualTo("tuitionId", t.getTuitionId())
                .get()
                .addOnSuccessListener(snapshots -> {
                    if (!snapshots.isEmpty()) {
                        DocumentSnapshot doc = snapshots.getDocuments().get(0);
                        String status = doc.getString("status");

                        if ("pending".equals(status)) {
                            btnEnroll.setText("Pending");
                            btnEnroll.setBackgroundTintList(ContextCompat.getColorStateList(requireContext(), android.R.color.darker_gray));
                            btnEnroll.setEnabled(false);
                        } else if ("approved".equals(status)) {
                            btnEnroll.setText("Enrolled");
                            btnEnroll.setBackgroundTintList(ContextCompat.getColorStateList(requireContext(), android.R.color.holo_green_dark));
                            btnEnroll.setEnabled(false);
                        }
                    }
                });
    }

    private void requestEnrollment(TuitionModel t) {
        FirebaseUser user = mAuth.getCurrentUser();
        if(user == null) {
            Toast.makeText(getContext(), "Login required", Toast.LENGTH_SHORT).show();
            return;
        }

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
                    Toast.makeText(getContext(), "Request Sent!", Toast.LENGTH_SHORT).show();
                    btnEnroll.setText("Pending");
                    btnEnroll.setEnabled(false);
                })
                .addOnFailureListener(e -> Toast.makeText(getContext(), "Failed", Toast.LENGTH_SHORT).show());
    }

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
            @Override
            public void onStateChanged(@NonNull View bottomSheet, int newState) {
                if (newState == BottomSheetBehavior.STATE_EXPANDED) {
                    btnSheetToggle.setRotation(180);
                } else {
                    btnSheetToggle.setRotation(0);
                }
            }
            @Override public void onSlide(@NonNull View bottomSheet, float slideOffset) {}
        });
    }

    // --- LOCATION ---
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
        if (requestCode == LOCATION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                setupUserLocation();
            }
        }
    }

    private void setupUserLocation() {
        locationOverlay = new MyLocationNewOverlay(new GpsMyLocationProvider(requireContext()), map);
        locationOverlay.enableMyLocation();

        // Custom Cyan User Icon
        Bitmap personIcon = getBitmapFromVectorDrawable(requireContext(), android.R.drawable.ic_menu_mylocation, "#00E5FF");
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
                map.getController().setZoom(16.0);
            } else {
                Toast.makeText(getContext(), "Waiting for location...", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private Bitmap getBitmapFromVectorDrawable(Context context, int drawableId, String colorHex) {
        Drawable drawable = ContextCompat.getDrawable(context, drawableId);
        if (drawable == null) return null;
        drawable.setTint(Color.parseColor(colorHex));
        Bitmap bitmap = Bitmap.createBitmap(drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        drawable.draw(canvas);
        return bitmap;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (map != null) map.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
        if (map != null) map.onPause();
    }
}