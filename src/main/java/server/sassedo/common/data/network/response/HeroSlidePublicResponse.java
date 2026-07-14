package server.sassedo.common.data.network.response;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class HeroSlidePublicResponse {
    private Long id;
    private String title;
    private String subtitle;
    private String primaryCtaLabel;
    private String primaryCtaHref;
    private String secondaryCtaLabel;
    private String secondaryCtaHref;
    private String backgroundImageUrl;
}
