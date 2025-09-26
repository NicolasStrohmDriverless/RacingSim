package com.example.racingsim.gl;

import android.opengl.GLES20;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;

/**
 * Stores interleaved vertex buffers together with their attribute layout.
 */
public class Mesh {

    private final int vertexBufferId;
    private final int indexBufferId;
    private final int vertexCount;
    private final int indexCount;
    private final int[] attributeSizes;
    private final int vertexStrideBytes;

    public Mesh(float[] vertexData, short[] indexData, int[] attributeSizes) {
        this.attributeSizes = attributeSizes.clone();
        this.vertexStrideBytes = calculateStride(attributeSizes);
        this.vertexCount = vertexData.length / componentsPerVertex(attributeSizes);
        this.indexCount = indexData != null ? indexData.length : 0;

        FloatBuffer vertexBuffer = ByteBuffer
                .allocateDirect(vertexData.length * 4)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer();
        vertexBuffer.put(vertexData).position(0);

        int[] buffers = new int[1];
        GLES20.glGenBuffers(1, buffers, 0);
        vertexBufferId = buffers[0];
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, vertexBufferId);
        GLES20.glBufferData(GLES20.GL_ARRAY_BUFFER, vertexBuffer.capacity() * 4, vertexBuffer, GLES20.GL_STATIC_DRAW);
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0);

        if (indexCount > 0) {
            ShortBuffer indexBuffer = ByteBuffer
                    .allocateDirect(indexCount * 2)
                    .order(ByteOrder.nativeOrder())
                    .asShortBuffer();
            indexBuffer.put(indexData).position(0);
            GLES20.glGenBuffers(1, buffers, 0);
            indexBufferId = buffers[0];
            GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, indexBufferId);
            GLES20.glBufferData(GLES20.GL_ELEMENT_ARRAY_BUFFER, indexBuffer.capacity() * 2, indexBuffer, GLES20.GL_STATIC_DRAW);
            GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, 0);
        } else {
            indexBufferId = 0;
        }
    }

    private static int componentsPerVertex(int[] attributeSizes) {
        int total = 0;
        for (int size : attributeSizes) {
            total += size;
        }
        return total;
    }

    private static int calculateStride(int[] attributeSizes) {
        return componentsPerVertex(attributeSizes) * 4;
    }

    public void draw(int primitiveType, int[] attributeLocations) {
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, vertexBufferId);
        int offset = 0;
        for (int i = 0; i < attributeLocations.length; i++) {
            int location = attributeLocations[i];
            if (location >= 0) {
                GLES20.glEnableVertexAttribArray(location);
                GLES20.glVertexAttribPointer(location, attributeSizes[i], GLES20.GL_FLOAT, false, vertexStrideBytes, offset);
            }
            offset += attributeSizes[i] * 4;
        }

        if (indexCount > 0) {
            GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, indexBufferId);
            GLES20.glDrawElements(primitiveType, indexCount, GLES20.GL_UNSIGNED_SHORT, 0);
            GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, 0);
        } else {
            GLES20.glDrawArrays(primitiveType, 0, vertexCount);
        }

        for (int location : attributeLocations) {
            if (location >= 0) {
                GLES20.glDisableVertexAttribArray(location);
            }
        }
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0);
    }
}
