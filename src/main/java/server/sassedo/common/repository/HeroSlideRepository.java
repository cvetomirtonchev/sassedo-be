package server.sassedo.common.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import server.sassedo.common.data.dto.HeroSlide;

import java.util.List;

public interface HeroSlideRepository extends JpaRepository<HeroSlide, Long> {

    List<HeroSlide> findAllByOrderBySortOrderAscIdAsc();

    List<HeroSlide> findAllByEnabledTrueOrderBySortOrderAscIdAsc();
}
