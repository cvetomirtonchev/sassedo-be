package server.sassedo.utils;

import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;

/**
 * Builds cacheable responses for the image-serving endpoints. Images are effectively immutable
 * (served by a stable id), so we advertise a long browser/proxy cache lifetime and an {@code ETag}.
 * This lets browsers and the Nginx proxy cache serve repeat requests without re-hitting the backend,
 * which — together with image downscaling — keeps concurrent image loads from exhausting the heap.
 */
public final class ImageResponses {

    private static final long MAX_AGE_SECONDS = 86400; // 1 day

    private ImageResponses() {
    }

    /**
     * @param data         the image bytes (already loaded by the caller)
     * @param mediaType    the content type to serve
     * @param ifNoneMatch  the request's {@code If-None-Match} header (may be {@code null})
     */
    public static ResponseEntity<byte[]> cached(byte[] data, MediaType mediaType, String ifNoneMatch) {
        if (data == null || data.length == 0) {
            return ResponseEntity.notFound().build();
        }
        String etag = "\"" + Integer.toHexString(Arrays.hashCode(data)) + "-" + data.length + "\"";
        CacheControl cacheControl = CacheControl.maxAge(MAX_AGE_SECONDS, TimeUnit.SECONDS).cachePublic();

        if (ifNoneMatch != null && matches(ifNoneMatch, etag)) {
            return ResponseEntity.status(HttpStatus.NOT_MODIFIED)
                    .eTag(etag)
                    .cacheControl(cacheControl)
                    .build();
        }
        return ResponseEntity.ok()
                .eTag(etag)
                .cacheControl(cacheControl)
                .contentType(mediaType)
                .body(data);
    }

    private static boolean matches(String ifNoneMatch, String etag) {
        for (String candidate : ifNoneMatch.split(",")) {
            String trimmed = candidate.trim();
            if (trimmed.equals(etag) || trimmed.equals("*")) {
                return true;
            }
        }
        return false;
    }
}
