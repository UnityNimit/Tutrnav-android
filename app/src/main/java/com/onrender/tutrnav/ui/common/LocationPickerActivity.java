package com.onrender.tutrnav.ui.common;

import android.content.Intent;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.widget.Button;
import androidx.appcompat.app.AppCompatActivity;

import com.onrender.tutrnav.R;

import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;

public class LocationPickerActivity extends AppCompatActivity {

    private MapView map;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // OSM Init
        Configuration.getInstance().load(getApplicationContext(), PreferenceManager.getDefaultSharedPreferences(getApplicationContext()));
        Configuration.getInstance().setUserAgentValue(getPackageName());

        setContentView(R.layout.activity_common_location_picker);

        map = findViewById(R.id.map);
        map.setTileSource(TileSourceFactory.MAPNIK);
        map.setMultiTouchControls(true);

        // Start at India (Zoom level 5)
        map.getController().setZoom(5.0);
        map.getController().setCenter(new GeoPoint(20.5937, 78.9629));

        Button btnConfirm = findViewById(R.id.btnConfirmLocation);
        btnConfirm.setOnClickListener(v -> {
            // Get the center of the map view
            GeoPoint center = (GeoPoint) map.getMapCenter();

            Intent result = new Intent();
            result.putExtra("lat", center.getLatitude());
            result.putExtra("lng", center.getLongitude());
            setResult(RESULT_OK, result);
            finish();
        });
    }
}