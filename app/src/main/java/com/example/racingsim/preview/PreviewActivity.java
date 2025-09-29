package com.example.racingsim.preview;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.racingsim.R;
import com.google.android.material.button.MaterialButton;

public class PreviewActivity extends Activity implements SensorEventListener {

    public static final String EXTRA_TRACK_POINTS = "com.example.racingsim.extra.TRACK_POINTS";
    public static final String EXTRA_TRACK_WIDTH = "com.example.racingsim.extra.TRACK_WIDTH";

    static {
        System.loadLibrary("racingsim");
    }

    private PreviewSurfaceView surfaceView;
    private SensorManager sensorManager;
    private Sensor accelerometer;
    private boolean acceleratePressed;
    private boolean brakePressed;
    private float currentSteer;
    private float currentDrive;

    private static native void nativeInit();
    private static native void nativeResize(int width, int height);
    private static native void nativeRender();
    private static native void nativeOnTouch(int action, float x, float y);
    private static native void nativeLoadTrack(float[] xy, int count, float width);

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        Intent intent = getIntent();
        float[] points = intent.getFloatArrayExtra(EXTRA_TRACK_POINTS);
        float trackWidth = intent.getFloatExtra(EXTRA_TRACK_WIDTH, 0.0f);
        if (points == null || points.length < 4 || trackWidth <= 0.0f) {
            Toast.makeText(this, "Track data unavailable", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        if (sensorManager != null) {
            accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        }
        if (accelerometer == null) {
            Toast.makeText(this, R.string.message_no_accelerometer, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        setContentView(R.layout.activity_map_3d);
        android.widget.FrameLayout container = findViewById(R.id.map3d_container);
        surfaceView = new PreviewSurfaceView(this);
        container.addView(surfaceView, new android.widget.FrameLayout.LayoutParams(
                android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                android.view.ViewGroup.LayoutParams.MATCH_PARENT));

        MaterialButton accelerateButton = findViewById(R.id.accelerateButton);
        MaterialButton brakeButton = findViewById(R.id.brakeButton);

        accelerateButton.setOnTouchListener((view, event) -> handlePedalTouch(view, event, true));
        brakeButton.setOnTouchListener((view, event) -> handlePedalTouch(view, event, false));

        updateDriveState();
        nativeLoadTrack(points, points.length / 2, trackWidth);
    }

    @Override
    protected void onResume() {
        super.onResume();
        surfaceView.onResume();
        if (sensorManager != null && accelerometer != null) {
            sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_GAME);
        }
    }

    @Override
    protected void onPause() {
        if (sensorManager != null) {
            sensorManager.unregisterListener(this);
        }
        nativeOnTouch(MotionEvent.ACTION_UP, 0.0f, 0.0f);
        surfaceView.onPause();
        super.onPause();
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() != Sensor.TYPE_ACCELEROMETER) {
            return;
        }
        float normalized = -event.values[0] / SensorManager.GRAVITY_EARTH;
        currentSteer = clamp(normalized);
        sendControlState(MotionEvent.ACTION_MOVE);
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // No-op
    }

    private boolean handlePedalTouch(View view, MotionEvent event, boolean isAccelerator) {
        int action = event.getActionMasked();
        if (action == MotionEvent.ACTION_DOWN || action == MotionEvent.ACTION_POINTER_DOWN || action == MotionEvent.ACTION_MOVE) {
            if (isAccelerator) {
                acceleratePressed = true;
            } else {
                brakePressed = true;
            }
        } else if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_POINTER_UP || action == MotionEvent.ACTION_CANCEL) {
            if (isAccelerator) {
                acceleratePressed = false;
            } else {
                brakePressed = false;
            }
            if (action == MotionEvent.ACTION_UP) {
                view.performClick();
            }
        } else {
            return false;
        }
        updateDriveState();
        return true;
    }

    private void updateDriveState() {
        float driveValue = 0.0f;
        if (acceleratePressed && !brakePressed) {
            driveValue = 1.0f;
        } else if (brakePressed && !acceleratePressed) {
            driveValue = -1.0f;
        }
        currentDrive = driveValue;
        sendControlState(MotionEvent.ACTION_MOVE);
    }

    private void sendControlState(int action) {
        nativeOnTouch(action, currentSteer, currentDrive);
    }

    private float clamp(float value) {
        if (value < -1.0f) {
            return -1.0f;
        }
        if (value > 1.0f) {
            return 1.0f;
        }
        return value;
    }

    private static class PreviewSurfaceView extends GLSurfaceView {

        private final PreviewRenderer renderer;

        PreviewSurfaceView(Context context) {
            super(context);
            setEGLContextClientVersion(3);
            renderer = new PreviewRenderer();
            setRenderer(renderer);
            setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY);
            setPreserveEGLContextOnPause(true);
        }
    }

    private static class PreviewRenderer implements GLSurfaceView.Renderer {

        @Override
        public void onSurfaceCreated(@NonNull javax.microedition.khronos.opengles.GL10 gl, @NonNull javax.microedition.khronos.egl.EGLConfig config) {
            nativeInit();
        }

        @Override
        public void onSurfaceChanged(@NonNull javax.microedition.khronos.opengles.GL10 gl, int width, int height) {
            nativeResize(width, height);
        }

        @Override
        public void onDrawFrame(@NonNull javax.microedition.khronos.opengles.GL10 gl) {
            nativeRender();
        }
    }
}

