package server.sassedo.utils;

import net.coobird.thumbnailator.Thumbnails;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

/**
 * Downscales and re-encodes uploaded images so stored blobs stay small without a visible loss of
 * quality. This is the main defence against the profile-photo OOM: raw phone uploads were stored
 * verbatim (up to ~4&nbsp;MB), and loading many of those into a 512&nbsp;MB heap exhausted memory.
 *
 * <p>Rules that protect quality:
 * <ul>
 *     <li>Only ever downscale (never upscale); aspect ratio is always preserved.</li>
 *     <li>EXIF orientation is applied so portrait phone photos are not rotated.</li>
 *     <li>Images with an alpha channel are re-encoded as PNG (lossless) so transparency is not
 *     flattened onto black; everything else becomes JPEG at the preset quality (~0.85 is visually
 *     near-lossless but 5-10x smaller than a multi-MB source).</li>
 *     <li>If the source cannot be decoded, or anything else fails, the original bytes are returned
 *     unchanged so an upload is never rejected because of processing.</li>
 * </ul>
 */
public final class ImageProcessor {

    private static final Logger log = LoggerFactory.getLogger(ImageProcessor.class);

    /**
     * Per-use-case sizing. The longest side is capped to {@code maxDimension}px; JPEG output uses
     * {@code quality} (0..1). Values are tuned to how each image is displayed in the UI.
     */
    public enum Preset {
        PROFILE(512, 0.85),
        LISTING(1600, 0.85),
        HERO(1920, 0.82),
        BLOG(1600, 0.85),
        CITY(1280, 0.82);

        private final int maxDimension;
        private final double quality;

        Preset(int maxDimension, double quality) {
            this.maxDimension = maxDimension;
            this.quality = quality;
        }
    }

    /** Result of processing: the (possibly) re-encoded bytes and the content type they now use. */
    public record ProcessedImage(byte[] data, String contentType) {
    }

    private ImageProcessor() {
    }

    public static ProcessedImage process(byte[] data, String contentType, Preset preset) {
        if (data == null || data.length == 0) {
            return new ProcessedImage(data, contentType);
        }
        try {
            BufferedImage source = ImageIO.read(new ByteArrayInputStream(data));
            if (source == null) {
                // No ImageIO reader could decode it (e.g. an exotic format). Keep the original.
                return new ProcessedImage(data, contentType);
            }

            boolean hasAlpha = source.getColorModel().hasAlpha();
            int longestSide = Math.max(source.getWidth(), source.getHeight());
            boolean needsResize = longestSide > preset.maxDimension;

            String outFormat = hasAlpha ? "png" : "jpg";
            String outContentType = hasAlpha ? "image/png" : "image/jpeg";

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            // Read from the raw bytes (not the decoded BufferedImage) so Thumbnailator can honour
            // EXIF orientation metadata.
            Thumbnails.Builder<? extends java.io.InputStream> builder =
                    Thumbnails.of(new ByteArrayInputStream(data))
                            .useExifOrientation(true)
                            .outputFormat(outFormat);
            if (needsResize) {
                builder.size(preset.maxDimension, preset.maxDimension);
            } else {
                builder.scale(1.0);
            }
            if ("jpg".equals(outFormat)) {
                builder.outputQuality(preset.quality);
            }
            builder.toOutputStream(out);
            byte[] processed = out.toByteArray();

            // Keep the processed bytes when we downscaled, or when re-encoding actually shrank the
            // file. Otherwise the original is already efficient, so avoid a needless recompression.
            if (processed.length > 0 && (needsResize || processed.length < data.length)) {
                return new ProcessedImage(processed, outContentType);
            }
            return new ProcessedImage(data, contentType);
        } catch (Exception e) {
            log.warn("Image processing failed ({} bytes, type {}); storing original bytes",
                    data.length, contentType, e);
            return new ProcessedImage(data, contentType);
        }
    }
}
