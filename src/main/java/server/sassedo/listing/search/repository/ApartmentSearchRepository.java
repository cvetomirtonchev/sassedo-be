package server.sassedo.listing.search.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import server.sassedo.listing.common.ListingStatus;
import server.sassedo.listing.common.PropertyType;
import server.sassedo.listing.search.data.dto.ApartmentSearch;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ApartmentSearchRepository extends JpaRepository<ApartmentSearch, Long> {

    List<ApartmentSearch> findByOwnerIdOrderByCreatedAtDesc(Long ownerId);

    @Query("SELECT s FROM ApartmentSearch s LEFT JOIN s.city c WHERE " +
            "(:status IS NULL OR s.status = :status) " +
            "AND (:cityId IS NULL OR c.id = :cityId) " +
            "AND (:propertyType IS NULL OR s.propertyType = :propertyType)")
    Page<ApartmentSearch> browse(@Param("status") ListingStatus status,
                                 @Param("cityId") Long cityId,
                                 @Param("propertyType") PropertyType propertyType,
                                 Pageable pageable);

    @Modifying
    @Query("UPDATE ApartmentSearch s SET s.status = server.sassedo.listing.common.ListingStatus.EXPIRED, s.updatedAt = :now " +
            "WHERE s.status = server.sassedo.listing.common.ListingStatus.ACTIVE " +
            "AND s.expiresAt IS NOT NULL AND s.expiresAt <= :now " +
            "AND (s.promotionState.promotedUntil IS NULL OR s.promotionState.promotedUntil <= :now)")
    int expireOverdue(@Param("now") LocalDateTime now);

    @Modifying
    @Query("UPDATE ApartmentSearch s SET s.expiresAt = :expiresAt " +
            "WHERE s.status = server.sassedo.listing.common.ListingStatus.ACTIVE AND s.expiresAt IS NULL")
    int backfillMissingExpiry(@Param("expiresAt") LocalDateTime expiresAt);

    @Query("SELECT s FROM ApartmentSearch s WHERE " +
            "(:status IS NULL OR s.status = :status) " +
            "AND (:search IS NULL OR :search = '' " +
            "OR LOWER(s.title) LIKE LOWER(CONCAT('%', :search, '%')) " +
            "OR LOWER(s.neighborhood) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<ApartmentSearch> adminSearch(@Param("status") ListingStatus status,
                                      @Param("search") String search,
                                      Pageable pageable);

    @Query("SELECT s.city.id, COUNT(s) FROM ApartmentSearch s WHERE " +
            "s.status = server.sassedo.listing.common.ListingStatus.ACTIVE AND s.city IS NOT NULL " +
            "GROUP BY s.city.id")
    List<Object[]> countActiveByCity();
}
