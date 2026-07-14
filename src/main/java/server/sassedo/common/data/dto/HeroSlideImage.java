package server.sassedo.common.data.dto;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Background image bytes for a hero carousel slide, stored as a BLOB in MySQL
 * (mirrors the blog image storage convention). Served publicly by id.
 */
@Getter
@Setter
@Entity
@Table(name = "hero_carousel_images")
public class HeroSlideImage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Lob
    @Column(columnDefinition = "MEDIUMBLOB", nullable = false)
    private byte[] data;

    @Column(name = "content_type")
    private String contentType;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    public void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
