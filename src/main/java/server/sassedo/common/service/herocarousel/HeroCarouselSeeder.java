package server.sassedo.common.service.herocarousel;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import server.sassedo.common.data.dto.HeroCarouselSettings;
import server.sassedo.common.data.dto.HeroSlide;
import server.sassedo.common.repository.HeroCarouselSettingsRepository;
import server.sassedo.common.repository.HeroSlideRepository;

/**
 * Seeds the default homepage hero carousel (badge + two slides, no background images) on first
 * startup so a fresh deployment reproduces the previously hardcoded design. Idempotent: skips
 * seeding when slides already exist.
 */
@Service
@RequiredArgsConstructor
public class HeroCarouselSeeder {

    private final HeroSlideRepository heroSlideRepository;
    private final HeroCarouselSettingsRepository heroCarouselSettingsRepository;

    @PostConstruct
    public void init() {
        seedSettings();
        seedSlides();
    }

    private void seedSettings() {
        if (heroCarouselSettingsRepository.count() > 0) {
            return;
        }
        HeroCarouselSettings settings = new HeroCarouselSettings();
        settings.setId(1L);
        settings.setBadgeBg("Първата българска платформа за квартири и съквартиранти в Европа");
        settings.setBadgeEn("The first Bulgarian platform for apartments and roommates in Europe");
        heroCarouselSettingsRepository.save(settings);
    }

    private void seedSlides() {
        if (heroSlideRepository.count() > 0) {
            return;
        }
        heroSlideRepository.save(slide(
                "Намери своя нов съквартирант",
                "Find your new roommate",
                "Свържи се с хора, които пасват на твоите навици, бюджет и град.",
                "Connect with people who match your habits, budget and city.",
                "Започни търсене", "Start searching", "/find-roommate",
                "Създай профил", "Create profile", "/register",
                1));
        heroSlideRepository.save(slide(
                "Намери перфектния дом под наем",
                "Find the perfect home to rent",
                "Разглеждай квартири, стаи и профили в един ясен поток за търсене.",
                "Browse apartments, rooms and profiles in one clear search flow.",
                "Започни търсене", "Start searching", "/find-roommate",
                "Създай профил", "Create profile", "/register",
                2));
    }

    private static HeroSlide slide(String titleBg, String titleEn, String subtitleBg, String subtitleEn,
                                   String primaryLabelBg, String primaryLabelEn, String primaryHref,
                                   String secondaryLabelBg, String secondaryLabelEn, String secondaryHref,
                                   int sortOrder) {
        HeroSlide slide = new HeroSlide();
        slide.setTitleBg(titleBg);
        slide.setTitleEn(titleEn);
        slide.setSubtitleBg(subtitleBg);
        slide.setSubtitleEn(subtitleEn);
        slide.setPrimaryCtaLabelBg(primaryLabelBg);
        slide.setPrimaryCtaLabelEn(primaryLabelEn);
        slide.setPrimaryCtaHref(primaryHref);
        slide.setSecondaryCtaLabelBg(secondaryLabelBg);
        slide.setSecondaryCtaLabelEn(secondaryLabelEn);
        slide.setSecondaryCtaHref(secondaryHref);
        slide.setSortOrder(sortOrder);
        slide.setEnabled(true);
        return slide;
    }
}
