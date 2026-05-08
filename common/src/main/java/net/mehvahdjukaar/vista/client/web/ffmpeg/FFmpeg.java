package net.mehvahdjukaar.vista.client.web.ffmpeg;

import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import net.minecraft.world.level.storage.loot.entries.LootPoolEntries;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Stream;

public final class FFmpeg {

    private static final Path SOURCES_CONFIG_PATH = Paths.get("ffmpeg_sources.json");
    private static final String SOURCES_RESOURCE_PATH = "/ffmpeg_sources.json";
    private static final Path PROGRAM_FOLDER = Paths.get("ffmpeg_bin");

    private final Path ffmpegPath;
    private final Path ffprobePath;
    private final OsType platform;

    public static CompletableFuture<FFmpeg> create() {
        FFmpeg instance = new FFmpeg(OsType.detect());
        return CompletableFuture.runAsync(instance::initialize)
                .thenApply(unused -> instance);
    }

    private FFmpeg(OsType platform) {
        this.platform = platform;
        LootPoolEntries
        this.ffmpegPath = PROGRAM_FOLDER.resolve(platform.ffmpegName);
        this.ffprobePath = PROGRAM_FOLDER.resolve(platform.ffprobeName);
    }

    // ===== COMMON LOGIC =====
    private void initialize() {
        try {
            Files.createDirectories(PROGRAM_FOLDER);
            if (Files.exists(ffmpegPath) && Files.exists(ffprobePath)) return;
            String downloadUrl = getDownloadUrlFromSources();
            Path archive = PROGRAM_FOLDER.resolve(ArchiveUtils.extractFileNameFromUrl(downloadUrl));

            if (Files.exists(archive) && !ArchiveUtils.isProbablyValid(archive)) {
                Files.deleteIfExists(archive);
            }

            if (!Files.exists(archive)) {
                //await
                FileDownloader.download(downloadUrl, archive);
            }

            extractAndInstall(archive);
            Files.deleteIfExists(archive);

        } catch (Exception e) {
            throw new RuntimeException("FFmpeg setup failed. Aborting.", e);
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
        if (!url.startsWith("http")) {
            url = "https://" + url;
        }
        return url;
    }

    private void ensureSourcesConfigExists() throws IOException {
        if (Files.exists(SOURCES_CONFIG_PATH)) return;

        try (InputStream in = FFmpeg.class.getResourceAsStream(SOURCES_RESOURCE_PATH)) {
            if (in == null) {
                throw new IOException("Resource not found: " + SOURCES_RESOURCE_PATH);
            }
            Files.copy(in, SOURCES_CONFIG_PATH);
        }
    }

    public Process runFFmpeg(String... args) throws IOException {
        return run(ffmpegPath, args);
    }

    public Process runFFprobe(String... args) throws IOException {
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
        if (!ArchiveUtils.isSupported(archive)) {
            throw new IOException("Unsupported archive format: " + archive.getFileName());
        }
        ArchiveUtils.extract(archive, PROGRAM_FOLDER);
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
