package com.example.racingsim.gl;

import android.opengl.GLES20;
import android.util.Log;

/**
 * Helper that compiles and links OpenGL shader programs.
 */
public class ShaderProgram {

    private static final String TAG = "ShaderProgram";

    private final int programId;

    public ShaderProgram(String vertexShaderSource, String fragmentShaderSource) {
        int vertexShader = compileShader(GLES20.GL_VERTEX_SHADER, vertexShaderSource);
        int fragmentShader = compileShader(GLES20.GL_FRAGMENT_SHADER, fragmentShaderSource);
        programId = linkProgram(vertexShader, fragmentShader);
    }

    public void use() {
        GLES20.glUseProgram(programId);
    }

    public int getProgramId() {
        return programId;
    }

    public int getUniformLocation(String name) {
        int location = GLES20.glGetUniformLocation(programId, name);
        if (location < 0) {
            Log.w(TAG, "Uniform not found: " + name);
        }
        return location;
    }

    public int getAttributeLocation(String name) {
        int location = GLES20.glGetAttribLocation(programId, name);
        if (location < 0) {
            Log.w(TAG, "Attribute not found: " + name);
        }
        return location;
    }

    private static int compileShader(int type, String source) {
        int shader = GLES20.glCreateShader(type);
        if (shader == 0) {
            throw new IllegalStateException("Unable to create shader of type " + type);
        }
        GLES20.glShaderSource(shader, source);
        GLES20.glCompileShader(shader);
        int[] status = new int[1];
        GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, status, 0);
        if (status[0] == 0) {
            String log = GLES20.glGetShaderInfoLog(shader);
            GLES20.glDeleteShader(shader);
            throw new IllegalStateException("Shader compilation failed: " + log);
        }
        return shader;
    }

    private static int linkProgram(int vertexShader, int fragmentShader) {
        int program = GLES20.glCreateProgram();
        if (program == 0) {
            throw new IllegalStateException("Unable to create shader program");
        }
        GLES20.glAttachShader(program, vertexShader);
        GLES20.glAttachShader(program, fragmentShader);
        GLES20.glLinkProgram(program);
        int[] status = new int[1];
        GLES20.glGetProgramiv(program, GLES20.GL_LINK_STATUS, status, 0);
        if (status[0] == 0) {
            String log = GLES20.glGetProgramInfoLog(program);
            GLES20.glDeleteProgram(program);
            throw new IllegalStateException("Program link failed: " + log);
        }
        GLES20.glDeleteShader(vertexShader);
        GLES20.glDeleteShader(fragmentShader);
        return program;
    }
}
