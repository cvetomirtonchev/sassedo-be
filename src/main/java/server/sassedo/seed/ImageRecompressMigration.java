package server.sassedo.seed;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import server.sassedo.blog.data.dto.BlogImage;
import server.sassedo.common.data.dto.HeroSlideImage;
import server.sassedo.listing.rental.data.dto.RentalListingPhoto;
import server.sassedo.listing.roommate.data.dto.RoommateListingPhoto;
import server.sassedo.location.data.dto.City;
import server.sassedo.user.data.dto.User;
import server.sassedo.utils.ImageProcessor;

import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Function;

/**
 * One-time maintenance job that recompresses image blobs already stored in the database, so legacy
 * uploads (notably multi-MB profile photos that caused the heap OOM) are shrunk in place with the
 * same {@link ImageProcessor} presets now applied on upload.
 *
 * <p>Guarded by {@code sassedo.shrink.large.images=true} (env {@code SASSEDO_SHRINK_LARGE_IMAGES=true}),
 * default off. It streams row-by-row: it first fetches only the ids, then loads, recompresses,
 * flushes and detaches a single row at a time, so peak memory is one image — never the whole table
 * (loading every blob at once is exactly what OOMs a small heap). {@link ImageProcessor} returns the
 * original bytes when recompression would not help, so the job is idempotent and safe to re-run.
 * Run it once, then set the flag back to false.
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "sassedo.shrink.large.images", havingValue = "true")
public class ImageRecompressMigration implements ApplicationRunner {

    /** Only recompress blobs larger than this; smaller images are already efficient. */
    private static final int LARGE_THRESHOLD_BYTES = 400 * 1024;

    @PersistenceContext
    private EntityManager em;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        log.info("[image-recompress] starting one-time recompression of oversized image blobs");

        int profile = process(User.class, "User", User::getProfilePhoto,
                (u, r) -> u.setProfilePhoto(r.data()), ImageProcessor.Preset.PROFILE);

        int rental = process(RentalListingPhoto.class, "RentalListingPhoto", RentalListingPhoto::getData,
                (p, r) -> { p.setData(r.data()); p.setContentType(r.contentType()); }, ImageProcessor.Preset.LISTING);

        int roommate = process(RoommateListingPhoto.class, "RoommateListingPhoto", RoommateListingPhoto::getData,
                (p, r) -> { p.setData(r.data()); p.setContentType(r.contentType()); }, ImageProcessor.Preset.LISTING);

        int hero = process(HeroSlideImage.class, "HeroSlideImage", HeroSlideImage::getData,
                (h, r) -> { h.setData(r.data()); h.setContentType(r.contentType()); }, ImageProcessor.Preset.HERO);

        int blog = process(BlogImage.class, "BlogImage", BlogImage::getData,
                (b, r) -> { b.setData(r.data()); b.setContentType(r.contentType()); }, ImageProcessor.Preset.BLOG);

        int cities = process(City.class, "City", City::getImage,
                (c, r) -> { c.setImage(r.data()); c.setImageContentType(r.contentType()); }, ImageProcessor.Preset.CITY);

        log.info("[image-recompress] done. recompressed: profile={}, rental={}, roommate={}, hero={}, blog={}, cities={}",
                profile, rental, roommate, hero, blog, cities);
    }

    /**
     * Streams every row of an entity by id, recompressing blobs above the threshold and flushing +
     * detaching each row before moving on so only one image is ever held in memory. Returns how many
     * rows were rewritten.
     */
    private <T> int process(Class<T> type, String entityName, Function<T, byte[]> blobGetter,
            BiConsumer<T, ImageProcessor.ProcessedImage> apply, ImageProcessor.Preset preset) {
        List<Long> ids = em.createQuery("select e.id from " + entityName + " e", Long.class).getResultList();
        int changed = 0;
        for (Long id : ids) {
            T entity = em.find(type, id);
            if (entity != null) {
                byte[] data = blobGetter.apply(entity);
                if (data != null && data.length > LARGE_THRESHOLD_BYTES) {
                    ImageProcessor.ProcessedImage processed = ImageProcessor.process(data, null, preset);
                    if (processed.data() != null && processed.data().length < data.length) {
                        apply.accept(entity, processed);
                        em.flush();
                        changed++;
                    }
                }
            }
            // Detach everything (including any freshly loaded blob) to keep peak memory to one row.
            em.clear();
        }
        return changed;
    }
}
