package server.sassedo.listing.roommate.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import server.sassedo.listing.common.ListingStatus;
import server.sassedo.listing.common.PropertyType;
import server.sassedo.listing.roommate.data.dto.RoommateListing;

import jakarta.persistence.LockModeType;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface RoommateListingRepository extends JpaRepository<RoommateListing, Long>,
        JpaSpecificationExecutor<RoommateListing> {

    List<RoommateListing> findByOwnerIdOrderByCreatedAtDesc(Long ownerId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT l FROM RoommateListing l WHERE l.id = :id")
    Optional<RoommateListing> findByIdForUpdate(@Param("id") Long id);

    @Query(value = "SELECT * FROM roommate_listings WHERE status = 'ACTIVE' ORDER BY RAND() LIMIT :limit",
            nativeQuery = true)
    List<RoommateListing> findRandomActive(@Param("limit") int limit);

    @Query("SELECT l FROM RoommateListing l LEFT JOIN l.city c WHERE " +
            "(:status IS NULL OR l.status = :status) " +
            "AND (:cityId IS NULL OR c.id = :cityId) " +
            "AND (:propertyType IS NULL OR l.propertyType = :propertyType)")
    Page<RoommateListing> browse(@Param("status") ListingStatus status,
                                 @Param("cityId") Long cityId,
                                 @Param("propertyType") PropertyType propertyType,
                                 Pageable pageable);

    @Modifying
    @Query("UPDATE RoommateListing l SET l.status = server.sassedo.listing.common.ListingStatus.EXPIRED, l.updatedAt = :now " +
            "WHERE l.status = server.sassedo.listing.common.ListingStatus.ACTIVE " +
            "AND l.expiresAt IS NOT NULL AND l.expiresAt <= :now " +
            "AND (l.promotionState.promotedUntil IS NULL OR l.promotionState.promotedUntil <= :now)")
    int expireOverdue(@Param("now") LocalDateTime now);

    @Modifying
    @Query("UPDATE RoommateListing l SET l.expiresAt = :expiresAt " +
            "WHERE l.status = server.sassedo.listing.common.ListingStatus.ACTIVE AND l.expiresAt IS NULL")
    int backfillMissingExpiry(@Param("expiresAt") LocalDateTime expiresAt);

    @Modifying
    @Query("UPDATE RoommateListing l SET l.status = server.sassedo.listing.common.ListingStatus.EXPIRED, " +
            "l.expiresAt = :now, l.updatedAt = :now " +
            "WHERE l.ownerId = :ownerId AND l.status IN :statuses")
    int expireByOwnerId(@Param("ownerId") Long ownerId,
                        @Param("statuses") List<ListingStatus> statuses,
                        @Param("now") LocalDateTime now);

    @Query("SELECT l FROM RoommateListing l LEFT JOIN l.city c WHERE " +
            "(:status IS NULL OR l.status = :status) " +
            "AND (:cityId IS NULL OR c.id = :cityId) " +
            "AND (:search IS NULL OR :search = '' " +
            "OR (:listingId IS NOT NULL AND l.id = :listingId) " +
            "OR LOWER(l.title) LIKE LOWER(CONCAT('%', :search, '%')) " +
            "OR LOWER(l.neighborhood) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<RoommateListing> adminSearch(@Param("status") ListingStatus status,
                                      @Param("search") String search,
                                      @Param("listingId") Long listingId,
                                      @Param("cityId") Long cityId,
                                      Pageable pageable);

    @Query("SELECT l.city.id, COUNT(l) FROM RoommateListing l WHERE " +
            "l.status = server.sassedo.listing.common.ListingStatus.ACTIVE AND l.city IS NOT NULL " +
            "GROUP BY l.city.id")
    List<Object[]> countActiveByCity();
}
