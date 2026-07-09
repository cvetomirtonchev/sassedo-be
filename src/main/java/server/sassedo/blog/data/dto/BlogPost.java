package server.sassedo.blog.data.dto;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Entity
@Table(name = "blog_posts",
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_blog_post_slug", columnNames = "slug")
        },
        indexes = {
                @Index(name = "idx_blog_post_published", columnList = "published, published_at")
        })
public class BlogPost {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private String slug;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String excerpt;

    @Lob
    @Column(name = "content_html", columnDefinition = "LONGTEXT")
    private String contentHtml;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "blog_post_cover_images", joinColumns = @JoinColumn(name = "post_id"))
    @OrderColumn(name = "position")
    @Column(name = "image_id")
    private List<Long> coverImageIds = new ArrayList<>();

    @Column(nullable = false)
    private boolean published = false;

    @Column(name = "author_id")
    private Long authorId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "published_at")
    private LocalDateTime publishedAt;

    @PrePersist
    public void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = this.createdAt;
        if (this.published && this.publishedAt == null) {
            this.publishedAt = this.createdAt;
        }
    }

    @PreUpdate
    public void onUpdate() {
        this.updatedAt = LocalDateTime.now();
        if (this.published && this.publishedAt == null) {
            this.publishedAt = this.updatedAt;
        }
    }
}
