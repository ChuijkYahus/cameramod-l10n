package net.mehvahdjukaar.vista.client.web.ffmpeg;

import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

public final class FFmpegManager {

    private static final Path SOURCES_CONFIG_PATH = Paths.get("ffmpeg_sources.json");
    private static final String SOURCES_RESOURCE_PATH = "/ffmpeg_sources.json";
    private static final Path PROGRAM_FOLDER = Paths.get("ffmpeg_bin");

    private final Path ffmpegPath;
    private final Path ffprobePath;
    private final OsType platform;

    private final CompletableFuture<Void> readyFuture;

    public static FFmpegManager create() {
        return new FFmpegManager(OsType.detect());
    }

    private FFmpegManager(OsType platform) {
        this.platform = platform;
        this.ffmpegPath = PROGRAM_FOLDER.resolve(platform.ffmpegName);
        this.ffprobePath = PROGRAM_FOLDER.resolve(platform.ffprobeName);

        try {
            Files.createDirectories(PROGRAM_FOLDER);
        } catch (IOException e) {
            throw new RuntimeException("Failed to create bin folder", e);
        }

        readyFuture = CompletableFuture.runAsync(this::initialize);
    }


    // ===== COMMON LOGIC =====
    private void initialize() {
        if (Files.exists(ffmpegPath) && Files.exists(ffprobePath)) return;

        try {
            String downloadUrl = getDownloadUrlFromSources();
            Path archive = PROGRAM_FOLDER.resolve(getArchiveName(downloadUrl));

            if (Files.exists(archive) && !ArchiveExtractor.isProbablyValid(archive)) {
                Files.deleteIfExists(archive);
            }

            if (!Files.exists(archive)) {
                downloadWithRetry(downloadUrl, archive);
            }

            extractAndInstall(archive);
            Files.deleteIfExists(archive);

        } catch (Exception e) {
            throw new RuntimeException("FFmpeg setup failed", e);
        }
    }

    private String getDownloadUrlFromSources() throws IOException {
        ensureSourcesConfigExists();

        String json = Files.readString(SOURCES_CONFIG_PATH, StandardCharsets.UTF_8);
        JsonObject root;
        try {
            root = JsonParser.parseString(json).getAsJsonObject();
        } catch (IllegalStateException | JsonParseException e) {
            throw new IOException("Invalid JSON in " + SOURCES_CONFIG_PATH, e);
        }

        String key = platform.jsonKey;
        if (!root.has(key)) {
            throw new IOException("Missing key '" + key + "' in " + SOURCES_CONFIG_PATH);
        }
        String url = root.get(key).getAsString().trim();
        if (url.isEmpty()) {
            throw new IOException("Empty URL for key '" + key + "' in " + SOURCES_CONFIG_PATH);
        }
        return url;
    }

    private String getArchiveName(String downloadUrl) throws IOException {
        String fileName = Path.of(URI.create(downloadUrl).getPath()).getFileName().toString();
        if (fileName.isEmpty()) {
            throw new IOException("Could not resolve archive name from URL: " + downloadUrl);
        }
        return fileName;
    }

    private void ensureSourcesConfigExists() throws IOException {
        if (Files.exists(SOURCES_CONFIG_PATH)) return;

        try (InputStream in = FFmpegManager.class.getResourceAsStream(SOURCES_RESOURCE_PATH)) {
            if (in == null) {
                throw new IOException("Resource not found: " + SOURCES_RESOURCE_PATH);
            }
            Files.copy(in, SOURCES_CONFIG_PATH);
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
                } catch (InterruptedException ignored) {
                }
            }
        }
    }

    private void downloadFile(String urlStr, Path target) throws IOException {
        Path tmp = target.resolveSibling(target.getFileName() + ".part");

        URLConnection conn = URI.create(urlStr).toURL().openConnection();
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

    private void moveRequiredBinariesFromProgramFolder() throws IOException {
        Path ffmpeg = null;
        Path ffprobe = null;

        try (Stream<Path> stream = Files.walk(PROGRAM_FOLDER)) {
            for (Path p : (Iterable<Path>) stream.filter(Files::isRegularFile)::iterator) {
                String name = p.getFileName().toString();
                if (name.equals(ffmpegPath.getFileName().toString())) {
                    ffmpeg = p;
                } else if (name.equals(ffprobePath.getFileName().toString())) {
                    ffprobe = p;
                }
                if (ffmpeg != null && ffprobe != null) {
                    break;
                }
            }
        }

        if (ffmpeg == null || ffprobe == null) {
            throw new IOException("Archive does not contain required binaries: "
                    + ffmpegPath.getFileName() + ", " + ffprobePath.getFileName());
        }

        Files.move(ffmpeg, ffmpegPath, StandardCopyOption.REPLACE_EXISTING);
        Files.move(ffprobe, ffprobePath, StandardCopyOption.REPLACE_EXISTING);
    }

    private void extractAndInstall(Path archive) throws IOException, InterruptedException {
        if (!ArchiveExtractor.isSupported(archive)) {
            throw new IOException("Unsupported archive format: " + archive.getFileName());
        }
        ArchiveExtractor.extract(archive, PROGRAM_FOLDER);
        moveRequiredBinariesFromProgramFolder();
        if (platform.requiresExecutableBit) {
            markExecutables();
        }
    }

    private void markExecutables() throws IOException {
        if (!ffmpegPath.toFile().setExecutable(true) || !ffprobePath.toFile().setExecutable(true)) {
            throw new IOException("Could not mark FFmpeg binaries as executable");
        }
    }

    private enum OsType {
        LINUX("linux", "ffmpeg", "ffprobe", true),
        WINDOWS("windows", "ffmpeg.exe", "ffprobe.exe", false);

        private final String jsonKey;
        private final String ffmpegName;
        private final String ffprobeName;
        private final boolean requiresExecutableBit;

        OsType(String sourceKey, String ffmpegName, String ffprobeName, boolean requiresExecutableBit) {
            this.jsonKey = sourceKey;
            this.ffmpegName = ffmpegName;
            this.ffprobeName = ffprobeName;
            this.requiresExecutableBit = requiresExecutableBit;
        }

        private static OsType detect() {
            String os = System.getProperty("os.name").toLowerCase(Locale.ROOT);
            return os.contains("win") ? OsType.WINDOWS : OsType.LINUX;
        }
    }


}
