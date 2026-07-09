package server.sassedo.blog.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;
import server.sassedo.blog.data.dto.BlogImage;
import server.sassedo.blog.data.dto.BlogPost;
import server.sassedo.blog.data.network.request.BlogPostRequest;
import server.sassedo.model.GenericException;

import java.io.IOException;

public interface BlogService {

    Page<BlogPost> getPublished(Pageable pageable);

    Page<BlogPost> adminGetAll(Pageable pageable);

    BlogPost getPublishedBySlug(String slug) throws GenericException;

    BlogPost getById(Long id) throws GenericException;

    BlogPost create(Long authorId, BlogPostRequest request) throws GenericException;

    BlogPost update(Long id, BlogPostRequest request) throws GenericException;

    void delete(Long id) throws GenericException;

    BlogImage addImage(MultipartFile file) throws GenericException, IOException;

    BlogImage getImage(Long id) throws GenericException;
}
