package server.sassedo.listing.roommate.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import server.sassedo.listing.common.ListingStatus;
import server.sassedo.listing.common.PropertyType;
import server.sassedo.listing.roommate.data.dto.RoommateListing;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface RoommateListingRepository extends JpaRepository<RoommateListing, Long>,
        JpaSpecificationExecutor<RoommateListing> {

    List<RoommateListing> findByOwnerIdOrderByCreatedAtDesc(Long ownerId);

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

    @Query("SELECT l FROM RoommateListing l WHERE " +
            "(:status IS NULL OR l.status = :status) " +
            "AND (:search IS NULL OR :search = '' " +
            "OR LOWER(l.title) LIKE LOWER(CONCAT('%', :search, '%')) " +
            "OR LOWER(l.neighborhood) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<RoommateListing> adminSearch(@Param("status") ListingStatus status,
                                      @Param("search") String search,
                                      Pageable pageable);

    @Query("SELECT l.city.id, COUNT(l) FROM RoommateListing l WHERE " +
            "l.status = server.sassedo.listing.common.ListingStatus.ACTIVE AND l.city IS NOT NULL " +
            "GROUP BY l.city.id")
    List<Object[]> countActiveByCity();
}
