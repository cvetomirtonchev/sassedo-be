package server.sassedo.utils;

import java.util.Set;

import org.springframework.web.multipart.MultipartFile;

import server.sassedo.model.GenericException;
import server.sassedo.model.GenericExceptionCode;

public class ImageUploadValidator {

    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "image/jpeg",
            "image/png",
            "image/webp"
    );

    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(
            "jpg",
            "jpeg",
            "png",
            "webp"
    );

    private static final String ERROR_MESSAGE = "Unsupported image format. Allowed: JPEG, PNG, WebP";

    /**
     * Enforces the JPEG/PNG/WebP allowlist. A file is accepted when its declared
     * content type is in the allowlist, or (when the content type is missing or a
     * generic binary type) when its filename extension is in the allowlist.
     */
    public static void validate(MultipartFile file) throws GenericException {
        String contentType = file.getContentType();
        if (contentType != null) {
            contentType = contentType.toLowerCase().trim();
        }

        if (contentType != null && ALLOWED_CONTENT_TYPES.contains(contentType)) {
            return;
        }

        boolean missingContentType = contentType == null
                || contentType.isEmpty()
                || contentType.equals("application/octet-stream");
        if (missingContentType && hasAllowedExtension(file.getOriginalFilename())) {
            return;
        }

        throw new GenericException(GenericExceptionCode.INVALID_FILE, ERROR_MESSAGE);
    }

    private static boolean hasAllowedExtension(String filename) {
        if (filename == null) {
            return false;
        }
        int dot = filename.lastIndexOf('.');
        if (dot < 0 || dot == filename.length() - 1) {
            return false;
        }
        String extension = filename.substring(dot + 1).toLowerCase();
        return ALLOWED_EXTENSIONS.contains(extension);
    }
}
