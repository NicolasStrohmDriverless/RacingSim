package com.example.racingsim;

import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.racingsim.model.DefaultMapPointsProvider;
import com.example.racingsim.model.MapPoints;
import com.example.racingsim.model.MapPointsProvider;
import com.example.racingsim.track.TrackData;
import com.example.racingsim.track.TrackGenerator;
import com.example.racingsim.track.TrackRenderer;
import com.example.racingsim.ui.Map3DActivity;

public class MainActivity extends AppCompatActivity {

    private ImageView trackImageView;
    private final TrackGenerator trackGenerator = new TrackGenerator();
    private final MapPointsProvider mapPointsProvider = new DefaultMapPointsProvider();
    private TrackData lastGeneratedTrack;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        trackImageView = findViewById(R.id.trackImageView);
        Button generateButton = findViewById(R.id.generateButton);
        Button preview3DButton = findViewById(R.id.btn_open_3d);

        generateButton.setOnClickListener(v -> generateAndShowTrack());
        preview3DButton.setOnClickListener(v -> open3DPreview());
        trackImageView.post(this::generateAndShowTrack);
    }

    private void generateAndShowTrack() {
        int width = trackImageView.getWidth();
        int height = trackImageView.getHeight();

        if (width <= 0 || height <= 0) {
            width = getResources().getDisplayMetrics().widthPixels;
            height = (int) (width * 0.75f);
        }

        TrackData trackData = trackGenerator.generate(width, height);
        lastGeneratedTrack = trackData;
        Bitmap bitmap = TrackRenderer.renderTrack(width, height, trackData);
        trackImageView.setImageBitmap(bitmap);
    }

    private void open3DPreview() {
        Intent intent = new Intent(this, Map3DActivity.class);
        intent.putExtra(Map3DActivity.EXTRA_MAP_POINTS_JSON, collectMapPointsJson());
        startActivity(intent);
    }

    private String collectMapPointsJson() {
        MapPoints points = mapPointsProvider.provideMapPoints(lastGeneratedTrack);
        if (points == null || (points.getBlue().isEmpty() && points.getYellow().isEmpty())) {
            // TODO: Replace fallback with real map data wiring when available.
            points = MapPoints.createDemoCourse();
        }
        return points.toJsonString();
    }
}
