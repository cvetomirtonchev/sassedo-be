package server.sassedo.blog.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import server.sassedo.blog.data.dto.BlogImage;
import server.sassedo.blog.data.dto.BlogPost;
import server.sassedo.blog.data.network.request.BlogPostRequest;
import server.sassedo.blog.data.network.response.BlogImageUploadResponse;
import server.sassedo.blog.service.BlogService;
import server.sassedo.common.data.network.response.PageMeta;
import server.sassedo.common.data.network.response.PagedResponse;
import server.sassedo.model.GenericException;
import server.sassedo.security.jwt.JwtUtils;
import server.sassedo.utils.ImageResponses;

import java.io.IOException;

import static server.sassedo.utils.ServerUtils.getUserId;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/blog")
@RequiredArgsConstructor
public class BlogController {

    private final BlogService blogService;
    private final BlogMapper mapper;
    private final JwtUtils jwtUtils;

    @GetMapping
    public ResponseEntity<?> browse(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size) {
        Page<BlogPost> posts = blogService.getPublished(
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "publishedAt")));
        return ResponseEntity.ok(toSummaryPagedResponse(posts));
    }

    @GetMapping("/images/{imageId}")
    public ResponseEntity<byte[]> getImage(@PathVariable Long imageId,
            @RequestHeader(value = HttpHeaders.IF_NONE_MATCH, required = false) String ifNoneMatch) {
        try {
            return imageResponse(blogService.getImage(imageId), ifNoneMatch);
        } catch (GenericException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/{slug}")
    public ResponseEntity<?> getBySlug(@PathVariable String slug) {
        try {
            BlogPost post = blogService.getPublishedBySlug(slug);
            return ResponseEntity.ok(mapper.map(post));
        } catch (GenericException e) {
            return ResponseEntity.badRequest().body(e.getErrorResponse());
        }
    }

    @GetMapping("/admin/all")
    public ResponseEntity<?> adminAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "40") int size) {
        Page<BlogPost> posts = blogService.adminGetAll(
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")));
        return ResponseEntity.ok(toSummaryPagedResponse(posts));
    }

    @GetMapping("/admin/{id}")
    public ResponseEntity<?> adminGetById(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(mapper.map(blogService.getById(id)));
        } catch (GenericException e) {
            return ResponseEntity.badRequest().body(e.getErrorResponse());
        }
    }

    @PostMapping("/admin")
    public ResponseEntity<?> create(@Valid @RequestBody BlogPostRequest request, HttpServletRequest httpRequest) {
        Long userId = getUserId(httpRequest, jwtUtils);
        try {
            BlogPost post = blogService.create(userId, request);
            return ResponseEntity.ok(mapper.map(post));
        } catch (GenericException e) {
            return ResponseEntity.badRequest().body(e.getErrorResponse());
        }
    }

    @PutMapping("/admin/{id}")
    public ResponseEntity<?> update(@PathVariable Long id, @Valid @RequestBody BlogPostRequest request) {
        try {
            BlogPost post = blogService.update(id, request);
            return ResponseEntity.ok(mapper.map(post));
        } catch (GenericException e) {
            return ResponseEntity.badRequest().body(e.getErrorResponse());
        }
    }

    @DeleteMapping("/admin/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id) {
        try {
            blogService.delete(id);
            return ResponseEntity.ok().build();
        } catch (GenericException e) {
            return ResponseEntity.badRequest().body(e.getErrorResponse());
        }
    }

    @PostMapping(value = "/admin/images", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> uploadImage(@RequestParam("file") MultipartFile file) {
        try {
            BlogImage image = blogService.addImage(file);
            return ResponseEntity.ok(new BlogImageUploadResponse(image.getId(), mapper.buildImageUrl(image.getId())));
        } catch (GenericException e) {
            return ResponseEntity.badRequest().body(e.getErrorResponse());
        } catch (IOException e) {
            return ResponseEntity.badRequest().body("Could not read uploaded file");
        }
    }

    private PagedResponse<?> toSummaryPagedResponse(Page<BlogPost> posts) {
        return new PagedResponse<>(
                posts.getContent().stream().map(mapper::mapSummary).toList(),
                new PageMeta(posts.getNumber(), posts.getTotalPages(), posts.getTotalElements()));
    }

    private ResponseEntity<byte[]> imageResponse(BlogImage image, String ifNoneMatch) {
        if (image.getData() == null || image.getData().length == 0) {
            return ResponseEntity.notFound().build();
        }
        MediaType mediaType = MediaType.IMAGE_JPEG;
        if (image.getContentType() != null) {
            try {
                mediaType = MediaType.parseMediaType(image.getContentType());
            } catch (Exception ignored) {
                // fall back to jpeg
            }
        }
        return ImageResponses.cached(image.getData(), mediaType, ifNoneMatch);
    }
}
