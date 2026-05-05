package net.mehvahdjukaar.vista.client.web.ffmpeg;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

public class LinuxFFmpegManager extends FFmpegManager {

    private static final String URL =
        "https://johnvansickle.com/ffmpeg/releases/ffmpeg-release-amd64-static.tar.xz";
    private static final String ARCHIVE_NAME = "ffmpeg.tar.xz";

    public LinuxFFmpegManager() {
        super("ffmpeg", "ffprobe");
    }

    @Override
    protected String getDownloadUrl() {
        return URL;
    }

    @Override
    protected String getArchiveName() {
        return ARCHIVE_NAME;
    }

    @Override
    protected void acceptDownloaded(Path archive) throws Exception {
        String cmd = String.format(
            "xz -dc %s | tar -xf - -C %s",
            archive.toAbsolutePath(),
            programFolder.toAbsolutePath()
        );

        Process p = new ProcessBuilder("sh", "-c", cmd).start();
        if (p.waitFor() != 0) {
            throw new IOException("Extraction failed");
        }

        moveBinaries();
        markExec();
    }

    private void markExec() {
        ffmpegPath.toFile().setExecutable(true);
        ffprobePath.toFile().setExecutable(true);
    }

    private void moveBinaries() throws IOException {
        try (var s = Files.walk(programFolder)) {
            s.filter(Files::isRegularFile)
             .filter(p -> {
                 String n = p.getFileName().toString();
                 return n.equals("ffmpeg") || n.equals("ffprobe");
             })
             .forEach(p -> {
                 try {
                     Files.move(p,
                         programFolder.resolve(p.getFileName()),
                         StandardCopyOption.REPLACE_EXISTING);
                 } catch (IOException ignored) {}
             });
        }
    }
}