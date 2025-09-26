package com.example.racingsim.gl;

import android.content.Context;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;

import com.example.racingsim.R;
import com.example.racingsim.model.MapPoints;

import java.util.ArrayList;
import java.util.List;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

/**
 * Renderer responsible for drawing the ground, cones and the billboard car preview.
 */
public class Map3DRenderer implements GLSurfaceView.Renderer {

    private static final float CONE_HEIGHT_M = 0.45f;
    private static final float CONE_RADIUS_M = 0.115f;
    private static final int CONE_SLICES = 32;
    private static final float STRIPE_CENTER_RATIO = 0.25f;
    private static final float STRIPE_HEIGHT_RATIO = 0.06f;

    private static final float CAR_WIDTH_M = 1.6f;
    private static final float CAR_HEIGHT_M = 1.2f;
    private static final float CAR_LIFT_M = 0.35f;

    private static final float DEFAULT_CAMERA_DISTANCE = 30f;
    private static final float MIN_CAMERA_DISTANCE = 10f;
    private static final float MAX_CAMERA_DISTANCE = 90f;
    private static final float MIN_CAMERA_PITCH = 10f;
    private static final float MAX_CAMERA_PITCH = 80f;
    private static final float GROUND_MARGIN = 12f;
    private static final float MAX_NORMALISED_RADIUS = 40f;

    private final Context context;
    private final List<float[]> bluePoints = new ArrayList<>();
    private final List<float[]> yellowPoints = new ArrayList<>();

    private final float[] projectionMatrix = new float[16];
    private final float[] viewMatrix = new float[16];
    private final float[] viewProjectionMatrix = new float[16];
    private final float[] modelMatrix = new float[16];
    private final float[] mvpMatrix = new float[16];

    private ShaderProgram colorProgram;
    private TexturedProgram texturedProgram;

    private Mesh blueConeMesh;
    private Mesh yellowConeMesh;
    private Mesh groundMesh;
    private Mesh carBillboardMesh;

    private int colorMvpLocation;
    private int colorModelLocation;
    private int colorLightDirectionLocation;
    private int colorPositionAttribute;
    private int colorNormalAttribute;
    private int colorColorAttribute;
    private final int[] colorAttributeLocations = new int[3];

    private int carTextureId;
    private final int[] textureAttributeLocations = new int[2];

    private float orbitYawDegrees = 45f;
    private float orbitPitchDegrees = 40f;
    private float orbitDistance;
    private final Object cameraLock = new Object();

    private float sceneRadius;

    private final float[] lightDirection = new float[]{-0.4f, -0.7f, -1f};
    private final float[] normalizedLightDirection = new float[3];

    public Map3DRenderer(Context context, MapPoints mapPoints) {
        this.context = context.getApplicationContext();
        MapPoints safePoints = mapPoints != null ? mapPoints : MapPoints.createDemoCourse();
        copyPoints(safePoints.getBlue(), bluePoints);
        copyPoints(safePoints.getYellow(), yellowPoints);
        normaliseCourse();
        computeSceneRadius();
        orbitDistance = clamp(Math.max(DEFAULT_CAMERA_DISTANCE, sceneRadius * 1.2f),
                MIN_CAMERA_DISTANCE, MAX_CAMERA_DISTANCE);
        normaliseLightDirection();
    }

