package server.sassedo.blog.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import server.sassedo.blog.data.dto.BlogPost;
import server.sassedo.blog.data.network.response.BlogPostResponse;
import server.sassedo.blog.data.network.response.BlogPostSummaryResponse;
import server.sassedo.model.GenericException;
import server.sassedo.user.data.dto.User;
import server.sassedo.user.service.user.UserService;

import java.util.ArrayList;
import java.util.stream.Collectors;

/**
 * Entity -> response mapping for blog posts (repo convention: manual mapping, no MapStruct).
 */
@Component
@RequiredArgsConstructor
public class BlogMapper {

    private final UserService userService;

    public BlogPostResponse map(BlogPost post) {
        BlogPostResponse r = new BlogPostResponse();
        r.setId(post.getId());
        r.setTitle(post.getTitle());
        r.setSlug(post.getSlug());
        r.setExcerpt(post.getExcerpt());
        r.setContentHtml(post.getContentHtml());
        r.setCoverImageIds(new ArrayList<>(post.getCoverImageIds()));
        r.setCoverImageUrls(post.getCoverImageIds().stream()
                .map(this::buildImageUrl)
                .collect(Collectors.toList()));
        r.setPublished(post.isPublished());
        r.setAuthorId(post.getAuthorId());
        r.setAuthorName(resolveAuthorName(post.getAuthorId()));
        r.setCreatedAt(post.getCreatedAt());
        r.setUpdatedAt(post.getUpdatedAt());
        r.setPublishedAt(post.getPublishedAt());
        return r;
    }

    public BlogPostSummaryResponse mapSummary(BlogPost post) {
        BlogPostSummaryResponse r = new BlogPostSummaryResponse();
        r.setId(post.getId());
        r.setTitle(post.getTitle());
        r.setSlug(post.getSlug());
        r.setExcerpt(post.getExcerpt());
        r.setThumbnailUrl(post.getCoverImageIds().isEmpty()
                ? null
                : buildImageUrl(post.getCoverImageIds().get(0)));
        r.setPublished(post.isPublished());
        r.setAuthorId(post.getAuthorId());
        r.setAuthorName(resolveAuthorName(post.getAuthorId()));
        r.setCreatedAt(post.getCreatedAt());
        r.setUpdatedAt(post.getUpdatedAt());
        r.setPublishedAt(post.getPublishedAt());
        return r;
    }

    public String buildImageUrl(Long imageId) {
        if (imageId == null) {
            return null;
        }
        return ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/api/blog/images/")
                .path(String.valueOf(imageId))
                .toUriString();
    }

    private String resolveAuthorName(Long authorId) {
        if (authorId == null) {
            return null;
        }
        try {
            User author = userService.getUserById(authorId);
            return author.getName();
        } catch (GenericException ignored) {
            return null;
        }
    }
}
