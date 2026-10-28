package net.mehvahdjukaar.vista.client.web;

import net.mehvahdjukaar.moonlight.api.util.FileDownloadUtils;
import net.mehvahdjukaar.vista.VistaMod;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public class MediaCacheManager {
    private static final String CACHE_SUBDIR = "vista_web_content_cache";
    private final Path cacheDir;
    private final long maxSizeBytes;
    private final Map<String, CachedEntry> urlToEntry = new ConcurrentHashMap<>();
    private final Map<Path, Integer> refCounts = new ConcurrentHashMap<>();
    private final Map<Path, Long> lastAccess = new ConcurrentHashMap<>();
    private final Map<String, CompletableFuture<Path>> pendingDownloads = new ConcurrentHashMap<>();
    private final Object downloadLock = new Object();

    public MediaCacheManager(Path baseDir, long maxSizeBytes) {
        this.cacheDir = baseDir.resolve(CACHE_SUBDIR);
        this.maxSizeBytes = maxSizeBytes;
        try {
            Files.createDirectories(cacheDir);
            restoreFromDisk();
            VistaMod.LOGGER.info("Media Cache initialized at {}, max size = {} MB", cacheDir, maxSizeBytes / (1024 * 1024));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public Path getOrDownload(URI uri) throws Exception {
        return getOrDownload(uri, null, null);
    }

    public Path getOrDownload(URI uri, @Nullable FileDownloadUtils.ProgressCallback progressCallback) throws Exception {
        return getOrDownload(uri, progressCallback, null);
    }

    public Path getOrDownload(URI uri, @Nullable FileDownloadUtils.ProgressCallback progressCallback,
                              @Nullable FileDownloadUtils.RetryCallback retryCallback) throws Exception {
        try {
            String scheme = uri.getScheme();
            boolean isLocalFileSoNoCaching = scheme == null || scheme.equalsIgnoreCase("file");
            if (isLocalFileSoNoCaching) {
                Path localPath = (scheme == null) ? Path.of(uri.toString()) : Path.of(uri);
                if (!Files.exists(localPath)) {
                    throw new IOException("Local media file does not exist: " + localPath);
                }
                VistaMod.LOGGER.info("Using local media file {} for URL {}", localPath, uri.toString());
                return localPath;
            }
        } catch (IllegalArgumentException ignored) {
        }

        String key = hashUrl(uri.toString());
        CachedEntry existing = urlToEntry.get(key);
        if (existing != null && Files.exists(existing.path)) {
            synchronized (this) {
                refCounts.merge(existing.path, 1, Integer::sum);
                touch(existing.path);
            }
            VistaMod.LOGGER.info("Cache HIT for {} -> {} (refCount={})", uri.toString(), existing.path, refCounts.get(existing.path));
            return existing.path;
        }

        CompletableFuture<Path> future = pendingDownloads.get(key);
        if (future != null && !future.isDone()) {
            VistaMod.LOGGER.info("Waiting for ongoing download of {}", uri.toString());
            return holdDownloaded(future.get());
        }

        //one download at a time
        synchronized (downloadLock) {
            //might have finished while we waited
            existing = urlToEntry.get(key);
            if (existing != null && Files.exists(existing.path)) {
                synchronized (this) {
                    refCounts.merge(existing.path, 1, Integer::sum);
                    touch(existing.path);
                }
                return existing.path;
            }
            future = pendingDownloads.get(key);
            if (future != null && !future.isDone()) {
                return holdDownloaded(future.get());
            }

            CompletableFuture<Path> downloadFuture = new CompletableFuture<>();
            pendingDownloads.put(key, downloadFuture);

            try {
                Path cachedPath = downloadAndCache(uri.toString(), key, progressCallback, retryCallback);
                downloadFuture.complete(cachedPath);
                synchronized (this) {
                    urlToEntry.put(key, new CachedEntry(cachedPath));
                    refCounts.put(cachedPath, 1);
                    touch(cachedPath);
                    enforceQuota();
                }
                VistaMod.LOGGER.info("Download & cache completed for {} -> {}", uri.toString(), cachedPath);
                return cachedPath;
            } catch (Exception e) {
                downloadFuture.completeExceptionally(e);
                throw e;
            } finally {
                pendingDownloads.remove(key);
            }
        }
    }

    private synchronized Path holdDownloaded(Path path) {
        refCounts.merge(path, 1, Integer::sum);
        return path;
    }

    public synchronized void release(URI uri) {
        CachedEntry entry = urlToEntry.get(hashUrl(uri.toString()));
        if (entry == null) return;
        refCounts.computeIfPresent(entry.path, (p, count) -> Math.max(0, count - 1));
    }

    private Path downloadAndCache(String urlStr, String key, @Nullable FileDownloadUtils.ProgressCallback progressCallback,
                                  @Nullable FileDownloadUtils.RetryCallback retryCallback) throws Exception {
        Path cachedPath = cacheDir.resolve(key + ".video");
        FileDownloadUtils.download(urlStr, cachedPath, "Mozilla/5.0", progressCallback, retryCallback);
        return cachedPath;
    }

    private void restoreFromDisk() throws IOException {
        try (var stream = Files.list(cacheDir)) {
            List<Path> files = stream.toList();
            for (Path p : files) {
                if (Files.isRegularFile(p) && p.toString().endsWith(".video")) {
                    String key = p.getFileName().toString().replace(".video", "");
                    urlToEntry.put(key, new CachedEntry(p));
                    refCounts.put(p, 0);
                    lastAccess.put(p, Files.getLastModifiedTime(p).toMillis());
                }
            }
        }
        VistaMod.LOGGER.info("Restored {} cached videos from disk", urlToEntry.size());
        enforceQuota();
    }

    private void enforceQuota() {
        long total = 0;
        for (Path p : refCounts.keySet()) {
            try {
                total += Files.size(p);
            } catch (IOException ignored) {
            }
        }
        if (total <= maxSizeBytes) return;

        VistaMod.LOGGER.info("Cache size {} MB exceeds limit {} MB. Evicting...", total / (1024 * 1024), maxSizeBytes / (1024 * 1024));
        List<Map.Entry<Path, Long>> entries = new ArrayList<>(lastAccess.entrySet());
        entries.sort(Map.Entry.comparingByValue());
        for (var e : entries) {
            Path p = e.getKey();
            if (refCounts.getOrDefault(p, 0) == 0) {
                try {
                    long size = Files.size(p);
                    Files.deleteIfExists(p);
                    total -= size;
                    String keyToRemove = null;
                    for (var entry : urlToEntry.entrySet()) {
                        if (entry.getValue().path.equals(p)) {
                            keyToRemove = entry.getKey();
                            break;
                        }
                    }
                    if (keyToRemove != null) urlToEntry.remove(keyToRemove);
                    refCounts.remove(p);
                    lastAccess.remove(p);
                    VistaMod.LOGGER.info("Evicted {} (size {} KB)", p, size / (1024));
                    if (total <= maxSizeBytes) break;
                } catch (IOException ignored) {
                }
            }
        }
    }

    private void touch(Path p) {
        lastAccess.put(p, System.currentTimeMillis());
    }

    private String hashUrl(String url) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(md.digest(url.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new RuntimeException("Hashing failed", e);
        }
    }

    private record CachedEntry(Path path) {
    }
}
