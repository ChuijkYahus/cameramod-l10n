package net.mehvahdjukaar.vista.client.web;

import net.mehvahdjukaar.vista.VistaMod;
import net.mehvahdjukaar.vista.client.web.ffmpeg.FFmpegManager;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

public class MediaSession implements AutoCloseable {
    private final List<MediaFrame> frames = new ArrayList<>();
    private final CompletableFuture<Void> loadFuture;

    @Nullable
    private volatile FFmpegMediaDecoder decoder;
    private volatile boolean completed;
    private volatile boolean failed;
    private volatile boolean closed;

    public MediaSession(String url, FFmpegManager ffmpeg, MediaCacheManager cacheManager, Executor executor) {
        this.loadFuture = CompletableFuture.runAsync(() -> load(url, ffmpeg, cacheManager), executor);
    }

    private void load(String url, FFmpegManager ffmpeg,  MediaCacheManager cacheManager) {
        try {
            Path videoPath = cacheManager.getOrDownload(url);
            if (closed) return;
            FFmpegMediaDecoder newDecoder = new FFmpegMediaDecoder(ffmpeg, this, videoPath);
            this.decoder = newDecoder;
            newDecoder.start();
        } catch (Exception e) {
            failed = true;
            VistaMod.LOGGER.error("Failed to load web video {}", url, e);
        }
    }

    public synchronized void add(MediaFrame frame) {
        frames.add(frame);
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

    public boolean isFailed() {
        return failed || loadFuture.isCompletedExceptionally();
    }

    public FrameLookup lookupFrame(double seconds) {
        synchronized (this) {
            if (closed) return new FrameLookup(null, FrameLookup.State.CLOSED);
            if (isFailed()) return new FrameLookup(null, FrameLookup.State.FAILED);
            if (frames.isEmpty()) return new FrameLookup(null, FrameLookup.State.LOADING);

            double queryTime = seconds;
            FrameLookup.State state = FrameLookup.State.READY;
            double duration = getDurationSecondsLocked();

            if (completed && duration > 0 && queryTime >= duration) {
                queryTime = queryTime % duration;
                state = FrameLookup.State.COMPLETE;
            } else if (!completed && queryTime > getLastFrameTimeLocked()) {
                state = FrameLookup.State.BUFFERING;
                queryTime = getLastFrameTimeLocked();
            }

            return new FrameLookup(getFrameAtTimeLocked(queryTime), state);
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
            if (frame.image() != null) {
                frame.image().close();
            }
        }
        frames.clear();
        completed = false;
    }
}
