package server.sassedo.blog.data.network.response;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
public class BlogPostResponse {
    private Long id;
    private String title;
    private String slug;
    private String excerpt;
    private String contentHtml;
    private List<Long> coverImageIds;
    private List<String> coverImageUrls;
    private boolean published;
    private Long authorId;
    private String authorName;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime publishedAt;
}
