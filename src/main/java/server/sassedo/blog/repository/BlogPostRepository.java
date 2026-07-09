package server.sassedo.blog.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import server.sassedo.blog.data.dto.BlogPost;

import java.util.Optional;

public interface BlogPostRepository extends JpaRepository<BlogPost, Long> {

    Optional<BlogPost> findBySlug(String slug);

    Page<BlogPost> findByPublishedTrue(Pageable pageable);

    boolean existsBySlug(String slug);
}
