package net.mehvahdjukaar.vista.client.web;

import com.mojang.blaze3d.platform.NativeImage;
import net.mehvahdjukaar.vista.VistaMod;
import net.mehvahdjukaar.vista.client.textures.FFmpegWebTexture;
import net.mehvahdjukaar.vista.client.textures.ImageRescaler;
import net.mehvahdjukaar.vista.client.web.ffmpeg.FFmpeg;
import net.mehvahdjukaar.vista.configs.ClientConfigs;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

public class FFmpegMediaSession implements IMediaSession {
    private final List<MediaFrame> frames = new ArrayList<>();
    private final CompletableFuture<Void> loadFuture;

    @Nullable
    private volatile FFmpegMediaDecoder decoder;
    private volatile boolean completed;
    private volatile boolean failed;
    private volatile boolean closed;

    private final int targetWidth;
    private final int targetHeight;

    public FFmpegMediaSession(String url, @Nullable FFmpeg ffmpeg, MediaCacheManager cacheManager,
                              Executor executor,
                              int targetWidth, int targetHeight) {
        this.targetWidth = targetWidth;
        this.targetHeight = targetHeight;
        this.loadFuture = CompletableFuture.runAsync(() -> asyncThreadJob(url, ffmpeg, cacheManager), executor);
    }

    private void asyncThreadJob(String url, @Nullable FFmpeg ffmpeg, MediaCacheManager cacheManager) {
        try {
            if (ffmpeg == null) {
                this.failed = true;
                return;
            }
            Path videoPath = cacheManager.getOrDownload(url);
            if (closed) return;
            FFmpegMediaDecoder newDecoder = new FFmpegMediaDecoder(ffmpeg, this, videoPath);
            this.decoder = newDecoder;
            newDecoder.run(); //blocking until done
        } catch (Exception e) {
            failed = true;
            VistaMod.LOGGER.error("Failed to load web video {}", url, e);
        }
    }

    public synchronized void add(MediaFrame frame) {
        NativeImage originalImg = frame.image();
        NativeImage scaledImg = ImageRescaler.resize(originalImg, targetWidth, targetHeight,
                ClientConfigs.SCALING_MODE.get(), ClientConfigs.BILINEAR.get());
        if (originalImg != scaledImg) originalImg.close();
        frames.add(new MediaFrame(scaledImg, frame.pts()));
    }

    public synchronized void setCompleted(boolean completed) {
        this.completed = completed;
    }

    public synchronized boolean isCompleted() {
        return completed;
    }

    public synchronized int size() {
        return frames.size();
    }

    public boolean isReady() {
        return size() > 0;
    }

    private boolean isFailed() {
        return failed || loadFuture.isCompletedExceptionally();
    }

    public MediaStatus.Pair lookupFrame(double seconds) {
        synchronized (this) {
            if (frames.isEmpty()) {
                if (failed) return MediaStatus.pair(MediaStatus.FAILED, null);
                return MediaStatus.loading();
            }
            MediaStatus state = MediaStatus.READY;
            if (isFailed()) {
                state = MediaStatus.FAILED;
            } else if (closed) {
                state = MediaStatus.CLOSED;
            }

            double queryTime = seconds;
            double duration = getDurationSecondsLocked();

            if (completed && duration > 0 && queryTime >= duration) {
                queryTime = queryTime % duration;
                state = MediaStatus.COMPLETE;
            } else if (!completed && queryTime > getLastFrameTimeLocked()) {
                queryTime = getLastFrameTimeLocked();
                state = MediaStatus.BUFFERING;
            }

            return MediaStatus.pair(state, getFrameAtTimeLocked(queryTime));
        }
    }

    private double getLastFrameTimeLocked() {
        return frames.isEmpty() ? 0 : frames.getLast().pts();
    }

    private double getDurationSecondsLocked() {
        if (frames.size() < 2) return 0;
        double lastPts = frames.getLast().pts();
        double previousPts = frames.get(frames.size() - 2).pts();
        double frameDuration = Math.max(0, lastPts - previousPts);
        return lastPts + frameDuration;
    }

    @Nullable
    private MediaFrame getFrameAtTimeLocked(double time) {
        if (frames.isEmpty()) return null;
        int idx = Collections.binarySearch(frames, new MediaFrame(null, time),
                Comparator.comparingDouble(MediaFrame::pts));
        if (idx >= 0) {
            return frames.get(idx);
        }
        int insertionPoint = -idx - 1;
        if (insertionPoint == 0) {
            return frames.getFirst();
        }
        return frames.get(insertionPoint - 1);
    }

    @Override
    public synchronized void close() {
        closed = true;
        FFmpegMediaDecoder currentDecoder = decoder;
        if (currentDecoder != null) {
            currentDecoder.stopDecoder();
        }
        for (MediaFrame frame : frames) {
            try {
                frame.close();
            } catch (Exception ignored) {
            }
        }
        frames.clear();
        completed = false;
    }

    @Override
    public FFmpegWebTexture createTextureView(ResourceLocation resourceLocation) {

        return new FFmpegWebTexture(resourceLocation, this, targetWidth, targetHeight);
    }
}
