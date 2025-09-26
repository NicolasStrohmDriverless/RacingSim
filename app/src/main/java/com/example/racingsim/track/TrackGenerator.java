package com.example.racingsim.track;

import android.graphics.Path;
import android.graphics.PointF;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

public class TrackGenerator {

    private static final int MIN_CONTROL_POINTS = 8;
    private static final int MAX_CONTROL_POINTS = 12;
    private static final int SAMPLES_PER_SEGMENT = 22;

    private final Random random = new Random();

    public TrackData generate(int width, int height) {
        float minDimen = Math.min(width, height);
        float cx = width / 2f;
        float cy = height / 2f;

        List<PointF> controlPoints = createControlPoints(cx, cy, minDimen);
        List<PointF> centerline = createCenterline(controlPoints);
        Path centerlinePath = buildPath(centerline);

        float trackWidth = minDimen * 0.22f;
        float coneSpacing = trackWidth * 0.9f;

        List<PointF> leftCones = new ArrayList<>();
        List<PointF> rightCones = new ArrayList<>();
        populateConePositions(centerline, trackWidth, coneSpacing, leftCones, rightCones);

        return new TrackData(centerlinePath,
                new ArrayList<>(centerline),
                new ArrayList<>(leftCones),
                new ArrayList<>(rightCones),
                trackWidth);
    }

    private List<PointF> createControlPoints(float cx, float cy, float minDimen) {
        int count = random.nextInt(MAX_CONTROL_POINTS - MIN_CONTROL_POINTS + 1) + MIN_CONTROL_POINTS;
        List<Float> angles = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            float angle = (float) (random.nextFloat() * Math.PI * 2.0);
            angles.add(angle);
        }
        Collections.sort(angles);

        float minRadius = minDimen * 0.28f;
        float maxRadius = minDimen * 0.45f;

        List<PointF> points = new ArrayList<>();
        for (float angle : angles) {
            float radius = minRadius + random.nextFloat() * (maxRadius - minRadius);
            float wobble = (random.nextFloat() - 0.5f) * minDimen * 0.04f;
            float x = cx + (float) Math.cos(angle) * (radius + wobble);
            float y = cy + (float) Math.sin(angle) * (radius + wobble);
            points.add(new PointF(x, y));
        }
        return points;
    }

    private List<PointF> createCenterline(List<PointF> controlPoints) {
        ArrayList<PointF> output = new ArrayList<>();
        int n = controlPoints.size();
        for (int i = 0; i < n; i++) {
            PointF p0 = controlPoints.get((i - 1 + n) % n);
            PointF p1 = controlPoints.get(i);
            PointF p2 = controlPoints.get((i + 1) % n);
            PointF p3 = controlPoints.get((i + 2) % n);
            for (int step = 0; step < SAMPLES_PER_SEGMENT; step++) {
                float t = step / (float) SAMPLES_PER_SEGMENT;
                output.add(catmullRom(p0, p1, p2, p3, t));
            }
        }
        return output;
    }

    private PointF catmullRom(PointF p0, PointF p1, PointF p2, PointF p3, float t) {
        float t2 = t * t;
        float t3 = t2 * t;

        float x = 0.5f * ((2f * p1.x) + (-p0.x + p2.x) * t +
                (2f * p0.x - 5f * p1.x + 4f * p2.x - p3.x) * t2 +
                (-p0.x + 3f * p1.x - 3f * p2.x + p3.x) * t3);
        float y = 0.5f * ((2f * p1.y) + (-p0.y + p2.y) * t +
                (2f * p0.y - 5f * p1.y + 4f * p2.y - p3.y) * t2 +
                (-p0.y + 3f * p1.y - 3f * p2.y + p3.y) * t3);
        return new PointF(x, y);
    }

    private Path buildPath(List<PointF> points) {
        Path path = new Path();
        if (points.isEmpty()) {
            return path;
        }
        PointF first = points.get(0);
        path.moveTo(first.x, first.y);
        for (int i = 1; i < points.size(); i++) {
            PointF point = points.get(i);
            path.lineTo(point.x, point.y);
        }
        path.close();
        return path;
    }

    private void populateConePositions(List<PointF> centerline,
                                       float trackWidth,
                                       float spacing,
                                       List<PointF> leftCones,
                                       List<PointF> rightCones) {
        if (centerline.isEmpty()) {
            return;
        }

        float accumulated = 0f;
        PointF previous = centerline.get(0);
        int total = centerline.size();
        float halfWidth = trackWidth / 2f;

        for (int index = 1; index <= total; index++) {
            PointF current = centerline.get(index % total);
            float dx = current.x - previous.x;
            float dy = current.y - previous.y;
            float segmentLength = (float) Math.hypot(dx, dy);
            if (segmentLength < 1e-3f) {
                continue;
            }

            while (accumulated + segmentLength >= spacing) {
                float distanceToNext = spacing - accumulated;
                float ratio = distanceToNext / segmentLength;
                float px = previous.x + ratio * dx;
                float py = previous.y + ratio * dy;

                float tangentX = dx / segmentLength;
                float tangentY = dy / segmentLength;
                float normalX = -tangentY;
                float normalY = tangentX;

                leftCones.add(new PointF(px + normalX * halfWidth, py + normalY * halfWidth));
                rightCones.add(new PointF(px - normalX * halfWidth, py - normalY * halfWidth));

                previous = new PointF(px, py);
                dx = current.x - previous.x;
                dy = current.y - previous.y;
                segmentLength = (float) Math.hypot(dx, dy);
                accumulated = 0f;
            }

            accumulated += segmentLength;
            previous = current;
        }

        // Ensure the cones start with the closest pair to the bottom of the screen for visual variety
        if (!leftCones.isEmpty()) {
            Comparator<PointF> comparator = Comparator.comparing(point -> point.y);
            int pivot = 0;
            for (int i = 1; i < leftCones.size(); i++) {
                if (comparator.compare(leftCones.get(i), leftCones.get(pivot)) > 0) {
                    pivot = i;
                }
            }
            rotateLists(leftCones, pivot);
            rotateLists(rightCones, pivot);
        }
    }

    private void rotateLists(List<PointF> points, int pivot) {
        if (pivot <= 0 || pivot >= points.size()) {
            return;
        }
        Collections.rotate(points, points.size() - pivot);
    }
}
