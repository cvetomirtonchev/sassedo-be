package server.sassedo.common.service.herocarousel;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import server.sassedo.common.data.dto.HeroCarouselSettings;
import server.sassedo.common.data.dto.HeroSlide;
import server.sassedo.common.data.dto.HeroSlideImage;
import server.sassedo.common.data.network.request.AddHeroSlideRequest;
import server.sassedo.common.data.network.request.UpdateHeroSettingsRequest;
import server.sassedo.common.data.network.request.UpdateHeroSlideRequest;
import server.sassedo.common.repository.HeroCarouselSettingsRepository;
import server.sassedo.common.repository.HeroSlideImageRepository;
import server.sassedo.common.repository.HeroSlideRepository;
import server.sassedo.model.GenericException;
import server.sassedo.model.GenericExceptionCode;
import server.sassedo.utils.ImageProcessor;
import server.sassedo.utils.ImageUploadValidator;

import java.io.IOException;
import java.util.List;

@Service
@RequiredArgsConstructor
public class HeroCarouselServiceImpl implements HeroCarouselService {

    private static final Long SETTINGS_ID = 1L;

    private final HeroSlideRepository heroSlideRepository;
    private final HeroSlideImageRepository heroSlideImageRepository;
    private final HeroCarouselSettingsRepository heroCarouselSettingsRepository;

    @Override
    public HeroCarouselSettings getSettings() {
        return heroCarouselSettingsRepository.findById(SETTINGS_ID)
                .orElseGet(() -> {
                    HeroCarouselSettings settings = new HeroCarouselSettings();
                    settings.setId(SETTINGS_ID);
                    return settings;
                });
    }

    @Override
    @Transactional
    public HeroCarouselSettings updateSettings(UpdateHeroSettingsRequest request) {
        HeroCarouselSettings settings = getSettings();
        if (request.getBadgeBg() != null) {
            settings.setBadgeBg(request.getBadgeBg().trim());
        }
        if (request.getBadgeEn() != null) {
            settings.setBadgeEn(request.getBadgeEn().trim());
        }
        return heroCarouselSettingsRepository.save(settings);
    }

    @Override
    public List<HeroSlide> getAllOrdered() {
        return heroSlideRepository.findAllByOrderBySortOrderAscIdAsc();
    }

    @Override
    public List<HeroSlide> getEnabledOrdered() {
        return heroSlideRepository.findAllByEnabledTrueOrderBySortOrderAscIdAsc();
    }

    @Override
    public HeroSlide getById(Long id) throws GenericException {
        return heroSlideRepository.findById(id)
                .orElseThrow(() -> new GenericException(GenericExceptionCode.HERO_SLIDE_NOT_FOUND, "Hero slide not found"));
    }

    @Override
    @Transactional
    public HeroSlide add(AddHeroSlideRequest request) throws GenericException {
        validateCtaUrl(request.getPrimaryCtaHref());
        validateOptionalCtaUrl(request.getSecondaryCtaHref());

        HeroSlide slide = new HeroSlide();
        slide.setTitleBg(trim(request.getTitleBg()));
        slide.setTitleEn(trim(request.getTitleEn()));
        slide.setSubtitleBg(trim(request.getSubtitleBg()));
        slide.setSubtitleEn(trim(request.getSubtitleEn()));
        slide.setPrimaryCtaLabelBg(trim(request.getPrimaryCtaLabelBg()));
        slide.setPrimaryCtaLabelEn(trim(request.getPrimaryCtaLabelEn()));
        slide.setPrimaryCtaHref(trim(request.getPrimaryCtaHref()));
        slide.setSecondaryCtaLabelBg(trim(request.getSecondaryCtaLabelBg()));
        slide.setSecondaryCtaLabelEn(trim(request.getSecondaryCtaLabelEn()));
        slide.setSecondaryCtaHref(trim(request.getSecondaryCtaHref()));
        slide.setSortOrder(request.getSortOrder());
        slide.setEnabled(request.getEnabled() == null || request.getEnabled());
        return heroSlideRepository.save(slide);
    }

