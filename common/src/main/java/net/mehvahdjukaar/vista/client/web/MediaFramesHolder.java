package net.mehvahdjukaar.vista.client.web;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Thread‑safe container for decoded video frames.
 * Frames are stored in order of increasing PTS.
 * Supports looking up the closest frame ≤ a given timestamp.
 */
public class MediaFramesHolder {
    private final List<MediaFrame> frames = new ArrayList<>();
    private boolean completed = false;

    /** Adds a frame (assumed to be in PTS order). */
    public synchronized void add(MediaFrame frame) {
        frames.add(frame);
    }

    public synchronized void setCompleted(boolean completed) {
        this.completed = completed;
    }

    public synchronized boolean isCompleted() {
        return completed;
    }

    /** Returns the total number of frames received so far. */
    public synchronized int size() {
        return frames.size();
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
                return frames.get(0);
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
}