    private void normaliseCourse() {
        if (bluePoints.isEmpty() && yellowPoints.isEmpty()) {
            return;
        }

        float minX = Float.MAX_VALUE;
        float minY = Float.MAX_VALUE;
        float maxX = -Float.MAX_VALUE;
        float maxY = -Float.MAX_VALUE;

        minX = updateMin(bluePoints, 0, minX);
        minX = updateMin(yellowPoints, 0, minX);
        maxX = updateMax(bluePoints, 0, maxX);
        maxX = updateMax(yellowPoints, 0, maxX);
        minY = updateMin(bluePoints, 1, minY);
        minY = updateMin(yellowPoints, 1, minY);
        maxY = updateMax(bluePoints, 1, maxY);
        maxY = updateMax(yellowPoints, 1, maxY);

        if (minX == Float.MAX_VALUE || minY == Float.MAX_VALUE
                || maxX == -Float.MAX_VALUE || maxY == -Float.MAX_VALUE) {
            return;
        }

        float centerX = (minX + maxX) * 0.5f;
        float centerY = (minY + maxY) * 0.5f;

        float maxRadius = recenter(bluePoints, centerX, centerY);
        maxRadius = Math.max(maxRadius, recenter(yellowPoints, centerX, centerY));

        if (maxRadius <= 0f) {
            return;
        }

        if (maxRadius > MAX_NORMALISED_RADIUS) {
            float scale = MAX_NORMALISED_RADIUS / maxRadius;
            scalePoints(bluePoints, scale);
            scalePoints(yellowPoints, scale);
        }
    }

    private float updateMin(List<float[]> points, int index, float currentMin) {
        float min = currentMin;
        for (float[] point : points) {
            min = Math.min(min, point[index]);
        }
        return min;
    }

    private float updateMax(List<float[]> points, int index, float currentMax) {
        float max = currentMax;
        for (float[] point : points) {
            max = Math.max(max, point[index]);
        }
        return max;
    }

    private float recenter(List<float[]> points, float centerX, float centerY) {
        float maxRadius = 0f;
        for (float[] point : points) {
            point[0] -= centerX;
            point[1] -= centerY;
            float radius = (float) Math.sqrt(point[0] * point[0] + point[1] * point[1]);
            maxRadius = Math.max(maxRadius, radius);
        }
        return maxRadius;
    }

    private void scalePoints(List<float[]> points, float scale) {
        for (float[] point : points) {
            point[0] *= scale;
            point[1] *= scale;
        }
    }

    private void copyPoints(List<float[]> source, List<float[]> target) {
        target.clear();
        for (float[] point : source) {
            target.add(new float[]{point[0], point[1]});
        }
    }

    private void computeSceneRadius() {
        float maxDistance = 0f;
        for (float[] point : bluePoints) {
            float distance = (float) Math.sqrt(point[0] * point[0] + point[1] * point[1]);
            maxDistance = Math.max(maxDistance, distance);
        }
        for (float[] point : yellowPoints) {
            float distance = (float) Math.sqrt(point[0] * point[0] + point[1] * point[1]);
            maxDistance = Math.max(maxDistance, distance);
        }
        sceneRadius = Math.max(15f, maxDistance + GROUND_MARGIN);
    }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        GLES20.glClearColor(0.05f, 0.05f, 0.08f, 1f);
        GLES20.glEnable(GLES20.GL_DEPTH_TEST);
        GLES20.glEnable(GLES20.GL_CULL_FACE);

        colorProgram = new ShaderProgram(COLOR_VERTEX_SHADER, COLOR_FRAGMENT_SHADER);
        colorMvpLocation = colorProgram.getUniformLocation("uMVPMatrix");
        colorModelLocation = colorProgram.getUniformLocation("uModelMatrix");
        colorLightDirectionLocation = colorProgram.getUniformLocation("uLightDirection");
        colorPositionAttribute = colorProgram.getAttributeLocation("aPosition");
        colorNormalAttribute = colorProgram.getAttributeLocation("aNormal");
        colorColorAttribute = colorProgram.getAttributeLocation("aColor");
        colorAttributeLocations[0] = colorPositionAttribute;
        colorAttributeLocations[1] = colorNormalAttribute;
        colorAttributeLocations[2] = colorColorAttribute;

        texturedProgram = new TexturedProgram();
        textureAttributeLocations[0] = texturedProgram.getPositionAttribute();
        textureAttributeLocations[1] = texturedProgram.getTexCoordAttribute();

