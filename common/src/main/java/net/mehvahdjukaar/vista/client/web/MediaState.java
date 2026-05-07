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

    public static Pair ready(MediaFrame frame) {
        return new Pair(frame, READY);
    }

    public static Pair buffering(MediaFrame frame) {
        return new Pair(frame, BUFFERING);
    }

    public static Pair complete(MediaFrame frame) {
        return new Pair(frame, COMPLETE);
    }

    public static Pair failed() {
        return new Pair(null, FAILED);
    }

    public static Pair closed() {
        return new Pair(null, CLOSED);
    }

    public record Pair(@Nullable MediaFrame frame, MediaState state) {
    }
}
