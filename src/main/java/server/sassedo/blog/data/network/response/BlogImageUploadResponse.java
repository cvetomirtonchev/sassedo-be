package server.sassedo.blog.data.network.response;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BlogImageUploadResponse {
    private Long id;
    private String url;

    public BlogImageUploadResponse(Long id, String url) {
        this.id = id;
        this.url = url;
    }
}
