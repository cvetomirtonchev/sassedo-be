package server.sassedo.blog.data.network.response;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class BlogPostSummaryResponse {
    private Long id;
    private String title;
    private String slug;
    private String excerpt;
    private String thumbnailUrl;
    private boolean published;
    private Long authorId;
    private String authorName;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime publishedAt;
}
