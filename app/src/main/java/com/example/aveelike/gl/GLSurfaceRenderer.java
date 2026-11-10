package com.example.aveelike.gl;

import android.opengl.GLES20;
import android.opengl.GLSurfaceView;

import java.util.Arrays;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

public final class GLSurfaceRenderer implements GLSurfaceView.Renderer {

    private SpectrumRenderer spectrumRenderer;
    private volatile float[] spectrum = new float[64];

    public void updateSpectrum(float[] newSpectrum) {
        if (newSpectrum != null) {
            spectrum = Arrays.copyOf(newSpectrum, newSpectrum.length);
        }
    }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        GLES20.glClearColor(0.035f, 0.035f, 0.045f, 1f);
        GLES20.glDisable(GLES20.GL_DEPTH_TEST);
        spectrumRenderer = new SpectrumRenderer();
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        GLES20.glViewport(0, 0, width, height);
    }

    @Override
    public void onDrawFrame(GL10 gl) {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);
        SpectrumRenderer renderer = spectrumRenderer;
        if (renderer != null) {
            renderer.draw(spectrum);
        }
    }
}
