import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import javax.sound.sampled.*;

public class AudioPlayer {

    private Clip clip;
    private volatile boolean lofiEnabled = false;
    private File currentFile;
    private int volume = 50;

    public synchronized void playMP3(File file) {

        stop();
        currentFile = file;

        try {
            AudioInputStream audioStream =
                    AudioSystem.getAudioInputStream(file);

            AudioFormat baseFormat = audioStream.getFormat();

            AudioFormat decodedFormat = new AudioFormat(
                    AudioFormat.Encoding.PCM_SIGNED,
                    baseFormat.getSampleRate(),
                    16,
                    baseFormat.getChannels(),
                    baseFormat.getChannels() * 2,
                    baseFormat.getSampleRate(),
                    false
            );

            AudioInputStream decodedStream =
                    AudioSystem.getAudioInputStream(
                            decodedFormat, audioStream);

            byte[] audioData = decodedStream.readAllBytes();

            // Apply slowdown and effects ONLY when LOFI is enabled.
            boolean useLofi = lofiEnabled;

            if (useLofi) {
                audioData = applyLofiEffect(audioData, decodedFormat);
            }

            ByteArrayInputStream byteStream =
                    new ByteArrayInputStream(audioData);

            // Normal speed when LOFI is OFF.
            // LOFI audio is already slowed by applyLofiEffect().
            AudioFormat playbackFormat = decodedFormat;

            AudioInputStream playbackStream =
                    new AudioInputStream(
                            byteStream,
                            playbackFormat,
                            audioData.length / playbackFormat.getFrameSize()
                    );

            clip = AudioSystem.getClip();
            clip.open(playbackStream);

            applyVolume();

            playbackStream.close();
            decodedStream.close();
            audioStream.close();

            clip.start();

        } catch (UnsupportedAudioFileException |
                 IOException |
                 LineUnavailableException e) {

            System.err.println("Playback error: " + e.getMessage());
        }
    }

    public synchronized void pause() {
        if (clip != null && clip.isRunning()) {
            clip.stop();
        }
    }

    public synchronized void resume() {
        if (clip != null && !clip.isRunning()
                && clip.getFramePosition() < clip.getFrameLength()) {
            clip.start();
        }
    }

    public synchronized void stop() {
        if (clip != null) {
            clip.stop();
            clip.close();
            clip = null;
        }
    }

    public synchronized void setVolume(int volume) {
        this.volume = Math.max(0, Math.min(100, volume));
        applyVolume();
    }

    // Get current song position in milliseconds
public synchronized long getPosition() {
    if (clip != null && clip.isOpen()) {
        return clip.getMicrosecondPosition() / 1000;
    }
    return 0;
}

// Get total song duration in milliseconds
public synchronized long getDuration() {
    if (clip != null && clip.isOpen()) {
        return clip.getMicrosecondLength() / 1000;
    }
    return 0;
}

// Move to a selected position in the song
public synchronized void seekTo(long milliseconds) {
    if (clip != null && clip.isOpen()) {
        long position = Math.max(
            0,
            Math.min(milliseconds, clip.getMicrosecondLength() / 1000)
        );

        clip.setMicrosecondPosition(position * 1000);
    }
}

    private void applyVolume() {
        if (clip != null && clip.isOpen()
                && clip.isControlSupported(FloatControl.Type.MASTER_GAIN)) {

            FloatControl gainControl =
                    (FloatControl) clip.getControl(
                            FloatControl.Type.MASTER_GAIN);

            float dB;

            if (volume == 0) {
                dB = gainControl.getMinimum();
            } else {
                dB = (float) (20 * Math.log10(volume / 100.0));

                dB = Math.max(
                        gainControl.getMinimum(),
                        Math.min(gainControl.getMaximum(), dB)
                );
            }

            gainControl.setValue(dB);
        }
    }

    public synchronized void setLofiEnabled(boolean enabled) {
        lofiEnabled = enabled;
    }

    public synchronized boolean isLofiEnabled() {
        return lofiEnabled;
    }

