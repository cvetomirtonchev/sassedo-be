package server.sassedo.common.data.dto;

import jakarta.persistence.*;

/**
 * Global hero carousel settings (single row). Currently holds the bilingual badge text
 * rendered above the slides. Resolved per locale with fallback like {@link Faq}.
 */
@Entity
@Table(name = "hero_carousel_settings")
public class HeroCarouselSettings {

    @Id
    private Long id = 1L;

    @Column(name = "badge_bg", length = 500)
    private String badgeBg;

    @Column(name = "badge_en", length = 500)
    private String badgeEn;

    public HeroCarouselSettings() {
    }

    public String getBadge(FaqLocale locale) {
        if (locale == FaqLocale.EN) {
            return badgeEn != null && !badgeEn.isBlank() ? badgeEn : badgeBg;
        }
        return badgeBg != null && !badgeBg.isBlank() ? badgeBg : badgeEn;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getBadgeBg() {
        return badgeBg;
    }

    public void setBadgeBg(String badgeBg) {
        this.badgeBg = badgeBg;
    }

    public String getBadgeEn() {
        return badgeEn;
    }

    public void setBadgeEn(String badgeEn) {
        this.badgeEn = badgeEn;
    }
}