        GeometryFactory.Color3f blueBody = new GeometryFactory.Color3f(0.0f, 0.35f, 0.9f);
        GeometryFactory.Color3f blueStripe = new GeometryFactory.Color3f(1f, 1f, 1f);
        GeometryFactory.Color3f yellowBody = new GeometryFactory.Color3f(0.95f, 0.8f, 0.05f);
        GeometryFactory.Color3f yellowStripe = new GeometryFactory.Color3f(0f, 0f, 0f);
        GeometryFactory.Color3f groundColor = new GeometryFactory.Color3f(0.1f, 0.1f, 0.12f);

        float stripeCenter = CONE_HEIGHT_M * STRIPE_CENTER_RATIO;
        float stripeHeight = CONE_HEIGHT_M * STRIPE_HEIGHT_RATIO;

        blueConeMesh = GeometryFactory.createConeWithStripe(CONE_RADIUS_M, CONE_HEIGHT_M, CONE_SLICES,
                stripeCenter, stripeHeight, blueBody, blueStripe);
        yellowConeMesh = GeometryFactory.createConeWithStripe(CONE_RADIUS_M, CONE_HEIGHT_M, CONE_SLICES,
                stripeCenter, stripeHeight, yellowBody, yellowStripe);
        groundMesh = GeometryFactory.createGround(sceneRadius * 2f, groundColor);
        carBillboardMesh = GeometryFactory.createTexturedQuad(CAR_WIDTH_M, CAR_HEIGHT_M);

