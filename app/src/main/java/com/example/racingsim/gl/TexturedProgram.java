package com.example.racingsim.gl;

/**
 * Minimal shader program for textured quads with alpha blending.
 */
public class TexturedProgram extends ShaderProgram {

    private final int mvpLocation;
    private final int textureLocation;
    private final int positionAttribute;
    private final int texCoordAttribute;

    public TexturedProgram() {
        super(VERTEX_SHADER, FRAGMENT_SHADER);
        mvpLocation = getUniformLocation("uMVPMatrix");
        textureLocation = getUniformLocation("uTexture");
        positionAttribute = getAttributeLocation("aPosition");
        texCoordAttribute = getAttributeLocation("aTexCoord");
    }

    public int getMvpLocation() {
        return mvpLocation;
    }

    public int getTextureLocation() {
        return textureLocation;
    }

    public int getPositionAttribute() {
        return positionAttribute;
    }

    public int getTexCoordAttribute() {
        return texCoordAttribute;
    }

    private static final String VERTEX_SHADER =
            "uniform mat4 uMVPMatrix;\n" +
            "attribute vec3 aPosition;\n" +
            "attribute vec2 aTexCoord;\n" +
            "varying vec2 vTexCoord;\n" +
            "void main() {\n" +
            "    vTexCoord = aTexCoord;\n" +
            "    gl_Position = uMVPMatrix * vec4(aPosition, 1.0);\n" +
            "}";

    private static final String FRAGMENT_SHADER =
            "precision mediump float;\n" +
            "uniform sampler2D uTexture;\n" +
            "varying vec2 vTexCoord;\n" +
            "void main() {\n" +
            "    vec4 color = texture2D(uTexture, vTexCoord);\n" +
            "    if (color.a < 0.01) {\n" +
            "        discard;\n" +
            "    }\n" +
            "    gl_FragColor = color;\n" +
            "}";
}
