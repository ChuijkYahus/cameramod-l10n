package net.mehvahdjukaar.vista.client.web;

import net.mehvahdjukaar.vista.VistaMod;
import net.mehvahdjukaar.vista.client.web.ffmpeg.FFmpeg;

import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

public class PcmAudioTrack implements PcmSource {

    private byte[] samples = new byte[BYTES_PER_SECOND * 8];
    private int length = 0;
    private volatile boolean completed = false;
    private volatile Process process;

    public void decodeAsync(FFmpeg ffmpeg, Path videoPath, Executor executor) {
        CompletableFuture.runAsync(() -> decode(ffmpeg, videoPath), executor);
    }

    private void decode(FFmpeg ffmpeg, Path videoPath) {
        try {
            process = ffmpeg.runFFmpeg("-hide_banner", "-loglevel", "error", "-i", videoPath.toString(),
                    "-vn", "-f", "s16le", "-ac", "1", "-ar", String.valueOf(SAMPLE_RATE), "-");
            try (InputStream in = process.getInputStream()) {
                byte[] chunk = new byte[BYTES_PER_SECOND];
                int read;
                while ((read = in.read(chunk)) > 0) append(chunk, read);
            }
            process.waitFor();
        } catch (Exception e) {
            VistaMod.LOGGER.warn("Failed to decode audio of {}", videoPath, e);
        } finally {
            completed = true;
        }
    }

    private synchronized void append(byte[] chunk, int count) {
        if (length + count > samples.length) {
            samples = Arrays.copyOf(samples, Math.max(length + count, samples.length * 2));
        }
        System.arraycopy(chunk, 0, samples, length, count);
        length += count;
    }

    @Override
    public synchronized boolean hasSamples() {
        return length > 0;
    }

    @Override
    public synchronized long readInto(long cursor, ByteBuffer dst) {
        int size = dst.remaining();
        int written = 0;
        while (written < size) {
            if (cursor >= length) {
                if (!completed || length == 0) break;
                cursor %= length;
            }
            int count = (int) Math.min(size - written, length - cursor);
            dst.put(written, samples, (int) cursor, count);
            written += count;
            cursor += count;
        }
        return cursor + (size - written);
    }

    public void stop() {
        Process p = process;
        if (p != null) p.destroyForcibly();
    }
}
