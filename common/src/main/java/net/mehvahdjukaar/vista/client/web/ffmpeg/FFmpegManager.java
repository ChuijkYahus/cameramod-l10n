package net.mehvahdjukaar.vista.client.web.ffmpeg;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public abstract class FFmpegManager {

    protected final Path programFolder = Paths.get("ffmpeg_bin");
    protected final Path ffmpegPath;
    protected final Path ffprobePath;

    private final CompletableFuture<Void> readyFuture;

    public static FFmpegManager createOsBased() {
        String os = System.getProperty("os.name").toLowerCase();

        if (os.contains("win")) {
            return new WindowsFFmpegManager();
        } else {
            return new LinuxFFmpegManager();
        }
    }

    protected FFmpegManager(String ffmpegName, String ffprobeName) {
        this.ffmpegPath = programFolder.resolve(ffmpegName);
        this.ffprobePath = programFolder.resolve(ffprobeName);

        try {
            Files.createDirectories(programFolder);
        } catch (IOException e) {
            throw new RuntimeException("Failed to create bin folder", e);
        }

        readyFuture = CompletableFuture.runAsync(this::initialize);
    }

    // ===== ABSTRACT CONTRACT =====
    protected abstract String getDownloadUrl();
    protected abstract String getArchiveName();
    protected abstract void acceptDownloaded(Path archive) throws Exception;

    // ===== COMMON LOGIC =====
    private void initialize() {
        if (Files.exists(ffmpegPath) && Files.exists(ffprobePath)) return;

        Path archive = programFolder.resolve(getArchiveName());

        try {
            if (Files.exists(archive) && !isValidArchive(archive)) {
                Files.deleteIfExists(archive);
            }

            if (!Files.exists(archive)) {
                downloadWithRetry(getDownloadUrl(), archive);
            }

            acceptDownloaded(archive);
            Files.deleteIfExists(archive);

        } catch (Exception e) {
            throw new RuntimeException("FFmpeg setup failed", e);
        }
    }

    protected boolean isValidArchive(Path archive) {
        try {
            long size = Files.size(archive);
            if (size == 0) return false;

            String name = archive.getFileName().toString().toLowerCase();

            if (name.endsWith(".zip")) {
                try (java.util.zip.ZipFile ignored = new java.util.zip.ZipFile(archive.toFile())) {
                    return true;
                }
            }

            // For .tar.xz we only do a minimal check
            // (full validation would require full decompression)
            return size > 1024; // reject obviously broken files

        } catch (Exception e) {
            return false;
        }
    }

    private void downloadWithRetry(String url, Path target) throws IOException {
        int attempts = 3;

        for (int i = 1; i <= attempts; i++) {
            try {
                downloadFile(url, target);
                return;
            } catch (IOException e) {
                System.err.println("Download failed (attempt " + i + "): " + e.getMessage());

                if (i == attempts) throw e;

                try {
                    Thread.sleep(1000L * i);
                } catch (InterruptedException ignored) {}
            }
        }
    }

    private void downloadFile(String urlStr, Path target) throws IOException {
        Path tmp = target.resolveSibling(target.getFileName() + ".part");

        URLConnection conn = new URL(urlStr).openConnection();
        long expected = conn.getContentLengthLong();

        try (InputStream in = conn.getInputStream();
             OutputStream out = Files.newOutputStream(tmp,
                     StandardOpenOption.CREATE,
                     StandardOpenOption.TRUNCATE_EXISTING)) {

            byte[] buf = new byte[16384];
            long done = 0;
            int r;
            int lastPercent = -1;

            while ((r = in.read(buf)) != -1) {
                out.write(buf, 0, r);
                done += r;

                if (expected > 0) {
                    int percent = (int) (done * 100 / expected);
                    if (percent != lastPercent) {
                        System.out.print("\rProgress: " + percent + "%");
                        lastPercent = percent;
                    }
                }
            }
            System.out.println();
        }

        // Validate size
        if (expected > 0 && Files.size(tmp) != expected) {
            Files.deleteIfExists(tmp);
            throw new IOException("Incomplete download (size mismatch)");
        }

        // Atomic move to final file
        Files.move(tmp, target,
                StandardCopyOption.REPLACE_EXISTING,
                StandardCopyOption.ATOMIC_MOVE);
    }

    public void waitUntilReady() throws IOException {
        try {
            readyFuture.get(5, TimeUnit.MINUTES);
        } catch (Exception e) {
            throw new IOException("Init failed", e);
        }
    }

    public Process runFFmpeg(String... args) throws IOException {
        waitUntilReady();
        return run(ffmpegPath, args);
    }

    public Process runFFprobe(String... args) throws IOException {
        waitUntilReady();
        return run(ffprobePath, args);
    }

    private Process run(Path bin, String... args) throws IOException {
        String[] cmd = new String[args.length + 1];
        cmd[0] = bin.toString();
        System.arraycopy(args, 0, cmd, 1, args.length);
        return new ProcessBuilder(cmd).start();
    }
}