package com.example.racingsim.track;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PointF;
import android.graphics.RadialGradient;
import android.graphics.Shader;

import java.util.Random;

public final class TrackRenderer {

    private TrackRenderer() {
    }

    public static Bitmap renderTrack(int width, int height, TrackData data) {
        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);

        drawBackground(canvas, width, height);
        drawTrack(canvas, data);
        drawCones(canvas, data);
        drawSparkles(canvas, width, height);

        return bitmap;
    }

    private static void drawBackground(Canvas canvas, int width, int height) {
        Paint gradientPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        Shader gradient = new LinearGradient(
                0, 0, 0, height,
                Color.parseColor("#FF6EE7FF"),
                Color.parseColor("#FFFFD56F"),
                Shader.TileMode.CLAMP);
        gradientPaint.setShader(gradient);
        canvas.drawRect(0, 0, width, height, gradientPaint);

        Paint bubblePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        bubblePaint.setColor(Color.parseColor("#66FFFFFF"));
        float bubbleRadius = Math.min(width, height) * 0.08f;
        canvas.drawCircle(width * 0.2f, height * 0.25f, bubbleRadius, bubblePaint);
        canvas.drawCircle(width * 0.85f, height * 0.3f, bubbleRadius * 0.9f, bubblePaint);
        canvas.drawCircle(width * 0.12f, height * 0.75f, bubbleRadius * 0.7f, bubblePaint);

        Paint stripePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        stripePaint.setColor(Color.parseColor("#33FFFFFF"));
        stripePaint.setStrokeWidth(bubbleRadius * 0.6f);
        stripePaint.setStrokeCap(Paint.Cap.ROUND);
        canvas.drawLine(width * 0.1f, height * 0.55f, width * 0.3f, height * 0.45f, stripePaint);
        canvas.drawLine(width * 0.75f, height * 0.7f, width * 0.95f, height * 0.6f, stripePaint);
    }

    private static void drawTrack(Canvas canvas, TrackData data) {
        Path path = data.getCenterlinePath();
        float trackWidth = data.getTrackWidth();

        Paint outlinePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        outlinePaint.setColor(Color.parseColor("#FF2B1A7C"));
        outlinePaint.setStyle(Paint.Style.STROKE);
        outlinePaint.setStrokeCap(Paint.Cap.ROUND);
        outlinePaint.setStrokeJoin(Paint.Join.ROUND);
        outlinePaint.setStrokeWidth(trackWidth * 1.2f);
        outlinePaint.setShadowLayer(trackWidth * 0.18f, 0f, trackWidth * 0.12f, Color.parseColor("#5512183D"));
        canvas.drawPath(path, outlinePaint);

        Paint trackPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        trackPaint.setColor(Color.parseColor("#FF9C6CFF"));
        trackPaint.setStyle(Paint.Style.STROKE);
        trackPaint.setStrokeCap(Paint.Cap.ROUND);
        trackPaint.setStrokeJoin(Paint.Join.ROUND);
        trackPaint.setStrokeWidth(trackWidth);
        trackPaint.setShadowLayer(trackWidth * 0.1f, 0f, trackWidth * 0.08f, Color.parseColor("#33271233"));
        canvas.drawPath(path, trackPaint);

        Paint shinePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        shinePaint.setColor(Color.parseColor("#66FFFFFF"));
        shinePaint.setStyle(Paint.Style.STROKE);
        shinePaint.setStrokeCap(Paint.Cap.ROUND);
        shinePaint.setStrokeJoin(Paint.Join.ROUND);
        shinePaint.setStrokeWidth(trackWidth * 0.25f);
        canvas.drawPath(path, shinePaint);
    }

    private static void drawCones(Canvas canvas, TrackData data) {
        float coneSize = data.getTrackWidth() * 0.32f;
        Paint bluePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        bluePaint.setStyle(Paint.Style.FILL);
        bluePaint.setColor(Color.parseColor("#FF2F68FF"));

        Paint yellowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        yellowPaint.setStyle(Paint.Style.FILL);
        yellowPaint.setColor(Color.parseColor("#FFFFE159"));

        Paint outlinePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        outlinePaint.setStyle(Paint.Style.STROKE);
        outlinePaint.setStrokeWidth(coneSize * 0.2f);
        outlinePaint.setColor(Color.parseColor("#1A000000"));

        Paint highlightPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        highlightPaint.setColor(Color.parseColor("#CCFFFFFF"));
        highlightPaint.setStyle(Paint.Style.FILL);

        for (PointF point : data.getLeftCones()) {
            drawCone(canvas, point, coneSize, bluePaint, outlinePaint, highlightPaint);
        }
        for (PointF point : data.getRightCones()) {
            drawCone(canvas, point, coneSize, yellowPaint, outlinePaint, highlightPaint);
        }
    }

    private static void drawCone(Canvas canvas,
                                 PointF point,
                                 float size,
                                 Paint fillPaint,
                                 Paint outlinePaint,
                                 Paint highlightPaint) {
        Path conePath = new Path();
        conePath.moveTo(point.x, point.y - size * 1.4f);
        conePath.lineTo(point.x + size, point.y + size);
        conePath.lineTo(point.x - size, point.y + size);
        conePath.close();

        fillPaint.setShadowLayer(size * 0.45f, 0f, size * 0.25f, Color.parseColor("#55000000"));
        canvas.drawPath(conePath, fillPaint);
        canvas.drawPath(conePath, outlinePaint);

        canvas.drawCircle(point.x - size * 0.2f, point.y - size * 0.3f, size * 0.35f, highlightPaint);
    }

    private static void drawSparkles(Canvas canvas, int width, int height) {
        Paint sparklePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        sparklePaint.setStyle(Paint.Style.FILL);
        Random random = new Random();
        int count = 30;
        for (int i = 0; i < count; i++) {
            float x = random.nextFloat() * width;
            float y = random.nextFloat() * height;
            float radius = Math.max(1f, random.nextFloat() * Math.min(width, height) * 0.01f);
            Shader shader = new RadialGradient(
                    x,
                    y,
                    radius,
                    new int[]{Color.parseColor("#FFFFFFFF"), Color.parseColor("#00FFFFFF")},
                    new float[]{0.2f, 1f},
                    Shader.TileMode.CLAMP);
            sparklePaint.setShader(shader);
            canvas.drawCircle(x, y, radius, sparklePaint);
            sparklePaint.setShader(null);
        }
    }
}