        carTextureId = TextureUtils.loadTexture(context, R.drawable.car_nora);
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        GLES20.glViewport(0, 0, width, height);
        float aspect = (float) width / (float) height;
        Matrix.perspectiveM(projectionMatrix, 0, 45f, aspect, 0.1f, 400f);
    }

    @Override
    public void onDrawFrame(GL10 gl) {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);

        float yaw;
        float pitch;
        float distance;
        synchronized (cameraLock) {
            yaw = orbitYawDegrees;
            pitch = orbitPitchDegrees;
            distance = orbitDistance;
        }

        float yawRad = (float) Math.toRadians(yaw);
        float pitchRad = (float) Math.toRadians(pitch);
        float cosPitch = (float) Math.cos(pitchRad);
        float eyeX = (float) (distance * Math.sin(yawRad) * cosPitch);
        float eyeY = (float) (distance * Math.cos(yawRad) * cosPitch);
        float eyeZ = (float) (distance * Math.sin(pitchRad));

        Matrix.setLookAtM(viewMatrix, 0, eyeX, eyeY, eyeZ, 0f, 0f, 0f, 0f, 0f, 1f);
        Matrix.multiplyMM(viewProjectionMatrix, 0, projectionMatrix, 0, viewMatrix, 0);

        drawGround();
        drawCones(blueConeMesh, bluePoints);
        drawCones(yellowConeMesh, yellowPoints);
        drawCarBillboard(yaw);
    }

    private void drawGround() {
        colorProgram.use();
        GLES20.glUniform3f(colorLightDirectionLocation, normalizedLightDirection[0],
                normalizedLightDirection[1], normalizedLightDirection[2]);
        Matrix.setIdentityM(modelMatrix, 0);
        Matrix.multiplyMM(mvpMatrix, 0, viewProjectionMatrix, 0, modelMatrix, 0);
        GLES20.glUniformMatrix4fv(colorMvpLocation, 1, false, mvpMatrix, 0);
        GLES20.glUniformMatrix4fv(colorModelLocation, 1, false, modelMatrix, 0);
        groundMesh.draw(GLES20.GL_TRIANGLES, colorAttributeLocations);
    }

    private void drawCones(Mesh mesh, List<float[]> points) {
        colorProgram.use();
        GLES20.glUniform3f(colorLightDirectionLocation, normalizedLightDirection[0],
                normalizedLightDirection[1], normalizedLightDirection[2]);
        for (float[] point : points) {
            Matrix.setIdentityM(modelMatrix, 0);
            Matrix.translateM(modelMatrix, 0, point[0], point[1], 0f);
            Matrix.multiplyMM(mvpMatrix, 0, viewProjectionMatrix, 0, modelMatrix, 0);
            GLES20.glUniformMatrix4fv(colorMvpLocation, 1, false, mvpMatrix, 0);
            GLES20.glUniformMatrix4fv(colorModelLocation, 1, false, modelMatrix, 0);
            mesh.draw(GLES20.GL_TRIANGLES, colorAttributeLocations);
        }
    }

    private void drawCarBillboard(float cameraYawDegrees) {
        texturedProgram.use();
        Matrix.setIdentityM(modelMatrix, 0);
        Matrix.translateM(modelMatrix, 0, 0f, 0f, CAR_LIFT_M);
        Matrix.rotateM(modelMatrix, 0, cameraYawDegrees, 0f, 0f, 1f);
        Matrix.multiplyMM(mvpMatrix, 0, viewProjectionMatrix, 0, modelMatrix, 0);
        GLES20.glUniformMatrix4fv(texturedProgram.getMvpLocation(), 1, false, mvpMatrix, 0);
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, carTextureId);
        GLES20.glUniform1i(texturedProgram.getTextureLocation(), 0);
        GLES20.glDisable(GLES20.GL_CULL_FACE);
        GLES20.glEnable(GLES20.GL_BLEND);
        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA);
        carBillboardMesh.draw(GLES20.GL_TRIANGLES, textureAttributeLocations);
        GLES20.glDisable(GLES20.GL_BLEND);
        GLES20.glEnable(GLES20.GL_CULL_FACE);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);
    }

    private void normaliseLightDirection() {
        float length = (float) Math.sqrt(lightDirection[0] * lightDirection[0]
                + lightDirection[1] * lightDirection[1]
                + lightDirection[2] * lightDirection[2]);
        if (length == 0f) {
            normalizedLightDirection[0] = 0f;
            normalizedLightDirection[1] = 0f;
            normalizedLightDirection[2] = -1f;
        } else {
            normalizedLightDirection[0] = lightDirection[0] / length;
            normalizedLightDirection[1] = lightDirection[1] / length;
            normalizedLightDirection[2] = lightDirection[2] / length;
        }
    }

    public void applyOrbitDelta(float deltaYaw, float deltaPitch) {
        synchronized (cameraLock) {
            orbitYawDegrees = (orbitYawDegrees + deltaYaw) % 360f;
            orbitPitchDegrees = clamp(orbitPitchDegrees + deltaPitch, MIN_CAMERA_PITCH, MAX_CAMERA_PITCH);
        }
    }

    public void applyZoom(float scaleFactor) {
        synchronized (cameraLock) {
            float safeScale = Math.max(0.2f, Math.min(scaleFactor, 5f));
            float newDistance = orbitDistance / safeScale;
            orbitDistance = clamp(newDistance, MIN_CAMERA_DISTANCE, MAX_CAMERA_DISTANCE);
        }
    }

    private float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }

    private static final String COLOR_VERTEX_SHADER =
            "uniform mat4 uMVPMatrix;\n" +
                    "uniform mat4 uModelMatrix;\n" +
                    "uniform vec3 uLightDirection;\n" +
                    "attribute vec3 aPosition;\n" +
                    "attribute vec3 aNormal;\n" +
                    "attribute vec3 aColor;\n" +
                    "varying vec3 vColor;\n" +
                    "varying float vLightIntensity;\n" +
                    "void main() {\n" +
                    "    vec3 worldNormal = normalize((uModelMatrix * vec4(aNormal, 0.0)).xyz);\n" +
                    "    float lambert = max(dot(normalize(-uLightDirection), worldNormal), 0.0);\n" +
                    "    vLightIntensity = max(lambert, 0.2);\n" +
                    "    vColor = aColor;\n" +
                    "    gl_Position = uMVPMatrix * vec4(aPosition, 1.0);\n" +
                    "}";

    private static final String COLOR_FRAGMENT_SHADER =
            "precision mediump float;\n" +
                    "varying vec3 vColor;\n" +
                    "varying float vLightIntensity;\n" +
                    "void main() {\n" +
                    "    gl_FragColor = vec4(vColor * vLightIntensity, 1.0);\n" +
                    "}";
}
