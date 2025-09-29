// NEW FILE
package com.example.racingsim.preview;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class PreviewActivity extends Activity {

    public static final String EXTRA_TRACK_POINTS = "com.example.racingsim.extra.TRACK_POINTS";
    public static final String EXTRA_TRACK_WIDTH = "com.example.racingsim.extra.TRACK_WIDTH";

    static {
        System.loadLibrary("racingsim");
    }

    private PreviewSurfaceView surfaceView;

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

        surfaceView = new PreviewSurfaceView(this);
        setContentView(surfaceView);
        nativeLoadTrack(points, points.length / 2, trackWidth);
    }

    @Override
    protected void onResume() {
        super.onResume();
        surfaceView.onResume();
    }

    @Override
    protected void onPause() {
        surfaceView.onPause();
        super.onPause();
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
            setOnTouchListener(this::onTouchEventForwarded);
        }

        private boolean onTouchEventForwarded(View view, MotionEvent event) {
            return handleTouch(event);
        }

        @Override
        public boolean onTouchEvent(MotionEvent event) {
            return handleTouch(event);
        }

        private boolean handleTouch(MotionEvent event) {
            int action = event.getActionMasked();
            if (action == MotionEvent.ACTION_CANCEL || action == MotionEvent.ACTION_UP) {
                nativeOnTouch(action, 0.0f, 0.0f);
                return true;
            }

            float steer = 0.0f;
            boolean steerActive = false;
            float drive = 0.0f;
            boolean driveActive = false;

            final float width = Math.max(1f, getWidth());
            final float height = Math.max(1f, getHeight());
            final float halfWidth = width * 0.5f;

            for (int i = 0; i < event.getPointerCount(); i++) {
                if (action == MotionEvent.ACTION_POINTER_UP && event.getActionIndex() == i) {
                    continue;
                }
                float x = event.getX(i);
                float y = event.getY(i);
                if (x < halfWidth) {
                    float normalized = ((x / halfWidth) * 2.0f) - 1.0f;
                    steer = clamp(normalized);
                    steerActive = true;
                } else {
                    float normalizedY = 1.0f - ((y / height) * 2.0f);
                    drive = clamp(normalizedY);
                    driveActive = true;
                }
            }

            if (!steerActive) {
                steer = 0.0f;
            }
            if (!driveActive) {
                drive = 0.0f;
            }
            nativeOnTouch(action, steer, drive);
            return true;
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

