package server.sassedo.utils;

import net.coobird.thumbnailator.Thumbnails;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import javax.imageio.ImageReadParam;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Iterator;

/**
 * Downscales and re-encodes uploaded images so stored blobs stay small without a visible loss of
 * quality. This is the main defence against the profile-photo OOM: raw phone uploads were stored
 * verbatim (up to ~4&nbsp;MB / tens of megapixels), and loading/decoding those on a 512&nbsp;MB heap
 * exhausted memory.
 *
 * <p>Rules that protect quality and memory:
 * <ul>
 *     <li><b>Bounded decode:</b> the source is decoded with ImageIO subsampling so a huge photo is
 *     never expanded to a full-resolution bitmap in heap (a 25&nbsp;MP JPEG would otherwise need
 *     ~100&nbsp;MB just to decode). We decode to roughly twice the target size, then scale precisely.</li>
 *     <li>Only ever downscale (never upscale); aspect ratio is preserved.</li>
 *     <li>EXIF orientation (JPEG) is read from the bytes and re-applied so portrait phone photos are
 *     not rotated.</li>
 *     <li>Images with an alpha channel are re-encoded as PNG (lossless); everything else becomes
 *     JPEG at the preset quality (~0.85 is visually near-lossless but far smaller).</li>
 *     <li>On any failure the original bytes are returned unchanged, so an upload is never rejected
 *     because of processing.</li>
 * </ul>
 */
public final class ImageProcessor {

    private static final Logger log = LoggerFactory.getLogger(ImageProcessor.class);

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

    public record ProcessedImage(byte[] data, String contentType) {
    }

    private ImageProcessor() {
    }

    public static ProcessedImage process(byte[] data, String contentType, Preset preset) {
        if (data == null || data.length == 0) {
            return new ProcessedImage(data, contentType);
        }
        try {
            BufferedImage source = decodeBounded(data, preset.maxDimension);
            if (source == null) {
                // No ImageIO reader could decode it (e.g. an exotic format). Keep the original.
                return new ProcessedImage(data, contentType);
            }

            source = applyOrientation(source, readExifOrientation(data));

            boolean hasAlpha = source.getColorModel().hasAlpha();
            int longestSide = Math.max(source.getWidth(), source.getHeight());
            boolean needsResize = longestSide > preset.maxDimension;

            String outFormat = hasAlpha ? "png" : "jpg";
            String outContentType = hasAlpha ? "image/png" : "image/jpeg";

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            Thumbnails.Builder<BufferedImage> builder = Thumbnails.of(source).outputFormat(outFormat);
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

            // Keep the processed bytes when we downscaled/rotated, or when re-encoding actually shrank
            // the file; otherwise the original is already efficient.
            if (processed.length > 0 && (needsResize || processed.length < data.length)) {
                return new ProcessedImage(processed, outContentType);
            }
            return new ProcessedImage(data, contentType);
        } catch (Throwable e) {
            log.warn("Image processing failed ({} bytes, type {}); storing original bytes",
                    data.length, contentType, e);
            return new ProcessedImage(data, contentType);
        }
    }

    /**
     * Decodes the image using source subsampling so the in-heap bitmap is bounded: we aim for a
     * decoded longest side of roughly {@code 2 x maxDimension}, which leaves quality headroom for the
     * final scale while never materialising the full-resolution image.
     */
    private static BufferedImage decodeBounded(byte[] data, int maxDimension) {
        try (ImageInputStream iis = ImageIO.createImageInputStream(new ByteArrayInputStream(data))) {
            if (iis == null) {
                return null;
            }
            Iterator<ImageReader> readers = ImageIO.getImageReaders(iis);
            if (!readers.hasNext()) {
                return null;
            }
            ImageReader reader = readers.next();
            try {
                reader.setInput(iis, true, true);
                int width = reader.getWidth(0);
                int height = reader.getHeight(0);
                int longest = Math.max(width, height);
                int target = Math.max(maxDimension * 2, maxDimension);

                ImageReadParam param = reader.getDefaultReadParam();
                if (longest > target) {
                    int subsampling = Math.max(1, longest / target);
                    param.setSourceSubsampling(subsampling, subsampling, 0, 0);
                }
                return reader.read(0, param);
            } finally {
                reader.dispose();
            }
        } catch (Exception e) {
            log.warn("Bounded decode failed; falling back to no processing", e);
            return null;
        }
    }

