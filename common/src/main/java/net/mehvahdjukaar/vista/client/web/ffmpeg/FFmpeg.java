package net.mehvahdjukaar.vista.client.web.ffmpeg;

import java.io.IOException;
import java.nio.file.Path;

public final class FFmpeg {

    private final Path ffprobePath;
    private final Path ffmpegPath;

    public FFmpeg(Path ffmpegPath, Path ffprobePath) {
        this.ffmpegPath = ffmpegPath;
        this.ffprobePath = ffprobePath;
    }

    public Process runFFmpeg(String... args) throws IOException {
        return run(ffmpegPath, args);
    }

    public Process runFFprobe(String... args) throws IOException {
        return run(ffprobePath, args);
    }

    private static Process run(Path bin, String... args) throws IOException {
        String[] cmd = new String[args.length + 1];
        cmd[0] = bin.toString();
        System.arraycopy(args, 0, cmd, 1, args.length);
        return new ProcessBuilder(cmd).start();
    }

}
