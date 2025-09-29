package com.example.racingsim.ui;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.Nullable;

import com.example.racingsim.R;
import com.example.racingsim.gl.Map3DView;
import com.example.racingsim.model.MapPoints;

/**
 * Hosts the OpenGL preview for the generated map in 3D.
 */
public class Map3DActivity extends Activity {

    public static final String EXTRA_MAP_POINTS_JSON = "MAP_POINTS_JSON";

    private Map3DView map3DView;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map_3d);

        String json = getIntent().getStringExtra(EXTRA_MAP_POINTS_JSON);
        MapPoints points;
        try {
            points = MapPoints.fromJson(json);
        } catch (IllegalArgumentException exception) {
            points = MapPoints.createDemoCourse();
        }

        ViewGroup container = findViewById(R.id.map3d_container);
        View pedalContainer = findViewById(R.id.pedal_container);
        if (pedalContainer != null) {
            pedalContainer.setVisibility(View.GONE);
        }
        map3DView = new Map3DView(this, points);
        ViewGroup.LayoutParams params = new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT);
        container.addView(map3DView, params);
    }

    @Override
    protected void onResume() {
        super.onResume();
        map3DView.onResume();
    }

    @Override
    protected void onPause() {
        map3DView.onPause();
        super.onPause();
    }
}
