package net.mehvahdjukaar.vista.client.web;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Manages persistent disk cache for video files downloaded from HTTP URLs.
 * Features:
 * - URL -> local file mapping (using SHA-256 hash as filename)
 * - Reference counting (multiple TVs can share the same cached file)
 * - LRU eviction when total cache size exceeds a given limit
 * - Thread‑safe: concurrent downloads of the same URL are collapsed
 */
public class MediaCacheManager {
    private static final String CACHE_SUBDIR = "vista_web_content_cache";
    private final Path cacheDir;
    private final long maxSizeBytes;
    private final Map<String, CachedEntry> urlToEntry = new ConcurrentHashMap<>();
    private final Map<Path, Integer> refCounts = new ConcurrentHashMap<>();
    private final Map<Path, Long> lastAccess = new ConcurrentHashMap<>();
    private final Map<String, CompletableFuture<Path>> pendingDownloads = new ConcurrentHashMap<>();

    public MediaCacheManager(Path baseDir, long maxSizeBytes)  {
        this.cacheDir = baseDir.resolve(CACHE_SUBDIR);
        this.maxSizeBytes = maxSizeBytes;
        Files.createDirectories(cacheDir);
        restoreFromDisk();
        log("INFO", "Cache initialized at " + cacheDir + ", max size = " + (maxSizeBytes / (1024 * 1024)) + " MB");
    }

    /**
     * Obtains a local path for the given URL. Downloads if not already cached.
     */
    public Path getOrDownload(String url) throws Exception {
        String key = hashUrl(url);
        // Fast path: already cached and available
        CachedEntry existing = urlToEntry.get(key);
        if (existing != null && Files.exists(existing.path)) {
            synchronized (this) {
                existing.refCount++;
                touch(existing.path);
            }
            log("INFO", "Cache HIT for " + url + " -> " + existing.path + " (refCount=" + existing.refCount + ")");
            return existing.path;
        }

        // Check if another thread is already downloading this URL
        CompletableFuture<Path> future = pendingDownloads.get(key);
        if (future != null && !future.isDone()) {
            log("INFO", "Waiting for ongoing download of " + url);
            return future.get(); // blocks until download finishes
        }

        // Start a new download on the caller's worker thread.
        CompletableFuture<Path> downloadFuture = new CompletableFuture<>();
        CompletableFuture<Path> previous = pendingDownloads.putIfAbsent(key, downloadFuture);
        if (previous != null) {
            log("INFO", "Waiting for ongoing download of " + url);
            return previous.get();
        }

        try {
            Path cachedPath = downloadAndCache(url, key);
            downloadFuture.complete(cachedPath);
            synchronized (this) {
                urlToEntry.put(key, new CachedEntry(cachedPath, 1));
                refCounts.put(cachedPath, 1);
                touch(cachedPath);
                enforceQuota();
            }
            log("INFO", "Download & cache completed for " + url + " -> " + cachedPath);
            return cachedPath;
        } catch (Exception e) {
            downloadFuture.completeExceptionally(e);
            throw e;
        } finally {
            pendingDownloads.remove(key);
        }
    }

    /**
     * Releases a reference to a previously obtained URL. When refCount reaches zero, the file is deleted.
     */
    public void release(String url) {
        String key = hashUrl(url);
        CachedEntry entry = urlToEntry.get(key);
        if (entry == null) {
            log("WARN", "Release called for unknown URL: " + url);
            return;
        }
        synchronized (this) {
            entry.refCount--;
            log("INFO", "Released " + url + ", new refCount=" + entry.refCount);
            if (entry.refCount <= 0) {
                urlToEntry.remove(key);
                refCounts.remove(entry.path);
                lastAccess.remove(entry.path);
                try {
                    Files.deleteIfExists(entry.path);
                    log("INFO", "Deleted cached file " + entry.path + " (no more references)");
                } catch (IOException e) {
                    log("ERROR", "Failed to delete " + entry.path + ": " + e.getMessage());
                }
            }
        }
    }

    // ---------- private helpers ----------
    private Path downloadAndCache(String urlStr, String key) throws Exception {
        log("INFO", "Downloading " + urlStr + " ...");
        Path tempFile = Files.createTempFile("download_", ".tmp");
        try {
            HttpURLConnection conn = (HttpURLConnection) new URL(urlStr).openConnection();
            conn.setRequestProperty("User-Agent", "Mozilla/5.0");
            conn.setConnectTimeout(10000);
            conn.setReadTimeout(30000);
            try (InputStream in = conn.getInputStream()) {
                Files.copy(in, tempFile, StandardCopyOption.REPLACE_EXISTING);
            }
            long size = Files.size(tempFile);
            log("INFO", "Downloaded " + size + " bytes from " + urlStr);
            Path cachedPath = cacheDir.resolve(key + ".video");
            Files.move(tempFile, cachedPath, StandardCopyOption.REPLACE_EXISTING);
            return cachedPath;
        } finally {
            Files.deleteIfExists(tempFile); // clean up temp if move fails
        }
    }

    private void restoreFromDisk() throws IOException {
        // Load existing files into cache map with refCount = 0 (they are not actively used)
        try (var stream = Files.list(cacheDir)) {
            List<Path> files = stream.collect(Collectors.toList());
            for (Path p : files) {
                if (Files.isRegularFile(p) && p.toString().endsWith(".video")) {
                    String key = p.getFileName().toString().replace(".video", "");
                    urlToEntry.put(key, new CachedEntry(p, 0));
                    refCounts.put(p, 0);
                    lastAccess.put(p, Files.getLastModifiedTime(p).toMillis());
                }
            }
        }
        log("INFO", "Restored " + urlToEntry.size() + " cached videos from disk");
        enforceQuota(); // clean up if necessary
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

        log("INFO", "Cache size " + (total / (1024 * 1024)) + " MB exceeds limit " + (maxSizeBytes / (1024 * 1024)) + " MB. Evicting...");
        // LRU: sort by lastAccess, oldest first, only evict files with refCount == 0
        List<Map.Entry<Path, Long>> entries = new ArrayList<>(lastAccess.entrySet());
        entries.sort(Map.Entry.comparingByValue());
        for (var e : entries) {
            Path p = e.getKey();
            if (refCounts.getOrDefault(p, 0) == 0) {
                try {
                    long size = Files.size(p);
                    Files.deleteIfExists(p);
                    total -= size;
                    // remove from all maps
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
                    log("INFO", "Evicted " + p + " (size " + (size / (1024)) + " KB)");
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
            byte[] hash = md.digest(url.getBytes("UTF-8"));
            StringBuilder hex = new StringBuilder();
            for (byte b : hash) hex.append(String.format("%02x", b));
            return hex.toString();
        } catch (Exception e) {
            throw new RuntimeException("Hashing failed", e);
        }
    }

    private void log(String level, String msg) {
        System.out.printf("[CacheManager] [%s] %s%n", level, msg);
    }

    private static class CachedEntry {
        final Path path;
        int refCount;

        CachedEntry(Path path, int refCount) {
            this.path = path;
            this.refCount = refCount;
        }
    }
}