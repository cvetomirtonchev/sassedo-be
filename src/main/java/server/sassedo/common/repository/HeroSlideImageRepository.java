package server.sassedo.common.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import server.sassedo.common.data.dto.HeroSlideImage;

public interface HeroSlideImageRepository extends JpaRepository<HeroSlideImage, Long> {
}
