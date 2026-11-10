package com.example.aveelike;

import android.Manifest;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import android.opengl.GLSurfaceView;

import com.example.aveelike.audio.AudioEngine;
import com.example.aveelike.gl.GLSurfaceRenderer;

public class MainActivity extends AppCompatActivity {

    private GLSurfaceView glSurfaceView;
    private GLSurfaceRenderer glRenderer;
    private AudioEngine audioEngine;

    private Button btnSelect;
    private Button btnPlayPause;
    private TextView txtStatus;

    private ActivityResultLauncher<String> audioPicker;
    private ActivityResultLauncher<String> permissionLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        glSurfaceView = findViewById(R.id.glSurfaceView);
        btnSelect = findViewById(R.id.btnSelect);
        btnPlayPause = findViewById(R.id.btnPlayPause);
        txtStatus = findViewById(R.id.txtStatus);

        setupOpenGL();
        setupAudioPicker();
        setupPermissionLauncher();

        audioEngine = new AudioEngine(this);
        audioEngine.setSpectrumListener(spectrum -> {
            GLSurfaceRenderer renderer = glRenderer;
            GLSurfaceView surface = glSurfaceView;
            if (renderer != null && surface != null) {
                renderer.updateSpectrum(spectrum);
            }
        });

        btnSelect.setOnClickListener(view -> audioPicker.launch("audio/*"));
        btnPlayPause.setOnClickListener(view -> togglePlayback());

        requestVisualizerPermissionIfNeeded();
    }

    private void setupOpenGL() {
        glSurfaceView.setEGLContextClientVersion(2);
        glRenderer = new GLSurfaceRenderer();
        glSurfaceView.setRenderer(glRenderer);
        // Continuous rendering keeps the spectrum smooth between FFT callbacks.
        glSurfaceView.setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY);
    }

    private void setupAudioPicker() {
        audioPicker = registerForActivityResult(
                new ActivityResultContracts.GetContent(),
                this::onAudioSelected
        );
    }

    private void onAudioSelected(Uri uri) {
        if (uri == null) {
            return;
        }
        audioEngine.loadAudio(uri);
        txtStatus.setText("Loading audio...");
        btnPlayPause.setText("PAUSE");
    }

    private void setupPermissionLauncher() {
        permissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                granted -> {
                    audioEngine.setRecordAudioPermissionGranted(granted);
                    if (granted) {
                        txtStatus.setText("Visualizer permission granted");
                    } else {
                        txtStatus.setText("Playback works; spectrum needs microphone permission");
                    }
                }
        );
    }

    private void requestVisualizerPermissionIfNeeded() {
        boolean granted = ContextCompat.checkSelfPermission(
                this, Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED;

        audioEngine.setRecordAudioPermissionGranted(granted);
        if (!granted) {
            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO);
        }
    }

    private void togglePlayback() {
        if (audioEngine == null || !audioEngine.hasAudio()) {
            Toast.makeText(this, "Select an audio file first", Toast.LENGTH_SHORT).show();
            return;
        }

        audioEngine.togglePlayback();
        if (audioEngine.isPlaying()) {
            btnPlayPause.setText("PAUSE");
            txtStatus.setText("Playing");
        } else {
            btnPlayPause.setText("PLAY");
            txtStatus.setText("Paused");
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (glSurfaceView != null) {
            glSurfaceView.onResume();
        }
    }

    @Override
    protected void onPause() {
        if (glSurfaceView != null) {
            glSurfaceView.onPause();
        }
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        if (audioEngine != null) {
            audioEngine.release();
            audioEngine = null;
        }
        super.onDestroy();
    }
}
