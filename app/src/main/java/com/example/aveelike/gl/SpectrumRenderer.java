package com.example.aveelike.gl;

import android.opengl.GLES20;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

public final class SpectrumRenderer {

    private static final int BAR_COUNT = 64;
    private static final int FLOATS_PER_VERTEX = 6;
    private static final int VERTICES_PER_BAR = 6;
    private static final int TOTAL_FLOATS =
            BAR_COUNT * VERTICES_PER_BAR * FLOATS_PER_VERTEX;

    private static final String VERTEX_SHADER =
            "attribute vec2 aPosition;" +
            "attribute vec4 aColor;" +
            "varying vec4 vColor;" +
            "void main(){gl_Position=vec4(aPosition,0.0,1.0);vColor=aColor;}";

    private static final String FRAGMENT_SHADER =
            "precision mediump float;" +
            "varying vec4 vColor;" +
            "void main(){gl_FragColor=vColor;}";

    private final ShaderProgram shader;
    private final FloatBuffer vertexBuffer;
    private final float[] vertices = new float[TOTAL_FLOATS];

    public SpectrumRenderer() {
        shader = new ShaderProgram(VERTEX_SHADER, FRAGMENT_SHADER);
        ByteBuffer bytes = ByteBuffer.allocateDirect(TOTAL_FLOATS * 4);
        bytes.order(ByteOrder.nativeOrder());
        vertexBuffer = bytes.asFloatBuffer();
    }

    public void draw(float[] spectrum) {
        if (spectrum == null || spectrum.length == 0) {
            return;
        }

        buildVertices(spectrum);

        vertexBuffer.clear();
        vertexBuffer.put(vertices);
        vertexBuffer.position(0);

        shader.use();

        int positionHandle = shader.getAttributeLocation("aPosition");
        int colorHandle = shader.getAttributeLocation("aColor");

        GLES20.glEnableVertexAttribArray(positionHandle);
        GLES20.glEnableVertexAttribArray(colorHandle);

        vertexBuffer.position(0);
        GLES20.glVertexAttribPointer(
                positionHandle, 2, GLES20.GL_FLOAT, false,
                FLOATS_PER_VERTEX * 4, vertexBuffer
        );

        vertexBuffer.position(2);
        GLES20.glVertexAttribPointer(
                colorHandle, 4, GLES20.GL_FLOAT, false,
                FLOATS_PER_VERTEX * 4, vertexBuffer
        );

        GLES20.glDrawArrays(
                GLES20.GL_TRIANGLES, 0, BAR_COUNT * VERTICES_PER_BAR
        );

        GLES20.glDisableVertexAttribArray(positionHandle);
        GLES20.glDisableVertexAttribArray(colorHandle);
    }

    private void buildVertices(float[] spectrum) {
        float gap = 0.006f;
        float barWidth = 2f / BAR_COUNT;
        int offset = 0;

        for (int i = 0; i < BAR_COUNT; i++) {
            float value = spectrum[Math.min(i, spectrum.length - 1)];
            value = Math.max(0f, Math.min(1f, value));

            float left = -1f + i * barWidth + gap;
            float right = -1f + (i + 1) * barWidth - gap;
            float bottom = -0.85f;
            float top = bottom + 0.05f + value * 1.45f;

            float t = i / 63f;
            float red = 0.15f + 0.65f * t;
            float green = 0.85f - 0.35f * t;
            float blue = 1f;

            offset = vertex(offset, left, bottom, red, green, blue, 1f);
            offset = vertex(offset, right, bottom, red, green, blue, 1f);
            offset = vertex(offset, left, top, red, green, blue, 1f);
            offset = vertex(offset, left, top, red, green, blue, 1f);
            offset = vertex(offset, right, bottom, red, green, blue, 1f);
            offset = vertex(offset, right, top, red, green, blue, 1f);
        }
    }

    private int vertex(
            int offset, float x, float y,
            float red, float green, float blue, float alpha
    ) {
        vertices[offset++] = x;
        vertices[offset++] = y;
        vertices[offset++] = red;
        vertices[offset++] = green;
        vertices[offset++] = blue;
        vertices[offset++] = alpha;
        return offset;
    }

    public void release() {
        shader.release();
    }
}
