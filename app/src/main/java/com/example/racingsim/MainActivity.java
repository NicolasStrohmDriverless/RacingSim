package com.example.racingsim;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.PointF;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Process;
import android.util.Log;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

import com.example.racingsim.model.DefaultMapPointsProvider;
import com.example.racingsim.model.MapPoints;
import com.example.racingsim.model.MapPointsProvider;
import com.example.racingsim.preview.PreviewActivity;
import com.example.racingsim.track.TrackData;
import com.example.racingsim.track.TrackGenerator;
import com.example.racingsim.track.TrackRenderer;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.atomic.AtomicInteger;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    private static final String PREFS_NAME = "com.example.racingsim.preferences";
    private static final String KEY_NIGHT_MODE = "night_mode";

    private ImageView trackImageView;
    private final TrackGenerator trackGenerator = new TrackGenerator();
    private final MapPointsProvider mapPointsProvider = new DefaultMapPointsProvider();
    private TrackData lastGeneratedTrack;
    private final ExecutorService renderExecutor = Executors.newSingleThreadExecutor(task -> {
        Thread thread = new Thread(() -> {
            Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);
            task.run();
        }, "TrackRenderWorker");
        thread.setDaemon(true);
        return thread;
    });
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final AtomicInteger generationCounter = new AtomicInteger();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        SharedPreferences preferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        int savedMode = preferences.getInt(KEY_NIGHT_MODE, AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
        AppCompatDelegate.setDefaultNightMode(savedMode);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        trackImageView = findViewById(R.id.trackImageView);
        Button generateButton = findViewById(R.id.generateButton);
        Button preview3DButton = findViewById(R.id.btn_open_3d);
        FloatingActionButton themeToggleButton = findViewById(R.id.themeToggleButton);

        generateButton.setOnClickListener(v -> generateAndShowTrack());
        preview3DButton.setOnClickListener(v -> open3DPreview());
        themeToggleButton.setOnClickListener(v -> toggleTheme());

        updateThemeToggleIcon(themeToggleButton);
        trackImageView.post(this::generateAndShowTrack);
    }

    @Override
    protected void onResume() {
        super.onResume();
        FloatingActionButton themeToggleButton = findViewById(R.id.themeToggleButton);
        if (themeToggleButton != null) {
            updateThemeToggleIcon(themeToggleButton);
        }
    }

    private void toggleTheme() {
        int currentMode = AppCompatDelegate.getDefaultNightMode();
        int newMode;
        if (currentMode == AppCompatDelegate.MODE_NIGHT_YES) {
            newMode = AppCompatDelegate.MODE_NIGHT_NO;
        } else if (currentMode == AppCompatDelegate.MODE_NIGHT_NO) {
            newMode = AppCompatDelegate.MODE_NIGHT_YES;
        } else {
            boolean isCurrentlyNight = (getResources().getConfiguration().uiMode &
                    android.content.res.Configuration.UI_MODE_NIGHT_MASK) ==
                    android.content.res.Configuration.UI_MODE_NIGHT_YES;
            newMode = isCurrentlyNight ? AppCompatDelegate.MODE_NIGHT_NO : AppCompatDelegate.MODE_NIGHT_YES;
        }

        AppCompatDelegate.setDefaultNightMode(newMode);
        getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
                .edit()
                .putInt(KEY_NIGHT_MODE, newMode)
                .apply();

        FloatingActionButton themeToggleButton = findViewById(R.id.themeToggleButton);
        if (themeToggleButton != null) {
            updateThemeToggleIcon(themeToggleButton);
        }
    }

    private void updateThemeToggleIcon(FloatingActionButton button) {
        int currentMode = AppCompatDelegate.getDefaultNightMode();
        boolean isNightMode;
        if (currentMode == AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM || currentMode == AppCompatDelegate.MODE_NIGHT_AUTO_BATTERY) {
            isNightMode = (getResources().getConfiguration().uiMode &
                    android.content.res.Configuration.UI_MODE_NIGHT_MASK) ==
                    android.content.res.Configuration.UI_MODE_NIGHT_YES;
        } else {
            isNightMode = currentMode == AppCompatDelegate.MODE_NIGHT_YES;
        }
        button.setImageResource(isNightMode ? R.drawable.ic_sun : R.drawable.ic_moon);
    }

    private void generateAndShowTrack() {
        int width = trackImageView.getWidth();
        int height = trackImageView.getHeight();

        if (width <= 0 || height <= 0) {
            width = getResources().getDisplayMetrics().widthPixels;
            height = (int) (width * 0.75f);
        }

        final int renderWidth = width;
        final int renderHeight = height;

        final int requestId = generationCounter.incrementAndGet();
        try {
            renderExecutor.submit(() -> {
                TrackData trackData = trackGenerator.generate(renderWidth, renderHeight);
                Bitmap bitmap = TrackRenderer.renderTrack(renderWidth, renderHeight, trackData);
                mainHandler.post(() -> {
                    if (isDestroyed() || requestId != generationCounter.get()) {
                        bitmap.recycle();
                        return;
                    }
                    lastGeneratedTrack = trackData;
                    trackImageView.setImageBitmap(bitmap);
                });
            });
        } catch (RejectedExecutionException exception) {
            Log.w(TAG, "Render executor rejected track generation request", exception);
        }
    }

    private void open3DPreview() {
        if (lastGeneratedTrack == null) {
            Toast.makeText(this, R.string.error_no_track_available, Toast.LENGTH_SHORT).show();
            return;
        }

        float[] points = buildCenterlineArray(lastGeneratedTrack);
        if (points == null || points.length < 4) {
            Toast.makeText(this, R.string.error_no_track_available, Toast.LENGTH_SHORT).show();
            return;
        }

        float width = lastGeneratedTrack.getTrackWidth();
        if (width <= 0.0f) {
            width = 8.0f;
        }

        Intent intent = new Intent(this, PreviewActivity.class);
        intent.putExtra(PreviewActivity.EXTRA_TRACK_POINTS, points);
        intent.putExtra(PreviewActivity.EXTRA_TRACK_WIDTH, width);
        startActivity(intent);
    }

    private float[] buildCenterlineArray(TrackData data) {
        if (data == null) {
            return null;
        }
        java.util.List<PointF> source = data.getCenterlinePoints();
        if (source == null || source.size() < 2) {
            return null;
        }
        float[] buffer = new float[source.size() * 2];
        for (int i = 0; i < source.size(); i++) {
            PointF point = source.get(i);
            buffer[i * 2] = point.x;
            buffer[i * 2 + 1] = point.y;
        }
        return buffer;
    }

    private String collectMapPointsJson() {
        MapPoints points = mapPointsProvider.provideMapPoints(lastGeneratedTrack);
        if (points == null || (points.getBlue().isEmpty() && points.getYellow().isEmpty())) {
            // TODO: Replace fallback with real map data wiring when available.
            points = MapPoints.createDemoCourse();
        }
        return points.toJsonString();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        renderExecutor.shutdownNow();
    }
}
