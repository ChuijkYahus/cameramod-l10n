package net.mehvahdjukaar.vista.client.web.audio;

import net.mehvahdjukaar.vista.VistaMod;
import net.mehvahdjukaar.vista.client.web.ffmpeg.FFmpeg;

import javax.sound.sampled.AudioFormat;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

public class FFmpegAudioTrack {

    //probably enough
    private static final int SAMPLE_RATE = 24000;
    private static final AudioFormat FORMAT = new AudioFormat(SAMPLE_RATE, 16, 1, true, false);
    private static final int BYTES_PER_SECOND = SAMPLE_RATE * FORMAT.getFrameSize();

    private byte[] pcm = new byte[BYTES_PER_SECOND * 8];
    private int length = 0;
    private volatile boolean completed = false;
    private volatile Process ffmpegProcess;

    public AudioFormat getFormat() {
        return FORMAT;
    }

    public long secondsToBytes(double seconds) {
        return Math.max(0, (long) (seconds * SAMPLE_RATE)) * FORMAT.getFrameSize();
    }

    public void decodeAsync(FFmpeg ffmpeg, Path videoPath, Executor executor) {
        CompletableFuture.runAsync(() -> decode(ffmpeg, videoPath), executor);
    }

    private void decode(FFmpeg ffmpeg, Path videoPath) {
        try {
            ffmpegProcess = ffmpeg.runFFmpeg("-hide_banner", "-loglevel", "error", "-i", videoPath.toString(),
                    "-vn", "-f", "s16le", "-ac", "1", "-ar", String.valueOf(SAMPLE_RATE), "-");
            try (InputStream in = ffmpegProcess.getInputStream()) {
                byte[] chunk = new byte[BYTES_PER_SECOND];
                int read;
                while ((read = in.read(chunk)) > 0) {
                    append(chunk, read);
                }
            }
            ffmpegProcess.waitFor();
            completed = true;
        } catch (Exception e) {
            VistaMod.LOGGER.warn("Failed to decode audio of {}", videoPath, e);
        }
    }

    private synchronized void append(byte[] chunk, int count) {
        if (length + count > pcm.length) {
            pcm = Arrays.copyOf(pcm, Math.max(length + count, pcm.length * 2));
        }
        System.arraycopy(chunk, 0, pcm, length, count);
        length += count;
    }

    public synchronized boolean isEmpty() {
        return length <= 0;
    }

    public synchronized double getDurationSeconds() {
        return completed ? length / (double) BYTES_PER_SECOND : 0;
    }

    // the cursor always moves by the full buffer. past what we decoded its just silence
    public synchronized long readInto(long cursor, ByteBuffer destination) {
        long count = Math.min(destination.remaining(), length - cursor);
        if (count > 0) destination.put(0, pcm, (int) cursor, (int) count);
        return cursor + destination.remaining();
    }

    public void stopDecoding() {
        Process p = ffmpegProcess;
        if (p != null) p.destroyForcibly();
    }
}
