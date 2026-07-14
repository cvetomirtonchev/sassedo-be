package server.sassedo.common.data.dto;

import jakarta.persistence.*;

/**
 * A single homepage hero carousel slide. Text is stored bilingually (Bulgarian + English)
 * and resolved per locale with fallback, mirroring the {@link Faq} convention. Each slide may
 * optionally reference a background image; when none is set the frontend keeps its default gradient.
 */
@Entity
@Table(name = "hero_carousel_slides")
public class HeroSlide {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "title_bg", length = 500)
    private String titleBg;

    @Column(name = "title_en", length = 500)
    private String titleEn;

    @Column(name = "subtitle_bg", length = 1000)
    private String subtitleBg;

    @Column(name = "subtitle_en", length = 1000)
    private String subtitleEn;

    @Column(name = "primary_cta_label_bg", length = 200)
    private String primaryCtaLabelBg;

    @Column(name = "primary_cta_label_en", length = 200)
    private String primaryCtaLabelEn;

    @Column(name = "primary_cta_href", length = 500)
    private String primaryCtaHref;

    @Column(name = "secondary_cta_label_bg", length = 200)
    private String secondaryCtaLabelBg;

    @Column(name = "secondary_cta_label_en", length = 200)
    private String secondaryCtaLabelEn;

    @Column(name = "secondary_cta_href", length = 500)
    private String secondaryCtaHref;

    @Column(name = "background_image_id")
    private Long backgroundImageId;

    @Column(name = "sort_order")
    private int sortOrder;

    @Column(name = "enabled", nullable = false)
    private boolean enabled = true;

    public HeroSlide() {
    }

    public String getTitle(FaqLocale locale) {
        return resolve(titleBg, titleEn, locale);
    }

    public String getSubtitle(FaqLocale locale) {
        return resolve(subtitleBg, subtitleEn, locale);
    }

    public String getPrimaryCtaLabel(FaqLocale locale) {
        return resolve(primaryCtaLabelBg, primaryCtaLabelEn, locale);
    }

    public String getSecondaryCtaLabel(FaqLocale locale) {
        return resolve(secondaryCtaLabelBg, secondaryCtaLabelEn, locale);
    }

    private static String resolve(String bg, String en, FaqLocale locale) {
        if (locale == FaqLocale.EN) {
            return en != null && !en.isBlank() ? en : bg;
        }
        return bg != null && !bg.isBlank() ? bg : en;
    }

    public Long getId() {
        return id;
    }

    public String getTitleBg() {
        return titleBg;
    }

    public void setTitleBg(String titleBg) {
        this.titleBg = titleBg;
    }

    public String getTitleEn() {
        return titleEn;
    }

    public void setTitleEn(String titleEn) {
        this.titleEn = titleEn;
    }

    public String getSubtitleBg() {
        return subtitleBg;
    }

    public void setSubtitleBg(String subtitleBg) {
        this.subtitleBg = subtitleBg;
    }

    public String getSubtitleEn() {
        return subtitleEn;
    }

    public void setSubtitleEn(String subtitleEn) {
        this.subtitleEn = subtitleEn;
    }

    public String getPrimaryCtaLabelBg() {
        return primaryCtaLabelBg;
    }

    public void setPrimaryCtaLabelBg(String primaryCtaLabelBg) {
        this.primaryCtaLabelBg = primaryCtaLabelBg;
    }

    public String getPrimaryCtaLabelEn() {
        return primaryCtaLabelEn;
    }

    public void setPrimaryCtaLabelEn(String primaryCtaLabelEn) {
        this.primaryCtaLabelEn = primaryCtaLabelEn;
    }

    public String getPrimaryCtaHref() {
        return primaryCtaHref;
    }

    public void setPrimaryCtaHref(String primaryCtaHref) {
        this.primaryCtaHref = primaryCtaHref;
    }

    public String getSecondaryCtaLabelBg() {
        return secondaryCtaLabelBg;
    }

    public void setSecondaryCtaLabelBg(String secondaryCtaLabelBg) {
        this.secondaryCtaLabelBg = secondaryCtaLabelBg;
    }

    public String getSecondaryCtaLabelEn() {
        return secondaryCtaLabelEn;
    }

    public void setSecondaryCtaLabelEn(String secondaryCtaLabelEn) {
        this.secondaryCtaLabelEn = secondaryCtaLabelEn;
    }

    public String getSecondaryCtaHref() {
        return secondaryCtaHref;
    }

    public void setSecondaryCtaHref(String secondaryCtaHref) {
        this.secondaryCtaHref = secondaryCtaHref;
    }

    public Long getBackgroundImageId() {
        return backgroundImageId;
    }

    public void setBackgroundImageId(Long backgroundImageId) {
        this.backgroundImageId = backgroundImageId;
    }

    public int getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(int sortOrder) {
        this.sortOrder = sortOrder;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
}
