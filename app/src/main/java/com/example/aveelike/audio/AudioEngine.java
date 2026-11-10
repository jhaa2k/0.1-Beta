package com.example.aveelike.audio;

import android.content.Context;
import android.media.audiofx.Visualizer;
import android.net.Uri;
import android.util.Log;

import androidx.media3.common.MediaItem;
import androidx.media3.common.PlaybackException;
import androidx.media3.common.Player;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.exoplayer.analytics.AnalyticsListener;

public class AudioEngine {

    private static final String TAG = "AveeLikeAudio";

    private final Context context;
    private ExoPlayer player;
    private Visualizer visualizer;

    private volatile boolean recordAudioPermissionGranted;
    private volatile SpectrumListener spectrumListener;
    private volatile int currentAudioSessionId = -1;

    public interface SpectrumListener {
        void onSpectrum(float[] spectrum);
    }

    public AudioEngine(Context context) {
        this.context = context.getApplicationContext();
        initializePlayer();
    }

    private void initializePlayer() {
        player = new ExoPlayer.Builder(context).build();

        player.addAnalyticsListener(new AnalyticsListener() {
            @Override
            public void onAudioSessionIdChanged(
                    AnalyticsListener.EventTime eventTime,
                    int audioSessionId
            ) {
                currentAudioSessionId = audioSessionId;
                attachVisualizer();
            }
        });

        player.addListener(new Player.Listener() {
            @Override
            public void onPlaybackStateChanged(int playbackState) {
                if (playbackState == Player.STATE_READY) {
                    Log.d(TAG, "Player ready");
                } else if (playbackState == Player.STATE_ENDED) {
                    Log.d(TAG, "Playback ended");
                }
            }

            @Override
            public void onPlayerError(PlaybackException error) {
                Log.e(TAG, "Playback error", error);
            }
        });
    }

    public void setSpectrumListener(SpectrumListener listener) {
        spectrumListener = listener;
    }

    public void setRecordAudioPermissionGranted(boolean granted) {
        recordAudioPermissionGranted = granted;
        if (granted) {
            attachVisualizer();
        } else {
            releaseVisualizer();
        }
    }

    public void loadAudio(Uri uri) {
        if (player == null || uri == null) {
            return;
        }

        releaseVisualizer();
        player.setMediaItem(MediaItem.fromUri(uri));
        player.prepare();
        player.play();

        // If the player reuses its current session, attach immediately.
        attachVisualizer();
    }

    public void togglePlayback() {
        if (player == null) {
            return;
        }
        if (player.isPlaying()) {
            player.pause();
        } else {
            player.play();
        }
    }

    public boolean isPlaying() {
        return player != null && player.isPlaying();
    }

    public boolean hasAudio() {
        return player != null && player.getMediaItemCount() > 0;
    }

    private synchronized void attachVisualizer() {
        if (!recordAudioPermissionGranted || player == null) {
            return;
        }

        int sessionId = currentAudioSessionId;
        if (sessionId <= 0) {
            return;
        }

        releaseVisualizer();

        try {
            int[] range = Visualizer.getCaptureSizeRange();
            int captureSize = range[1];

            Visualizer candidate = new Visualizer(sessionId);
            candidate.setCaptureSize(captureSize);
            candidate.setScalingMode(Visualizer.SCALING_MODE_NORMALIZED);

            int captureRate = Visualizer.getMaxCaptureRate() / 2;
            candidate.setDataCaptureListener(
                    new Visualizer.OnDataCaptureListener() {
                        @Override
                        public void onWaveFormDataCapture(
                                Visualizer effect, byte[] waveform, int samplingRate) {
                            // FFT capture is used by this app.
                        }

                        @Override
                        public void onFftDataCapture(
                                Visualizer effect, byte[] fft, int samplingRate) {
                            float[] bars = FFTProcessor.process(fft, 64);
                            SpectrumListener listener = spectrumListener;
                            if (listener != null) {
                                listener.onSpectrum(bars);
                            }
                        }
                    },
                    captureRate,
                    false,
                    true
            );

            candidate.setEnabled(true);
            visualizer = candidate;
            Log.d(TAG, "Visualizer attached to audio session " + sessionId);
        } catch (Exception error) {
            Log.e(TAG, "Could not attach Visualizer", error);
            releaseVisualizer();
        }
    }

    private synchronized void releaseVisualizer() {
        if (visualizer == null) {
            return;
        }

        try {
            visualizer.setEnabled(false);
            visualizer.setDataCaptureListener(null, 0, false, false);
            visualizer.release();
        } catch (Exception error) {
            Log.w(TAG, "Error releasing Visualizer", error);
        } finally {
            visualizer = null;
        }
    }

    public void release() {
        releaseVisualizer();
        if (player != null) {
            player.release();
            player = null;
        }
    }
}
