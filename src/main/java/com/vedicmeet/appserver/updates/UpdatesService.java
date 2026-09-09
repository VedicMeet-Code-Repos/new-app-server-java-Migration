package com.vedicmeet.appserver.updates;

import com.vedicmeet.appserver.cache.CacheService;
import org.bson.Document;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Port of the manifest read path in Node modules/app-updates.js (+ BundleStorage).
 *
 * GET /updates/manifest.json:
 *   - cacheKey app:manifest:{platform}, 1h TTL
 *   - read update-bundles/manifest.json (local FS by default; S3 optional)
 *   - applyCdnUrlsToManifest: if a CDN base is configured, rewrite the two bundle URLs
 *   - return the manifest object as-is (NOT wrapped in the ApiResponse envelope)
 *   - missing manifest -> null here -> HTTP 404 {success:false} in the controller
 *
 * Default backend is local disk (Node: process.cwd()/update-bundles). The optional S3
 * backend (BUNDLE_USE_S3=true) needs the AWS SDK, which is intentionally not added to
 * keep the build light; enabling it without wiring the SDK fails fast with a clear
 * message rather than silently diverging.
 */
@Service
public class UpdatesService {

    private final CacheService cache;
    private final String bundlesDir;
    private final String cdnBaseUrl;   // BUNDLE_CDN_BASE_URL (null/empty = none)
    private final boolean useS3;       // BUNDLE_USE_S3

    public UpdatesService(CacheService cache,
                          @Value("${vedicmeet.bundles.dir:update-bundles}") String bundlesDir,
                          @Value("${BUNDLE_CDN_BASE_URL:}") String cdnBaseUrl,
                          @Value("${BUNDLE_USE_S3:false}") boolean useS3) {
        this.cache = cache;
        this.bundlesDir = bundlesDir;
        this.cdnBaseUrl = cdnBaseUrl;
        this.useS3 = useS3;
    }

    /** Returns the manifest document (CDN-rewritten if configured), or null if absent. */
    public Document getManifest(String platformParam) {
        String platform = (platformParam == null || platformParam.isEmpty()) ? "all" : platformParam;
        String cacheKey = "app:manifest:" + platform;

        Document cached = cache.get(cacheKey, Document.class);
        if (cached != null) {
            return cached;
        }

        String manifestRaw = readManifestText();
        if (manifestRaw == null) {
            return null; // controller maps this to HTTP 404
        }

        Document manifest = Document.parse(manifestRaw);
        applyCdnUrls(manifest);

        cache.set(cacheKey, manifest, 60 * 60); // 1 hour, as in Node
        return manifest;
    }

    private String readManifestText() {
        if (useS3) {
            throw new IllegalStateException(
                    "BUNDLE_USE_S3=true but S3 bundle storage is not wired in the Java port yet; "
                            + "use local update-bundles/ or add the AWS SDK path.");
        }
        try {
            Path path = Paths.get(bundlesDir, "manifest.json");
            if (!Files.exists(path)) {
                return null;
            }
            return Files.readString(path);
        } catch (Exception e) {
            return null;
        }
    }

    /** Mirrors applyCdnUrlsToManifest: only rewrites when a CDN base is configured. */
    private void applyCdnUrls(Document manifest) {
        if (cdnBaseUrl == null || cdnBaseUrl.isEmpty()) {
            return;
        }
        String base = cdnBaseUrl.replaceAll("/$", "");
        manifest.put("androidBundleUrl", base + "/index.android.bundle");
        manifest.put("iosBundleUrl", base + "/index.ios.bundle");
    }
}
