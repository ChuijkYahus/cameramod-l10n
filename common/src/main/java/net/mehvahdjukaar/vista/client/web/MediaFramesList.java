package net.mehvahdjukaar.vista.client.web;

import java.util.*;

/**
 * Thread‑safe container for decoded video frames.
 * Frames are stored in order of increasing PTS.
 * Supports looking up the closest frame ≤ a given timestamp.
 */
public class MediaFramesList implements AutoCloseable {
    private final List<MediaFrame> frames = new ArrayList<>();
    private boolean completed = false;

    /**
     * Adds a frame (assumed to be in PTS order).
     */
    public synchronized void add(MediaFrame frame) {
        frames.add(frame);
    }

    public synchronized void setCompleted(boolean completed) {
        this.completed = completed;
    }

    public synchronized boolean isCompleted() {
        return completed;
    }

    /**
     * Returns the total number of frames received so far.
     */
    public synchronized int size() {
        return frames.size();
    }

    public synchronized double getLastFrameTime() {
        return frames.isEmpty() ? 0 : frames.get(frames.size() - 1).pts();
    }

    public synchronized double getDurationSeconds() {
        if (frames.size() < 2) return 0;
        double lastPts = frames.get(frames.size() - 1).pts();
        double previousPts = frames.get(frames.size() - 2).pts();
        double frameDuration = Math.max(0, lastPts - previousPts);
        return lastPts + frameDuration;
    }

    public synchronized MediaFrame getLoopingFrameAtTime(double time) {
        double duration = getDurationSeconds();
        if (duration > 0 && time >= duration) {
            time = time % duration;
        }
        return getFrameAtTime(time);
    }

    /**
     * Returns the frame with the largest PTS ≤ the given time.
     * If no frame is available yet, returns null.
     * If time is beyond all received frames and the stream is not completed,
     * returns the last frame (to keep showing something).
     */
    public synchronized MediaFrame getFrameAtTime(double time) {
        if (frames.isEmpty()) return null;

        // Binary search for insertion point
        int idx = Collections.binarySearch(frames, new MediaFrame(null, time),
                Comparator.comparingDouble(MediaFrame::pts));
        if (idx >= 0) {
            // Exact match
            return frames.get(idx);
        } else {
            int insertionPoint = -idx - 1;
            if (insertionPoint == 0) {
                // Requested time before first frame → return first frame? Or null?
                // We'll return first frame (closest available)
                return frames.getFirst();
            } else {
                // Return the frame just before the insertion point
                return frames.get(insertionPoint - 1);
            }
        }
    }

    /**
     * Legacy sequential access – kept for compatibility.
     * Not recommended for new code.
     */
    @Deprecated
    public synchronized MediaFrame getFrame() {
        return frames.isEmpty() ? null : frames.get(0);
    }

    @Deprecated
    public synchronized boolean advance() {
        // This method is obsolete with time‑based lookup.
        // We keep it only for existing callers.
        return false;
    }

    @Deprecated
    public synchronized int getCurrentDisplayFrameNumber() {
        return frames.isEmpty() ? 0 : 1;
    }

    @Override
    public synchronized void close() {
        for (MediaFrame frame : frames) {
            if (frame.image() != null) {
                frame.image().close();
            }
        }
        frames.clear();
        completed = false;
    }
}
