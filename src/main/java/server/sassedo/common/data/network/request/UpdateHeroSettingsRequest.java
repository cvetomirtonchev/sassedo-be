package server.sassedo.common.data.network.request;

import jakarta.validation.constraints.Size;

public class UpdateHeroSettingsRequest {

    @Size(max = 500)
    private String badgeBg;

    @Size(max = 500)
    private String badgeEn;

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
