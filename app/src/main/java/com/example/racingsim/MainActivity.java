package com.example.racingsim;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.racingsim.track.TrackData;
import com.example.racingsim.track.TrackGenerator;
import com.example.racingsim.track.TrackRenderer;

public class MainActivity extends AppCompatActivity {

    private ImageView trackImageView;
    private final TrackGenerator trackGenerator = new TrackGenerator();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        trackImageView = findViewById(R.id.trackImageView);
        Button generateButton = findViewById(R.id.generateButton);

        generateButton.setOnClickListener(v -> generateAndShowTrack());
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
        Bitmap bitmap = TrackRenderer.renderTrack(width, height, trackData);
        trackImageView.setImageBitmap(bitmap);
    }
}
