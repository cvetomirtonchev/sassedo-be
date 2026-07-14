package server.sassedo.common.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import server.sassedo.common.data.dto.HeroCarouselSettings;

public interface HeroCarouselSettingsRepository extends JpaRepository<HeroCarouselSettings, Long> {
}
