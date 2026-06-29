package net.mehvahdjukaar.vista.client.web.ffmpeg;

import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import net.mehvahdjukaar.moonlight.api.util.ArchiveUtils;
import net.mehvahdjukaar.moonlight.api.util.FileDownloadUtils;
import net.mehvahdjukaar.moonlight.api.util.OsType;
import net.mehvahdjukaar.vista.VistaMod;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

public final class FFmpegManager {

    private static final Path SOURCES_CONFIG_PATH = Paths.get("vista_ffmpeg_sources.json");
    private static final String SOURCES_RESOURCE_PATH = "/vista_ffmpeg_sources.json";
    private static final Path PROGRAM_FOLDER = Paths.get("vista_ffmpeg_bin");
    private static volatile int downloadProgress = -1;

    private static final OsType OS_TYPE = OsType.current();
    private static final Path FFMPEG_PATH = PROGRAM_FOLDER.resolve(OS_TYPE.executableName("ffmpeg"));
    private static final Path FFPROBE_PATH = PROGRAM_FOLDER.resolve(OS_TYPE.executableName("ffprobe"));

    public static CompletableFuture<FFmpeg> getOrDownload(@Nullable String customUrl) {
        return CompletableFuture.supplyAsync(() -> initialize(customUrl));
    }

    public static int getDownloadProgress() {
        return downloadProgress;
    }

    private static FFmpeg initialize(@Nullable String customUrl) {
        try {
            Files.createDirectories(PROGRAM_FOLDER);
            if (!hasRequiredFiles()) {
                // Prefer a system-wide install (in PATH) before downloading our own copy,
                // unless the user explicitly forced a custom download URL.
                if (customUrl == null) {
                    FFmpeg system = detectSystemFFmpeg();
                    if (system != null) {
                        downloadProgress = -1;
                        return system;
                    }
                }
                downloadProgress = -1;
                String downloadUrl = customUrl != null ? customUrl : getDownloadUrlFromSources();
                Path archive = PROGRAM_FOLDER.resolve(ArchiveUtils.extractFileNameFromUrl(downloadUrl));

                if (Files.exists(archive) && !ArchiveUtils.isProbablyValid(archive)) {
                    Files.deleteIfExists(archive);
                }

                if (!Files.exists(archive)) {
                    //await
                    FileDownloadUtils.download(downloadUrl, archive, null, percent -> downloadProgress = percent);
                }

                extractAndInstall(archive);
                Files.deleteIfExists(archive);
            }
            downloadProgress = -1;
        } catch (Exception e) {
            downloadProgress = -1;
            throw new RuntimeException("FFmpeg setup failed. Aborting.", e);
        }
        VistaMod.LOGGER.info("Using managed FFmpeg binaries at {}", FFMPEG_PATH.toAbsolutePath());
        return new FFmpeg(FFMPEG_PATH, FFPROBE_PATH);
    }

    public static boolean hasRequiredFiles() {
        return Files.exists(FFMPEG_PATH) && Files.exists(FFPROBE_PATH);
    }

    /**
     * Looks for a system-wide FFmpeg install on the user's PATH. Both ffmpeg and ffprobe
     * must be present, otherwise we fall back to downloading our own copy.
     * Cheap (a handful of filesystem stats) and never downloads anything, so it's safe
     * to call on the main thread to decide whether a download is even needed.
     */
    @Nullable
    public static FFmpeg detectSystemFFmpeg() {
        Path ffmpeg = findInPath(OS_TYPE.executableName("ffmpeg"));
        Path ffprobe = findInPath(OS_TYPE.executableName("ffprobe"));
        if (ffmpeg != null && ffprobe != null) {
            VistaMod.LOGGER.info("Using system FFmpeg from PATH: {} and {}", ffmpeg, ffprobe);
            return new FFmpeg(ffmpeg, ffprobe);
        }
        return null;
    }

    @Nullable
    private static Path findInPath(String executableName) {
        String pathEnv = System.getenv("PATH");
        if (pathEnv == null || pathEnv.isEmpty()) return null;
        for (String dir : pathEnv.split(File.pathSeparator)) {
            if (dir.isEmpty()) continue;
            try {
                Path candidate = Paths.get(dir).resolve(executableName);
                if (Files.isRegularFile(candidate) && Files.isExecutable(candidate)) {
                    return candidate.toAbsolutePath();
                }
            } catch (Exception ignored) {
                // malformed PATH entry, skip
            }
        }
        return null;
    }

    private static String getDownloadUrlFromSources() throws IOException {
        ensureSourcesConfigExists();

        String json = Files.readString(SOURCES_CONFIG_PATH, StandardCharsets.UTF_8);
        JsonObject root;
        try {
            root = JsonParser.parseString(json).getAsJsonObject();
        } catch (IllegalStateException | JsonParseException e) {
            throw new IOException("Invalid JSON in " + SOURCES_CONFIG_PATH, e);
        }

        String key = OS_TYPE.key();
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

    private static void ensureSourcesConfigExists() throws IOException {
        if (Files.exists(SOURCES_CONFIG_PATH)) return;

        try (InputStream in = FFmpegManager.class.getResourceAsStream(SOURCES_RESOURCE_PATH)) {
            if (in == null) {
                throw new IOException("Resource not found: " + SOURCES_RESOURCE_PATH);
            }
            Files.copy(in, SOURCES_CONFIG_PATH);
        }
    }

    private static void moveRequiredBinariesFromProgramFolder() throws IOException {
        Path ffmpeg = null;
        Path ffprobe = null;

        try (Stream<Path> stream = Files.walk(PROGRAM_FOLDER)) {
            for (Path p : (Iterable<Path>) stream.filter(Files::isRegularFile)::iterator) {
                String name = p.getFileName().toString();
                if (name.equals(FFMPEG_PATH.getFileName().toString())) {
                    ffmpeg = p;
                } else if (name.equals(FFPROBE_PATH.getFileName().toString())) {
                    ffprobe = p;
                }
                if (ffmpeg != null && ffprobe != null) {
                    break;
                }
            }
        }

        if (ffmpeg == null || ffprobe == null) {
            throw new IOException("Archive does not contain required binaries: "
                    + FFMPEG_PATH.getFileName() + ", " + FFPROBE_PATH.getFileName());
        }

        Files.move(ffmpeg, FFMPEG_PATH, StandardCopyOption.REPLACE_EXISTING);
        Files.move(ffprobe, FFPROBE_PATH, StandardCopyOption.REPLACE_EXISTING);
    }

    private static void extractAndInstall(Path archive) throws IOException, InterruptedException {
        if (!ArchiveUtils.isSupported(archive)) {
            throw new IOException("Unsupported archive format: " + archive.getFileName());
        }
        ArchiveUtils.extract(archive, PROGRAM_FOLDER);
        moveRequiredBinariesFromProgramFolder();
        if (OS_TYPE.requiresExecutableBit()) {
            markExecutables();
        }
    }

    private static void markExecutables() throws IOException {
        if (!FFMPEG_PATH.toFile().setExecutable(true) || !FFPROBE_PATH.toFile().setExecutable(true)) {
            throw new IOException("Could not mark FFmpeg binaries as executable");
        }
    }

}
