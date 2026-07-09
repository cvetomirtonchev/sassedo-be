package server.sassedo.blog.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import server.sassedo.blog.data.dto.BlogImage;

public interface BlogImageRepository extends JpaRepository<BlogImage, Long> {
}
