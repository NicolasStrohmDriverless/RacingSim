package com.example.racingsim.model;

import android.graphics.PointF;

import com.example.racingsim.track.TrackData;

import java.util.ArrayList;
import java.util.List;

/**
 * Default implementation that mirrors the current 2D generator output into the 3D preview format.
 */
public class DefaultMapPointsProvider implements MapPointsProvider {

    @Override
    public MapPoints provideMapPoints(TrackData trackData) {
        if (trackData == null) {
            return MapPoints.createDemoCourse();
        }
        List<float[]> blue = convert(trackData.getLeftCones());
        List<float[]> yellow = convert(trackData.getRightCones());
        if (blue.isEmpty() && yellow.isEmpty()) {
            return MapPoints.createDemoCourse();
        }
        return new MapPoints(blue, yellow);
    }

    private List<float[]> convert(List<PointF> points) {
        List<float[]> result = new ArrayList<>();
        if (points == null) {
            return result;
        }
        for (PointF point : points) {
            result.add(new float[]{point.x, point.y});
        }
        return result;
    }
}
