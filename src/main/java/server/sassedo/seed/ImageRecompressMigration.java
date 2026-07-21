package server.sassedo.seed;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import server.sassedo.blog.data.dto.BlogImage;
import server.sassedo.blog.repository.BlogImageRepository;
import server.sassedo.common.data.dto.HeroSlideImage;
import server.sassedo.common.repository.HeroSlideImageRepository;
import server.sassedo.listing.rental.data.dto.RentalListingPhoto;
import server.sassedo.listing.rental.repository.RentalListingPhotoRepository;
import server.sassedo.listing.roommate.data.dto.RoommateListingPhoto;
import server.sassedo.listing.roommate.repository.RoommateListingPhotoRepository;
import server.sassedo.location.data.dto.City;
import server.sassedo.location.repository.CityRepository;
import server.sassedo.user.data.dto.User;
import server.sassedo.user.repository.UserRepository;
import server.sassedo.utils.ImageProcessor;

import java.util.function.Function;

/**
 * One-time maintenance job that recompresses image blobs already stored in the database, so legacy
 * uploads (notably multi-MB profile photos that caused the heap OOM) are shrunk in place with the
 * same {@link ImageProcessor} presets now applied on upload.
 *
 * <p>Guarded by {@code sassedo.shrink.large.images=true} (env {@code SASSEDO_SHRINK_LARGE_IMAGES=true}),
 * default off. Only blobs above {@link #LARGE_THRESHOLD_BYTES} are touched, and {@link ImageProcessor}
 * returns the original bytes when recompression would not help, so the job is safe to re-run
 * (idempotent) and leaves already-small images untouched. Run it once, then unset the flag.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "sassedo.shrink.large.images", havingValue = "true")
public class ImageRecompressMigration implements ApplicationRunner {

    /** Only recompress blobs larger than this; smaller images are already efficient. */
    private static final int LARGE_THRESHOLD_BYTES = 400 * 1024;

    private final UserRepository userRepository;
    private final RentalListingPhotoRepository rentalPhotoRepository;
    private final RoommateListingPhotoRepository roommatePhotoRepository;
    private final HeroSlideImageRepository heroImageRepository;
    private final BlogImageRepository blogImageRepository;
    private final CityRepository cityRepository;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        log.info("[image-recompress] starting one-time recompression of oversized image blobs");

        int users = recompress(userRepository, User::getProfilePhoto, (u, r) -> {
            u.setProfilePhoto(r.data());
        }, ImageProcessor.Preset.PROFILE);

        int rental = recompress(rentalPhotoRepository, RentalListingPhoto::getData, (p, r) -> {
            p.setData(r.data());
            p.setContentType(r.contentType());
        }, ImageProcessor.Preset.LISTING);

        int roommate = recompress(roommatePhotoRepository, RoommateListingPhoto::getData, (p, r) -> {
            p.setData(r.data());
            p.setContentType(r.contentType());
        }, ImageProcessor.Preset.LISTING);

        int hero = recompress(heroImageRepository, HeroSlideImage::getData, (h, r) -> {
            h.setData(r.data());
            h.setContentType(r.contentType());
        }, ImageProcessor.Preset.HERO);

        int blog = recompress(blogImageRepository, BlogImage::getData, (b, r) -> {
            b.setData(r.data());
            b.setContentType(r.contentType());
        }, ImageProcessor.Preset.BLOG);

        int cities = recompress(cityRepository, City::getImage, (c, r) -> {
            c.setImage(r.data());
            c.setImageContentType(r.contentType());
        }, ImageProcessor.Preset.CITY);

        log.info("[image-recompress] done. recompressed: profile={}, rental={}, roommate={}, hero={}, blog={}, cities={}",
                users, rental, roommate, hero, blog, cities);
    }

    /**
     * Walks every row of a repository, recompresses blobs above the threshold with the given preset,
     * and saves the ones that actually shrank. Returns how many rows were rewritten.
     */
    private <T, R extends org.springframework.data.repository.CrudRepository<T, ?>> int recompress(
            R repository,
            Function<T, byte[]> blobGetter,
            java.util.function.BiConsumer<T, ImageProcessor.ProcessedImage> apply,
            ImageProcessor.Preset preset) {
        int changed = 0;
        for (T entity : repository.findAll()) {
            byte[] data = blobGetter.apply(entity);
            if (data == null || data.length <= LARGE_THRESHOLD_BYTES) {
                continue;
            }
            ImageProcessor.ProcessedImage processed = ImageProcessor.process(data, null, preset);
            if (processed.data() != null && processed.data().length < data.length) {
                apply.accept(entity, processed);
                repository.save(entity);
                changed++;
            }
        }
        return changed;
    }
}
