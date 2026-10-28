package net.mehvahdjukaar.vista.client.web;

import com.mojang.blaze3d.platform.NativeImage;
import net.mehvahdjukaar.vista.VistaMod;
import net.mehvahdjukaar.vista.client.web.ffmpeg.FFmpeg;
import org.jetbrains.annotations.Nullable;

import java.io.*;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

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

    public void stopDecoder() {
        running = false;
        Process p = ffmpegProcess;
        if (p != null) p.destroyForcibly();
    }

    public void run() {
        try {
            int[] size = probeVideoSize(videoPath.toString());
            if (size == null) {
                if (!hasAudioStream(videoPath.toString())) {
                    VistaMod.LOGGER.warn("No video or audio stream in {}, not playable media", videoPath);
                    session.setFailed(MediaError.BAD_LINK);
                    return;
                }
                VistaMod.LOGGER.info("No video stream in {}, playing audio only", videoPath);
                session.setAudioOnly();
                return;
            }
            int width = size[0], height = size[1];
            int frameSize = width * height * BYTES_PER_PIXEL;
            double framerate = probeFrameRate(videoPath.toString());
            if (!Double.isFinite(framerate) || framerate <= 0) {
                VistaMod.LOGGER.warn("Invalid probed framerate {}, defaulting to {} fps", framerate, FALLBACK_FPS);
                framerate = FALLBACK_FPS;
            }
            VistaMod.LOGGER.info("Video: {}x{}, {} fps", width, height, framerate);

            Process process = ffmpeg.runFFmpeg(buildFFmpegCommand());
            ffmpegProcess = process;
            if (!running) process.destroyForcibly();

            int frameCount = 0;
            try (InputStream in = new BufferedInputStream(process.getInputStream(), 1024 * 1024)) {
                while (running) {
                    byte[] data = in.readNBytes(frameSize);
                    if (data.length < frameSize) break;

                    session.add(new MediaFrame(rgbBytesToNativeImage(width, height, data), frameCount / framerate));
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
            session.setCompleted(true);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            VistaMod.LOGGER.error("Failed to decode {}", videoPath, e);
        } finally {
            Process p = ffmpegProcess;
            if (p != null) p.destroyForcibly();
        }
    }


    private String[] buildFFmpegCommand() {
        List<String> args = new ArrayList<>();
        args.add("-hide_banner");
        args.add("-loglevel");
        args.add("error");
        args.add("-i");
        args.add(videoPath.toString());
        args.add("-f");
        args.add("image2pipe");
        args.add("-vcodec");
        args.add("rawvideo");
        args.add("-pix_fmt");
        args.add("rgb24");
        args.add("-");
        return args.toArray(new String[0]);
    }

    private double probeFrameRate(String path) throws IOException {
        Process p = ffmpeg.runFFprobe("-v", "error", "-select_streams", "v:0",
                "-show_entries", "stream=r_frame_rate", "-of", "csv=p=0", path);
        try (BufferedReader r = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
            String line = r.readLine();
            if (line != null && line.contains("/")) {
                String[] parts = line.trim().split("/");
                double num = Double.parseDouble(parts[0]);
                double den = Double.parseDouble(parts[1]);
                if (den == 0) {
                    return FALLBACK_FPS;
                }
                return num / den;
            } else if (line != null && !line.isEmpty()) {
                return Double.parseDouble(line);
            }
        }
        VistaMod.LOGGER.warn("Could not probe framerate, defaulting to {} fps", FALLBACK_FPS);
        return FALLBACK_FPS;
    }

    private boolean hasAudioStream(String path) throws IOException {
        Process p = ffmpeg.runFFprobe("-v", "error", "-select_streams", "a:0",
                "-show_entries", "stream=codec_type", "-of", "csv=p=0", path);
        try (BufferedReader r = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
            String line = r.readLine();
            return line != null && !line.trim().isEmpty();
        }
    }

    @Nullable
    private int[] probeVideoSize(String path) throws IOException {
        Process p = ffmpeg.runFFprobe("-v", "error", "-select_streams", "v:0",
                "-show_entries", "stream=width,height", "-of", "csv=s=x:p=0", path);
        try (BufferedReader r = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
            String line = r.readLine();
            if (line == null || line.trim().isEmpty()) {
                return null;
            }
            String[] parts = line.trim().split("x");
            return new int[]{Integer.parseInt(parts[0]), Integer.parseInt(parts[1])};
        }
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

}
