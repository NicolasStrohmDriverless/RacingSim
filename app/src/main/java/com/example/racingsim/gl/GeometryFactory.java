package com.example.racingsim.gl;

import java.util.ArrayList;
import java.util.List;

/**
 * Creates simple geometry meshes used by the 3D preview.
 */
public final class GeometryFactory {

    private GeometryFactory() {
    }

    public static Mesh createConeWithStripe(float radiusBase,
                                            float height,
                                            int slices,
                                            float stripeCenter,
                                            float stripeHeight,
                                            Color3f bodyColor,
                                            Color3f stripeColor) {
        float stripeHalf = stripeHeight * 0.5f;
        float stripeLower = clamp(stripeCenter - stripeHalf, 0f, height);
        float stripeUpper = clamp(stripeCenter + stripeHalf, 0f, height);

        List<Float> ringHeights = new ArrayList<>();
        ringHeights.add(0f);
        if (stripeLower > 0.0001f) {
            ringHeights.add(stripeLower);
        }
        if (stripeUpper > stripeLower + 0.0001f && stripeUpper < height) {
            ringHeights.add(stripeUpper);
        }
        ringHeights.add(height);

        int ringCount = ringHeights.size();
        int verticesPerRing = slices + 1;
        int totalSideVertices = ringCount * verticesPerRing;
        List<Float> vertices = new ArrayList<>(totalSideVertices * 9);

        float slope = radiusBase / height;
        float normalScale = (float) Math.sqrt(1f + slope * slope);
        float normalZ = slope / normalScale;

        for (int ringIndex = 0; ringIndex < ringCount; ringIndex++) {
            float currentHeight = ringHeights.get(ringIndex);
            float radius = radiusBase * (1f - (currentHeight / height));
            Color3f color = selectColorForHeight(currentHeight, stripeLower, stripeUpper, bodyColor, stripeColor);
            for (int slice = 0; slice <= slices; slice++) {
                float angle = (float) (2.0 * Math.PI * slice / slices);
                float cos = (float) Math.cos(angle);
                float sin = (float) Math.sin(angle);
                float x = radius * cos;
                float y = radius * sin;
                float nx = cos / normalScale;
                float ny = sin / normalScale;
                float nz = normalZ;
                // Position (x, y, z)
                vertices.add(x);
                vertices.add(y);
                vertices.add(currentHeight);
                // Normal (x, y, z)
                vertices.add(nx);
                vertices.add(ny);
                vertices.add(nz);
                // Color (r, g, b)
                vertices.add(color.r);
                vertices.add(color.g);
                vertices.add(color.b);
            }
        }

        List<Short> indices = new ArrayList<>(slices * (ringCount - 1) * 6 + slices * 3);
        for (int ring = 0; ring < ringCount - 1; ring++) {
            int ringStart = ring * verticesPerRing;
            int nextRingStart = (ring + 1) * verticesPerRing;
            for (int slice = 0; slice < slices; slice++) {
                short current = (short) (ringStart + slice);
                short next = (short) (ringStart + slice + 1);
                short upper = (short) (nextRingStart + slice);
                short upperNext = (short) (nextRingStart + slice + 1);

                indices.add(current);
                indices.add(upper);
                indices.add(upperNext);

                indices.add(current);
                indices.add(upperNext);
                indices.add(next);
            }
        }

        // Base disc (downward normal)
        Color3f baseColor = bodyColor;
        short baseCenterIndex = (short) (totalSideVertices);
        vertices.add(0f);
        vertices.add(0f);
        vertices.add(0f);
        vertices.add(0f);
        vertices.add(0f);
        vertices.add(-1f);
        vertices.add(baseColor.r);
        vertices.add(baseColor.g);
        vertices.add(baseColor.b);

        for (int slice = 0; slice < slices; slice++) {
            short current = (short) slice;
            short next = (short) (slice + 1);
            indices.add(baseCenterIndex);
            indices.add(next);
            indices.add(current);
        }

        float[] vertexArray = new float[vertices.size()];
        for (int i = 0; i < vertices.size(); i++) {
            vertexArray[i] = vertices.get(i);
        }
        short[] indexArray = new short[indices.size()];
        for (int i = 0; i < indices.size(); i++) {
            indexArray[i] = indices.get(i);
        }

        return new Mesh(vertexArray, indexArray, new int[]{3, 3, 3});
    }

