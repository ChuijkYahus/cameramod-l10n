package net.mehvahdjukaar.vista.client.web;

import org.jetbrains.annotations.Nullable;

public record FrameLookup(@Nullable MediaFrame frame, State state) {
    public enum State {
        LOADING,
        READY,
        BUFFERING,
        COMPLETE,
        FAILED,
        CLOSED
    }

    public boolean hasFrame() {
        return frame != null && frame.image() != null;
    }
}
