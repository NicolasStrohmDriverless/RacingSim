package com.example.racingsim.gl;

import android.content.Context;
import android.opengl.GLSurfaceView;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;

import com.example.racingsim.model.MapPoints;

/**
 * Custom GLSurfaceView that wires touch input into the renderer.
 */
public class Map3DView extends GLSurfaceView {

    private static final float ORBIT_SENSITIVITY = 0.4f;
    private static final float PITCH_SENSITIVITY = 0.3f;

    private final Map3DRenderer renderer;
    private final ScaleGestureDetector scaleGestureDetector;

    private float previousX;
    private float previousY;

    public Map3DView(Context context, MapPoints mapPoints) {
        super(context);
        setEGLContextClientVersion(2);
        setEGLConfigChooser(8, 8, 8, 8, 16, 0);

        renderer = new Map3DRenderer(context, mapPoints);
        setRenderer(renderer);
        setRenderMode(RENDERMODE_CONTINUOUSLY);

        scaleGestureDetector = new ScaleGestureDetector(context, new ScaleGestureDetector.SimpleOnScaleGestureListener() {
            @Override
            public boolean onScale(ScaleGestureDetector detector) {
                renderer.applyZoom(detector.getScaleFactor());
                return true;
            }
        });
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        scaleGestureDetector.onTouchEvent(event);

        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                previousX = event.getX();
                previousY = event.getY();
                break;
            case MotionEvent.ACTION_POINTER_DOWN:
                previousX = event.getX(0);
                previousY = event.getY(0);
                break;
            case MotionEvent.ACTION_POINTER_UP:
                if (event.getPointerCount() > 1) {
                    int index = event.getActionIndex() == 0 ? 1 : 0;
                    previousX = event.getX(index);
                    previousY = event.getY(index);
                } else {
                    previousX = event.getX();
                    previousY = event.getY();
                }
                break;
            case MotionEvent.ACTION_MOVE:
                if (!scaleGestureDetector.isInProgress() && event.getPointerCount() == 1) {
                    float currentX = event.getX();
                    float currentY = event.getY();
                    float deltaX = currentX - previousX;
                    float deltaY = currentY - previousY;
                    renderer.applyOrbitDelta(deltaX * ORBIT_SENSITIVITY, deltaY * PITCH_SENSITIVITY);
                    previousX = currentX;
                    previousY = currentY;
                }
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                previousX = 0f;
                previousY = 0f;
                break;
            default:
                break;
        }
        return true;
    }
}
