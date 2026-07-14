package server.sassedo.common.data.network.response;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class HeroCarouselSettingsResponse {
    private String badgeBg;
    private String badgeEn;

    public HeroCarouselSettingsResponse(String badgeBg, String badgeEn) {
        this.badgeBg = badgeBg;
        this.badgeEn = badgeEn;
    }
}
