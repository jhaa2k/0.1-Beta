# AveeLike

A minimal Java Android audio visualizer using Media3 ExoPlayer, Android Visualizer FFT capture, and OpenGL ES 2.0.

## Build

The GitHub Actions workflow builds a debug APK on pushes to `main` or when manually dispatched.

Open **Actions → AveeLike Android Build → Run workflow**. Download the `AveeLike-debug-APK` artifact from the completed run.

## Requirements

- Java 17
- Android SDK 34
- Android Gradle Plugin 8.5.2
- Gradle 8.7
- Min SDK 24 / Target SDK 34

## Notes

The system audio picker grants access to the selected file URI. The app requests microphone permission because Android's Visualizer API requires it for FFT capture. Denying that permission does not prevent audio playback.
