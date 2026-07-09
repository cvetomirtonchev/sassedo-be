package server.sassedo.blog.data.network.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class BlogPostRequest {

    @NotBlank
    private String title;

    private String slug;

    private String excerpt;

    private String contentHtml;

    private List<Long> coverImageIds;

    private Boolean published;
}
