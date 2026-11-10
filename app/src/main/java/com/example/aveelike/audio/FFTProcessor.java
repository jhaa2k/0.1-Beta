package com.example.aveelike.audio;

public final class FFTProcessor {

    private FFTProcessor() {
    }

    public static float[] process(byte[] fft, int barCount) {
        float[] output = new float[Math.max(0, barCount)];
        if (fft == null || fft.length < 8 || barCount <= 0) {
            return output;
        }

        // Android Visualizer FFT data stores real/imaginary pairs.
        // Bin zero is DC; skip it to avoid a dominant DC component.
        int maxBinExclusive = fft.length / 2;
        int minBin = 1;
        if (maxBinExclusive <= minBin) {
            return output;
        }

        double ratio = Math.pow(
                (double) maxBinExclusive / minBin,
                1.0 / barCount
        );

        for (int bar = 0; bar < barCount; bar++) {
            int start = (int) Math.floor(minBin * Math.pow(ratio, bar));
            int end = (int) Math.floor(minBin * Math.pow(ratio, bar + 1));

            start = Math.max(minBin, start);
            end = Math.max(start + 1, end);
            end = Math.min(maxBinExclusive, end);

            float sum = 0f;
            int count = 0;

            for (int bin = start; bin < end; bin++) {
                int realIndex = bin * 2;
                int imaginaryIndex = realIndex + 1;
                if (imaginaryIndex >= fft.length) {
                    break;
                }

                float real = fft[realIndex];
                float imaginary = fft[imaginaryIndex];
                float magnitude = (float) Math.sqrt(
                        real * real + imaginary * imaginary
                );
                sum += magnitude;
                count++;
            }

            if (count > 0) {
                float average = sum / count;
                // Gentle compression makes quieter frequency bands visible.
                output[bar] = Math.min(1f, (float) Math.sqrt(average / 128f) * 1.5f);
            }
        }

        return output;
    }
}
