package com.example.racingsim.model;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Container for blue and yellow cone positions in meters.
 */
public class MapPoints {

    public static final String JSON_KEY_BLUE = "blue";
    public static final String JSON_KEY_YELLOW = "yellow";

    private final List<float[]> blue;
    private final List<float[]> yellow;

    public MapPoints(List<float[]> bluePoints, List<float[]> yellowPoints) {
        this.blue = copyPoints(bluePoints);
        this.yellow = copyPoints(yellowPoints);
    }

    private static List<float[]> copyPoints(List<float[]> source) {
        List<float[]> result = new ArrayList<>();
        if (source == null) {
            return result;
        }
        for (float[] point : source) {
            if (point == null || point.length < 2) {
                continue;
            }
            result.add(new float[]{point[0], point[1]});
        }
        return result;
    }

    public List<float[]> getBlue() {
        return Collections.unmodifiableList(blue);
    }

    public List<float[]> getYellow() {
        return Collections.unmodifiableList(yellow);
    }

    public String toJsonString() {
        JSONObject object = new JSONObject();
        try {
            object.put(JSON_KEY_BLUE, toArray(blue));
            object.put(JSON_KEY_YELLOW, toArray(yellow));
        } catch (JSONException e) {
            throw new IllegalStateException("Failed to serialise map points", e);
        }
        return object.toString();
    }

    private static JSONArray toArray(List<float[]> points) {
        JSONArray array = new JSONArray();
        for (float[] point : points) {
            JSONArray entry = new JSONArray();
            try {
                entry.put(point[0]);
                entry.put(point[1]);
            } catch (JSONException e) {
                throw new IllegalStateException("Failed to serialise map point", e);
            }
            array.put(entry);
        }
        return array;
    }

    public static MapPoints fromJson(String json) {
        if (json == null || json.isEmpty()) {
            return new MapPoints(new ArrayList<>(), new ArrayList<>());
        }
        try {
            JSONObject object = new JSONObject(json);
            List<float[]> blue = parseArray(object.optJSONArray(JSON_KEY_BLUE));
            List<float[]> yellow = parseArray(object.optJSONArray(JSON_KEY_YELLOW));
            return new MapPoints(blue, yellow);
        } catch (JSONException e) {
            throw new IllegalArgumentException("Invalid map points JSON", e);
        }
    }

    private static List<float[]> parseArray(JSONArray array) throws JSONException {
        List<float[]> points = new ArrayList<>();
        if (array == null) {
            return points;
        }
        for (int i = 0; i < array.length(); i++) {
            JSONArray entry = array.getJSONArray(i);
            if (entry.length() < 2) {
                continue;
            }
            float x = (float) entry.getDouble(0);
            float y = (float) entry.getDouble(1);
            points.add(new float[]{x, y});
        }
        return points;
    }

    public static MapPoints createDemoCourse() {
        List<float[]> blue = new ArrayList<>();
        List<float[]> yellow = new ArrayList<>();

        // Simple slalom demo course to keep the preview interesting before real data is wired up.
        blue.add(new float[]{-4f, 5f});
        blue.add(new float[]{-3f, 15f});
        blue.add(new float[]{-4f, 25f});
        blue.add(new float[]{-3f, 35f});

        yellow.add(new float[]{4f, 10f});
        yellow.add(new float[]{3f, 20f});
        yellow.add(new float[]{4f, 30f});
        yellow.add(new float[]{3f, 40f});

        return new MapPoints(blue, yellow);
    }
}
