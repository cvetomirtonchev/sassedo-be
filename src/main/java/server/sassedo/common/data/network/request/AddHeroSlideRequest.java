package server.sassedo.common.data.network.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class AddHeroSlideRequest {
    @NotBlank
    @Size(max = 500)
    private String titleBg;

    @NotBlank
    @Size(max = 500)
    private String titleEn;

    @Size(max = 1000)
    private String subtitleBg;

    @Size(max = 1000)
    private String subtitleEn;

    @NotBlank
    @Size(max = 200)
    private String primaryCtaLabelBg;

    @NotBlank
    @Size(max = 200)
    private String primaryCtaLabelEn;

    @NotBlank
    @Size(max = 500)
    private String primaryCtaHref;

    @Size(max = 200)
    private String secondaryCtaLabelBg;

    @Size(max = 200)
    private String secondaryCtaLabelEn;

    @Size(max = 500)
    private String secondaryCtaHref;

    @Min(0)
    private int sortOrder;

    private Boolean enabled;

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

    public int getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(int sortOrder) {
        this.sortOrder = sortOrder;
    }

    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }
}
