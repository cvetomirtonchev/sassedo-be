package server.sassedo.common.data.network.response;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class HeroSlideAdminResponse {
    private Long id;
    private String titleBg;
    private String titleEn;
    private String subtitleBg;
    private String subtitleEn;
    private String primaryCtaLabelBg;
    private String primaryCtaLabelEn;
    private String primaryCtaHref;
    private String secondaryCtaLabelBg;
    private String secondaryCtaLabelEn;
    private String secondaryCtaHref;
    private Long backgroundImageId;
    private String backgroundImageUrl;
    private int sortOrder;
    private boolean enabled;
}