    private byte[] applyLofiEffect(
            byte[] audioData, AudioFormat format) {

        if (format.getSampleSizeInBits() != 16
                || format.getEncoding()
                != AudioFormat.Encoding.PCM_SIGNED) {
            return audioData;
        }

        int channels = format.getChannels();
        int frameSize = format.getFrameSize();
        boolean bigEndian = format.isBigEndian();

        if (channels < 1 || frameSize < channels * 2) {
            return audioData;
        }

        int inputFrames = audioData.length / frameSize;

        if (inputFrames == 0) {
            return audioData;
        }

        // LOFI tuning
        double slowFactor = 0.95;   // Four times the duration
        double lowPass = 0.20;
        double reverbMix = 0.20;
        double feedback = 0.25;
        double stereoWidth = 1.20;
        double warmth = 1.05;

        int[] delayMs = {20, 30, 40, 50, 60, 70};

        int[] delays = new int[delayMs.length];
        int maxDelay = 1;

        for (int i = 0; i < delayMs.length; i++) {
            delays[i] = Math.max(
                    1,
                    (int) (
                            format.getSampleRate()
                                    * delayMs[i] / 1000.0
                    )
            );

            maxDelay = Math.max(maxDelay, delays[i]);
        }

        int outputFrames = (int) Math.ceil(inputFrames / slowFactor);

        if ((long) outputFrames * frameSize > Integer.MAX_VALUE) {
            throw new IllegalArgumentException(
                    "This song is too large to process in memory."
            );
        }

        byte[] output = new byte[outputFrames * frameSize];

        double[][] reverbBuffers =
                new double[channels][maxDelay + 1];

        int[] reverbPositions = new int[channels];
        double[] previousFiltered = new double[channels];

        for (int frame = 0; frame < outputFrames; frame++) {

            double sourcePosition = frame * slowFactor;

            int sourceFrame = Math.min(
                    (int) sourcePosition,
                    inputFrames - 1
            );

            int nextFrame = Math.min(
                    sourceFrame + 1,
                    inputFrames - 1
            );

            double fraction = sourcePosition - sourceFrame;

            double[] filtered = new double[channels];
            double[] wet = new double[channels];

            // Interpolate and smooth the source audio
            for (int ch = 0; ch < channels; ch++) {

                int index1 = sourceFrame * frameSize + ch * 2;
                int index2 = nextFrame * frameSize + ch * 2;

                int sample1;
                int sample2;

                if (bigEndian) {
                    sample1 = ((audioData[index1] & 0xFF) << 8)
                            | (audioData[index1 + 1] & 0xFF);

                    sample2 = ((audioData[index2] & 0xFF) << 8)
                            | (audioData[index2 + 1] & 0xFF);
                } else {
                    sample1 = ((audioData[index1 + 1] & 0xFF) << 8)
                            | (audioData[index1] & 0xFF);

                    sample2 = ((audioData[index2 + 1] & 0xFF) << 8)
                            | (audioData[index2] & 0xFF);
                }

                double input1 = ((short) sample1) / 32768.0;
                double input2 = ((short) sample2) / 32768.0;

                double input = input1 + (input2 - input1) * fraction;

                filtered[ch] = previousFiltered[ch]
                        + lowPass * (input - previousFiltered[ch]);

                previousFiltered[ch] = filtered[ch];
            }

            // Read reverb reflections
            for (int ch = 0; ch < channels; ch++) {

                double[] buffer = reverbBuffers[ch];
                int pos = reverbPositions[ch];

                double sum = 0.0;

                for (int delay : delays) {
                    int readPos = Math.floorMod(
                            pos - delay,
                            buffer.length
                    );

                    sum += buffer[readPos];
                }

                wet[ch] = sum / delays.length;
            }

            // Widen stereo without altering mono files
            double left = filtered[0];
            double right = channels >= 2 ? filtered[1] : filtered[0];

            double mid = (left + right) * 0.5;
            double side = (left - right) * 0.5;

            double wideLeft = mid + side * stereoWidth;
            double wideRight = mid - side * stereoWidth;

            for (int ch = 0; ch < channels; ch++) {

                double dry = filtered[ch];

                if (channels >= 2) {
                    dry = (ch == 0) ? wideLeft : wideRight;
                }

                double result = dry * (1.0 - reverbMix)
                        + wet[ch] * reverbMix;

                result = Math.tanh(result * warmth)
                        / Math.tanh(warmth);

                int finalSample = (int) Math.round(
                        result * 32767.0
                );

                finalSample = Math.max(
                        -32768,
                        Math.min(32767, finalSample)
                );

                int outputIndex = frame * frameSize + ch * 2;

                if (bigEndian) {
                    output[outputIndex] = (byte) (finalSample >> 8);
                    output[outputIndex + 1] = (byte) finalSample;
                } else {
                    output[outputIndex] = (byte) finalSample;
                    output[outputIndex + 1] = (byte) (finalSample >> 8);
                }

                int pos = reverbPositions[ch];

                reverbBuffers[ch][pos] =
                        filtered[ch] * 0.30 + wet[ch] * feedback;

                reverbPositions[ch] =
                        (pos + 1) % reverbBuffers[ch].length;
            }
        }

        return output;
    }
}