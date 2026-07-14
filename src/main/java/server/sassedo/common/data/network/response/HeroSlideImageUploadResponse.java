package server.sassedo.common.data.network.response;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class HeroSlideImageUploadResponse {
    private Long imageId;
    private String url;

    public HeroSlideImageUploadResponse(Long imageId, String url) {
        this.imageId = imageId;
        this.url = url;
    }
}
