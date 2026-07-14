package server.sassedo.common.data.network.response;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class HeroCarouselPublicResponse {
    private String badge;
    private List<HeroSlidePublicResponse> slides;

    public HeroCarouselPublicResponse(String badge, List<HeroSlidePublicResponse> slides) {
        this.badge = badge;
        this.slides = slides;
    }
}
