package com.example.racingsim.track;

import android.graphics.Path;
import android.graphics.PointF;

import java.util.Collections;
import java.util.List;

public class TrackData {
    private final Path centerlinePath;
    private final List<PointF> centerlinePoints;
    private final List<PointF> leftCones;
    private final List<PointF> rightCones;
    private final float trackWidth;

    public TrackData(Path centerlinePath,
                     List<PointF> centerlinePoints,
                     List<PointF> leftCones,
                     List<PointF> rightCones,
                     float trackWidth) {
        this.centerlinePath = new Path(centerlinePath);
        this.centerlinePoints = Collections.unmodifiableList(centerlinePoints);
        this.leftCones = Collections.unmodifiableList(leftCones);
        this.rightCones = Collections.unmodifiableList(rightCones);
        this.trackWidth = trackWidth;
    }

    public Path getCenterlinePath() {
        return centerlinePath;
    }

    public List<PointF> getCenterlinePoints() {
        return centerlinePoints;
    }

    public List<PointF> getLeftCones() {
        return leftCones;
    }

    public List<PointF> getRightCones() {
        return rightCones;
    }

    public float getTrackWidth() {
        return trackWidth;
    }
}
