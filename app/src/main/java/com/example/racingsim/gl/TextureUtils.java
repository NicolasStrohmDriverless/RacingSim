package com.example.racingsim.gl;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.opengl.GLES20;
import android.opengl.GLUtils;

/**
 * Utility for loading bitmaps into OpenGL textures.
 */
public final class TextureUtils {

    private TextureUtils() {
    }

    public static int loadTexture(Context context, int resourceId) {
        int[] textureHandles = new int[1];
        GLES20.glGenTextures(1, textureHandles, 0);
        if (textureHandles[0] == 0) {
            throw new IllegalStateException("Failed to generate texture handle");
        }

        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inScaled = false;
        Bitmap bitmap = BitmapFactory.decodeResource(context.getResources(), resourceId, options);
        if (bitmap == null) {
            GLES20.glDeleteTextures(1, textureHandles, 0);
            throw new IllegalStateException("Failed to decode texture resource");
        }

        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureHandles[0]);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);
        GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bitmap, 0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);
        bitmap.recycle();
        return textureHandles[0];
    }
}