    public static Mesh createCylinder(float radius,
                                      float height,
                                      int slices,
                                      Color3f color) {
        int ringCount = 2;
        int verticesPerRing = slices + 1;
        int totalSideVertices = ringCount * verticesPerRing;
        List<Float> vertices = new ArrayList<>(totalSideVertices * 9);

        float[] ringHeights = new float[]{0f, height};
        for (int ringIndex = 0; ringIndex < ringCount; ringIndex++) {
            float currentHeight = ringHeights[ringIndex];
            for (int slice = 0; slice <= slices; slice++) {
                float angle = (float) (2.0 * Math.PI * slice / slices);
                float cos = (float) Math.cos(angle);
                float sin = (float) Math.sin(angle);
                float x = radius * cos;
                float y = radius * sin;
                float nx = cos;
                float ny = sin;
                // Position (x, y, z)
                vertices.add(x);
                vertices.add(y);
                vertices.add(currentHeight);
                // Normal (x, y, z)
                vertices.add(nx);
                vertices.add(ny);
                vertices.add(0f);
                // Color (r, g, b)
                vertices.add(color.r);
                vertices.add(color.g);
                vertices.add(color.b);
            }
        }

        List<Short> indices = new ArrayList<>(slices * (ringCount - 1) * 6 + slices * 6);
        for (int ring = 0; ring < ringCount - 1; ring++) {
            int ringStart = ring * verticesPerRing;
            int nextRingStart = (ring + 1) * verticesPerRing;
            for (int slice = 0; slice < slices; slice++) {
                short current = (short) (ringStart + slice);
                short next = (short) (ringStart + slice + 1);
                short upper = (short) (nextRingStart + slice);
                short upperNext = (short) (nextRingStart + slice + 1);

                indices.add(current);
                indices.add(upper);
                indices.add(upperNext);

                indices.add(current);
                indices.add(upperNext);
                indices.add(next);
            }
        }

        // Bottom disc (downward normal)
        short bottomCenterIndex = (short) (totalSideVertices);
        vertices.add(0f);
        vertices.add(0f);
        vertices.add(0f);
        vertices.add(0f);
        vertices.add(0f);
        vertices.add(-1f);
        vertices.add(color.r);
        vertices.add(color.g);
        vertices.add(color.b);

        for (int slice = 0; slice < slices; slice++) {
            short current = (short) slice;
            short next = (short) (slice + 1);
            indices.add(bottomCenterIndex);
            indices.add(next);
            indices.add(current);
        }

        // Top disc (upward normal)
        short topCenterIndex = (short) (totalSideVertices + 1);
        vertices.add(0f);
        vertices.add(0f);
        vertices.add(height);
        vertices.add(0f);
        vertices.add(0f);
        vertices.add(1f);
        vertices.add(color.r);
        vertices.add(color.g);
        vertices.add(color.b);

        int topRingStart = verticesPerRing;
        for (int slice = 0; slice < slices; slice++) {
            short current = (short) (topRingStart + slice);
            short next = (short) (topRingStart + slice + 1);
            indices.add(topCenterIndex);
            indices.add(current);
            indices.add(next);
        }

        float[] vertexArray = new float[vertices.size()];
        for (int i = 0; i < vertices.size(); i++) {
            vertexArray[i] = vertices.get(i);
        }
        short[] indexArray = new short[indices.size()];
        for (int i = 0; i < indices.size(); i++) {
            indexArray[i] = indices.get(i);
        }

        return new Mesh(vertexArray, indexArray, new int[]{3, 3, 3});
    }

    private static Color3f selectColorForHeight(float height,
                                                float stripeLower,
                                                float stripeUpper,
                                                Color3f bodyColor,
                                                Color3f stripeColor) {
        if (height >= stripeLower && height <= stripeUpper) {
            return stripeColor;
        }
        return bodyColor;
    }

    private static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }

    public static Mesh createGround(float size, Color3f color) {
        float half = size * 0.5f;
        float[] vertices = new float[]{
                -half, -half, 0f, 0f, 0f, 1f, color.r, color.g, color.b,
                half, -half, 0f, 0f, 0f, 1f, color.r, color.g, color.b,
                half, half, 0f, 0f, 0f, 1f, color.r, color.g, color.b,
                -half, half, 0f, 0f, 0f, 1f, color.r, color.g, color.b
        };
        short[] indices = new short[]{
                0, 1, 2,
                0, 2, 3
        };
        return new Mesh(vertices, indices, new int[]{3, 3, 3});
    }

    public static Mesh createTexturedQuad(float width, float height) {
        float halfWidth = width * 0.5f;
        float[] vertices = new float[]{
                -halfWidth, 0f, 0f, 0f, 1f,
                halfWidth, 0f, 0f, 1f, 1f,
                halfWidth, 0f, height, 1f, 0f,
                -halfWidth, 0f, height, 0f, 0f
        };
        short[] indices = new short[]{0, 1, 2, 0, 2, 3};
        return new Mesh(vertices, indices, new int[]{3, 2});
    }

    public static final class Color3f {
        public final float r;
        public final float g;
        public final float b;

        public Color3f(float r, float g, float b) {
            this.r = r;
            this.g = g;
            this.b = b;
        }
    }
}