    @Override
    @Transactional
    public HeroSlide update(UpdateHeroSlideRequest request) throws GenericException {
        HeroSlide slide = getById(request.getId());
        if (request.getTitleBg() != null) {
            slide.setTitleBg(request.getTitleBg().trim());
        }
        if (request.getTitleEn() != null) {
            slide.setTitleEn(request.getTitleEn().trim());
        }
        if (request.getSubtitleBg() != null) {
            slide.setSubtitleBg(request.getSubtitleBg().trim());
        }
        if (request.getSubtitleEn() != null) {
            slide.setSubtitleEn(request.getSubtitleEn().trim());
        }
        if (request.getPrimaryCtaLabelBg() != null) {
            slide.setPrimaryCtaLabelBg(request.getPrimaryCtaLabelBg().trim());
        }
        if (request.getPrimaryCtaLabelEn() != null) {
            slide.setPrimaryCtaLabelEn(request.getPrimaryCtaLabelEn().trim());
        }
        if (request.getPrimaryCtaHref() != null) {
            validateCtaUrl(request.getPrimaryCtaHref());
            slide.setPrimaryCtaHref(request.getPrimaryCtaHref().trim());
        }
        if (request.getSecondaryCtaLabelBg() != null) {
            slide.setSecondaryCtaLabelBg(request.getSecondaryCtaLabelBg().trim());
        }
        if (request.getSecondaryCtaLabelEn() != null) {
            slide.setSecondaryCtaLabelEn(request.getSecondaryCtaLabelEn().trim());
        }
        if (request.getSecondaryCtaHref() != null) {
            validateOptionalCtaUrl(request.getSecondaryCtaHref());
            slide.setSecondaryCtaHref(request.getSecondaryCtaHref().trim());
        }
        if (request.getSortOrder() != null) {
            slide.setSortOrder(request.getSortOrder());
        }
        if (request.getEnabled() != null) {
            slide.setEnabled(request.getEnabled());
        }
        return heroSlideRepository.save(slide);
    }

    @Override
    @Transactional
    public void delete(Long id) throws GenericException {
        HeroSlide slide = getById(id);
        Long imageId = slide.getBackgroundImageId();
        heroSlideRepository.delete(slide);
        if (imageId != null) {
            heroSlideImageRepository.deleteById(imageId);
        }
    }

    @Override
    @Transactional
    public HeroSlideImage setSlideImage(Long slideId, MultipartFile file) throws GenericException, IOException {
        HeroSlide slide = getById(slideId);
        if (file == null || file.isEmpty()) {
            throw new GenericException(GenericExceptionCode.INVALID_FILE, "Uploaded file is empty");
        }
        ImageUploadValidator.validate(file);

        ImageProcessor.ProcessedImage processed = ImageProcessor.process(
                file.getBytes(), file.getContentType(), ImageProcessor.Preset.HERO);
        HeroSlideImage image = new HeroSlideImage();
        image.setData(processed.data());
        image.setContentType(processed.contentType());
        image = heroSlideImageRepository.save(image);

        Long previousImageId = slide.getBackgroundImageId();
        slide.setBackgroundImageId(image.getId());
        heroSlideRepository.save(slide);

        if (previousImageId != null) {
            heroSlideImageRepository.deleteById(previousImageId);
        }
        return image;
    }

    @Override
    @Transactional
    public void removeSlideImage(Long slideId) throws GenericException {
        HeroSlide slide = getById(slideId);
        Long imageId = slide.getBackgroundImageId();
        if (imageId == null) {
            return;
        }
        slide.setBackgroundImageId(null);
        heroSlideRepository.save(slide);
        heroSlideImageRepository.deleteById(imageId);
    }

    @Override
    public HeroSlideImage getImage(Long id) throws GenericException {
        return heroSlideImageRepository.findById(id)
                .orElseThrow(() -> new GenericException(GenericExceptionCode.HERO_IMAGE_NOT_FOUND, "Hero image not found"));
    }

    private static String trim(String value) {
        return value == null ? null : value.trim();
    }

    /**
     * Only allow same-origin relative paths or absolute http(s) URLs. Rejects dangerous schemes
     * such as {@code javascript:} so an admin-configured button cannot inject script into the page.
     */
    private static void validateCtaUrl(String url) throws GenericException {
        if (url == null || url.isBlank()) {
            throw new GenericException(GenericExceptionCode.HERO_INVALID_CTA_URL, "Button link is required");
        }
        String trimmed = url.trim();
        boolean relative = trimmed.startsWith("/") && !trimmed.startsWith("//");
        boolean http = trimmed.startsWith("http://") || trimmed.startsWith("https://");
        if (!relative && !http) {
            throw new GenericException(GenericExceptionCode.HERO_INVALID_CTA_URL,
                    "Button link must be a relative path (starting with /) or an http(s) URL");
        }
    }

    private static void validateOptionalCtaUrl(String url) throws GenericException {
        if (url == null || url.isBlank()) {
            return;
        }
        validateCtaUrl(url);
    }
}
