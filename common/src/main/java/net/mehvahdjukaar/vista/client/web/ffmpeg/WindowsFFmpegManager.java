package net.mehvahdjukaar.vista.client.web.ffmpeg;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class WindowsFFmpegManager extends FFmpegManager {

    private static final String ARCHIVE_NAME = "ffmpeg.zip";
    private static final String SOURCE_KEY = "windows";

    public WindowsFFmpegManager() {
        super("ffmpeg.exe", "ffprobe.exe");
    }

    @Override
    protected String getSourceKey() {
        return SOURCE_KEY;
    }

    @Override
    protected String getArchiveName() {
        return ARCHIVE_NAME;
    }

    @Override
    protected void acceptDownloaded(Path archive) throws IOException {
        try (ZipFile zf = new ZipFile(archive.toFile())) {
            Enumeration<? extends ZipEntry> entries = zf.entries();

            while (entries.hasMoreElements()) {
                ZipEntry e = entries.nextElement();

                if (e.getName().endsWith("ffmpeg.exe") ||
                    e.getName().endsWith("ffprobe.exe")) {

                    Path dest = programFolder.resolve(
                        new File(e.getName()).getName()
                    );

                    try (InputStream is = zf.getInputStream(e)) {
                        Files.copy(is, dest, StandardCopyOption.REPLACE_EXISTING);
                    }
                }
            }
        }
    }

}
