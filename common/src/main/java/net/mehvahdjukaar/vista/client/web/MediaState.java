package net.mehvahdjukaar.vista.client.web;

import org.jetbrains.annotations.Nullable;


public enum MediaState {
    LOADING,
    READY,
    BUFFERING,
    COMPLETE,
    FAILED,
    CLOSED;

    public static Pair loading() {
        return new Pair(null, LOADING);
    }

    public boolean isGood() {
        return this != FAILED && this != CLOSED;
    }

    public static Pair pair(MediaState state, @Nullable MediaFrame frameAtTimeLocked) {
        return new Pair(frameAtTimeLocked, state);
    }

    public record Pair(@Nullable MediaFrame frame, MediaState state) {
    }
}
