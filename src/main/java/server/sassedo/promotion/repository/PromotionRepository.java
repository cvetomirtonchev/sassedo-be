package server.sassedo.promotion.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import server.sassedo.promotion.common.ListingType;
import server.sassedo.promotion.common.PromotionStatus;
import server.sassedo.promotion.data.dto.Promotion;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface PromotionRepository extends JpaRepository<Promotion, Long> {

    List<Promotion> findByOwnerIdOrderByCreatedAtDesc(Long ownerId);

    List<Promotion> findByListingTypeAndListingIdOrderByCreatedAtDesc(ListingType listingType, Long listingId);

    List<Promotion> findByListingTypeAndListingIdAndStatusIn(ListingType listingType, Long listingId,
                                                             List<PromotionStatus> statuses);

    List<Promotion> findByStatusAndStartDateLessThanEqual(PromotionStatus status, LocalDateTime now);

    List<Promotion> findByStatusAndEndDateLessThanEqual(PromotionStatus status, LocalDateTime now);

    @Query("SELECT p FROM Promotion p WHERE " +
            "(:status IS NULL OR p.status = :status) " +
            "AND (:listingType IS NULL OR p.listingType = :listingType)")
    Page<Promotion> adminSearch(@Param("status") PromotionStatus status,
                                @Param("listingType") ListingType listingType,
                                Pageable pageable);
}
