package server.sassedo.promotion.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import server.sassedo.promotion.data.dto.PromotionPackage;

import java.util.List;

@Repository
public interface PromotionPackageRepository extends JpaRepository<PromotionPackage, Long> {

    List<PromotionPackage> findByActiveTrueOrderBySortPriorityDescPriceCentsAsc();

    List<PromotionPackage> findAllByOrderBySortPriorityDescPriceCentsAsc();
}
