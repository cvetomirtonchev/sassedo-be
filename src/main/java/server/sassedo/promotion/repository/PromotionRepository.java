package server.sassedo.promotion.repository;

import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import server.sassedo.promotion.common.ListingType;
import server.sassedo.promotion.common.PromotionStatus;
import server.sassedo.promotion.data.dto.Promotion;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface PromotionRepository extends JpaRepository<Promotion, Long> {

    List<Promotion> findByOwnerIdOrderByCreatedAtDesc(Long ownerId);

    List<Promotion> findByListingTypeAndListingIdOrderByCreatedAtDesc(ListingType listingType, Long listingId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    List<Promotion> findByListingTypeAndListingIdAndStatusIn(ListingType listingType, Long listingId,
                                                             List<PromotionStatus> statuses);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    List<Promotion> findByListingTypeAndListingIdAndStatus(ListingType listingType, Long listingId,
                                                           PromotionStatus status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    List<Promotion> findByStatusAndStartDateIsNull(PromotionStatus status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    List<Promotion> findByStatusAndStartDateLessThanEqual(PromotionStatus status, LocalDateTime now);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    List<Promotion> findByStatusAndEndDateLessThanEqual(PromotionStatus status, LocalDateTime now);

    @Query("SELECT p FROM Promotion p WHERE " +
            "(:status IS NULL OR p.status = :status) " +
            "AND (:listingType IS NULL OR p.listingType = :listingType)")
    Page<Promotion> adminSearch(@Param("status") PromotionStatus status,
                                @Param("listingType") ListingType listingType,
                                Pageable pageable);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Promotion p WHERE p.id = :id")
    Optional<Promotion> findByIdForUpdate(@Param("id") Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Promotion p WHERE p.ownerId = :ownerId AND p.status IN :statuses ORDER BY p.id")
    List<Promotion> findByOwnerIdAndStatusInForUpdate(@Param("ownerId") Long ownerId,
                                                      @Param("statuses") List<PromotionStatus> statuses);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    List<Promotion> findByPredecessorPromotionIdAndStatusIn(Long predecessorPromotionId,
                                                            List<PromotionStatus> statuses);

    boolean existsByPredecessorPromotionIdAndStatusIn(Long predecessorPromotionId,
                                                      List<PromotionStatus> statuses);
}