    /** Rotates the image to compensate for the given EXIF orientation (1..8). No-op for 1/unknown. */
    private static BufferedImage applyOrientation(BufferedImage image, int orientation) {
        try {
            return switch (orientation) {
                case 3 -> rotate(image, 180);
                case 6 -> rotate(image, 90);
                case 8 -> rotate(image, 270);
                default -> image; // 1 (normal), or mirrored variants we intentionally leave as-is
            };
        } catch (Exception e) {
            return image;
        }
    }

    private static BufferedImage rotate(BufferedImage image, int degrees) throws java.io.IOException {
        return Thumbnails.of(image).scale(1.0).rotate(degrees).asBufferedImage();
    }

    /**
     * Reads the EXIF orientation tag (0x0112) from a JPEG's APP1 segment. Returns 1 (normal) when the
     * image is not a JPEG, has no EXIF, or cannot be parsed. Intentionally minimal — enough to fix the
     * common phone-photo rotation cases without pulling in a metadata library.
     */
    private static int readExifOrientation(byte[] d) {
        try {
            if (d.length < 4 || (d[0] & 0xFF) != 0xFF || (d[1] & 0xFF) != 0xD8) {
                return 1; // not a JPEG
            }
            int offset = 2;
            while (offset + 4 <= d.length) {
                if ((d[offset] & 0xFF) != 0xFF) {
                    return 1;
                }
                int marker = d[offset + 1] & 0xFF;
                int segLength = ((d[offset + 2] & 0xFF) << 8) | (d[offset + 3] & 0xFF);
                if (marker == 0xE1 && offset + 4 + 6 <= d.length
                        && d[offset + 4] == 'E' && d[offset + 5] == 'x' && d[offset + 6] == 'i'
                        && d[offset + 7] == 'f' && d[offset + 8] == 0) {
                    return parseExifOrientation(d, offset + 10, offset + 2 + segLength);
                }
                if (marker == 0xDA) {
                    return 1; // start of scan; no EXIF found
                }
                offset += 2 + segLength;
            }
            return 1;
        } catch (Exception e) {
            return 1;
        }
    }

    private static int parseExifOrientation(byte[] d, int tiffStart, int limit) {
        boolean littleEndian = (d[tiffStart] & 0xFF) == 0x49;
        int ifdOffset = readInt(d, tiffStart + 4, littleEndian);
        int ifd = tiffStart + ifdOffset;
        if (ifd + 2 > limit) {
            return 1;
        }
        int entries = readShort(d, ifd, littleEndian);
        for (int i = 0; i < entries; i++) {
            int entry = ifd + 2 + i * 12;
            if (entry + 12 > limit) {
                break;
            }
            int tag = readShort(d, entry, littleEndian);
            if (tag == 0x0112) {
                return readShort(d, entry + 8, littleEndian);
            }
        }
        return 1;
    }

    private static int readShort(byte[] d, int offset, boolean littleEndian) {
        int b0 = d[offset] & 0xFF;
        int b1 = d[offset + 1] & 0xFF;
        return littleEndian ? (b1 << 8) | b0 : (b0 << 8) | b1;
    }

    private static int readInt(byte[] d, int offset, boolean littleEndian) {
        int b0 = d[offset] & 0xFF;
        int b1 = d[offset + 1] & 0xFF;
        int b2 = d[offset + 2] & 0xFF;
        int b3 = d[offset + 3] & 0xFF;
        return littleEndian ? (b3 << 24) | (b2 << 16) | (b1 << 8) | b0
                : (b0 << 24) | (b1 << 16) | (b2 << 8) | b3;
    }
}
