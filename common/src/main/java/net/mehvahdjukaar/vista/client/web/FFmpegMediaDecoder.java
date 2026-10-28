package net.mehvahdjukaar.vista.client.web;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.blaze3d.platform.NativeImage;
import net.mehvahdjukaar.vista.VistaMod;
import net.mehvahdjukaar.vista.client.web.ffmpeg.FFmpeg;
import org.jetbrains.annotations.Nullable;

import java.io.*;
import java.nio.file.Path;

public class FFmpegMediaDecoder {
    private static final double FALLBACK_FPS = 20;
    private static final int BYTES_PER_PIXEL = 3;

    private final FFmpeg ffmpeg;
    private final FFmpegMediaSession session;
    private final Path videoPath;
    private volatile boolean running = true;
    @Nullable
    private volatile Process ffmpegProcess;

    public FFmpegMediaDecoder(FFmpeg ffmpeg, FFmpegMediaSession session, Path videoPath) {
        this.ffmpeg = ffmpeg;
        this.session = session;
        this.videoPath = videoPath;
    }

    public void stop() {
        running = false;
        Process p = ffmpegProcess;
        if (p != null) p.destroyForcibly();
    }

    public void run() {
        try {
            StreamInfo info = probeStreams();
            if (!info.hasVideo()) {
                if (!info.hasAudio()) {
                    VistaMod.LOGGER.warn("No video or audio stream in {}, not playable media", videoPath);
                    session.setFailed(MediaError.BAD_LINK);
                    return;
                }
                VistaMod.LOGGER.info("No video stream in {}, playing audio only", videoPath);
                session.setAudioOnly();
                return;
            }
            int width = info.width(), height = info.height();
            int bytesPerFrame = width * height * BYTES_PER_PIXEL;
            double framerate = info.framerate();
            if (!Double.isFinite(framerate) || framerate <= 0) {
                VistaMod.LOGGER.warn("Invalid probed framerate {}, defaulting to {} fps", framerate, FALLBACK_FPS);
                framerate = FALLBACK_FPS;
            }
            VistaMod.LOGGER.info("Video: {}x{}, {} fps", width, height, framerate);

            Process process = ffmpeg.runFFmpeg("-hide_banner", "-loglevel", "error", "-i", videoPath.toString(),
                    "-f", "image2pipe", "-vcodec", "rawvideo", "-pix_fmt", "rgb24", "-");
            ffmpegProcess = process;
            if (!running) process.destroyForcibly();

            int frameCount = 0;
            try (InputStream in = new BufferedInputStream(process.getInputStream(), 1024 * 1024)) {
                while (running) {
                    byte[] data = in.readNBytes(bytesPerFrame);
                    if (data.length < bytesPerFrame) break;

                    session.addFrame(new MediaFrame(rgbBytesToNativeImage(width, height, data), frameCount / framerate));
                    frameCount++;
                }
            }

            int exitCode = process.waitFor();
            if (!running) return;
            if (exitCode != 0) VistaMod.LOGGER.warn("FFmpeg exited with code {} on {}", exitCode, videoPath);
            if (frameCount == 0) {
                session.setFailed(MediaError.NONE);
                return;
            }
            VistaMod.LOGGER.info("Finished decoding {}, total frames: {}", videoPath, frameCount);
            session.markCompleted();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            VistaMod.LOGGER.error("Failed to decode {}", videoPath, e);
            if (running) session.setFailed(MediaError.classify(e));
        } finally {
            Process p = ffmpegProcess;
            if (p != null) p.destroyForcibly();
        }
    }

    private StreamInfo probeStreams() throws IOException {
        Process p = ffmpeg.runFFprobe("-v", "error", "-show_entries", "stream=codec_type,width,height,r_frame_rate",
                "-of", "json", videoPath.toString());
        JsonElement json;
        try (Reader r = new InputStreamReader(p.getInputStream())) {
            json = JsonParser.parseReader(r);
        }
        int width = 0, height = 0;
        double framerate = FALLBACK_FPS;
        boolean hasAudio = false;
        if (!json.isJsonObject() || !json.getAsJsonObject().has("streams")) {
            return new StreamInfo(width, height, framerate, false);
        }

        for (JsonElement e : json.getAsJsonObject().getAsJsonArray("streams")) {
            JsonObject stream = e.getAsJsonObject();
            String type = stream.has("codec_type") ? stream.get("codec_type").getAsString() : "";
            if (type.equals("audio")) {
                hasAudio = true;
            } else if (type.equals("video") && width == 0 && stream.has("width") && stream.has("height")) {
                width = stream.get("width").getAsInt();
                height = stream.get("height").getAsInt();
                if (stream.has("r_frame_rate")) framerate = parseFrameRate(stream.get("r_frame_rate").getAsString());
            }
        }
        return new StreamInfo(width, height, framerate, hasAudio);
    }

    private static double parseFrameRate(String rate) {
        String[] parts = rate.split("/");
        if (parts.length == 2) return Double.parseDouble(parts[0]) / Double.parseDouble(parts[1]);
        return Double.parseDouble(rate);
    }

    private NativeImage rgbBytesToNativeImage(int width, int height, byte[] data) {
        NativeImage img = new NativeImage(NativeImage.Format.RGBA, width, height, true);
        for (int y = 0, p = 0; y < height; y++) {
            for (int x = 0; x < width; x++, p += BYTES_PER_PIXEL) {
                int r = data[p] & 0xFF;
                int g = data[p + 1] & 0xFF;
                int b = data[p + 2] & 0xFF;
                int abgr = 0xFF000000 | (b << 16) | (g << 8) | r;
                img.setPixelRGBA(x, y, abgr);
            }
        }
        return img;
    }

    private record StreamInfo(int width, int height, double framerate, boolean hasAudio) {
        private boolean hasVideo() {
            return width > 0 && height > 0;
        }
    }
}
