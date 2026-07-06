package server.sassedo.listing.rental.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import server.sassedo.listing.common.ListingStatus;
import server.sassedo.listing.common.PropertyType;
import server.sassedo.listing.rental.data.dto.RentalListing;

import java.util.List;

@Repository
public interface RentalListingRepository extends JpaRepository<RentalListing, Long> {

    List<RentalListing> findByOwnerIdOrderByCreatedAtDesc(Long ownerId);

    @Query("SELECT l FROM RentalListing l LEFT JOIN l.city c WHERE " +
            "(:status IS NULL OR l.status = :status) " +
            "AND (:cityId IS NULL OR c.id = :cityId) " +
            "AND (:propertyType IS NULL OR l.propertyType = :propertyType)")
    Page<RentalListing> browse(@Param("status") ListingStatus status,
                               @Param("cityId") Long cityId,
                               @Param("propertyType") PropertyType propertyType,
                               Pageable pageable);

    @Query("SELECT l FROM RentalListing l WHERE " +
            "(:status IS NULL OR l.status = :status) " +
            "AND (:search IS NULL OR :search = '' " +
            "OR LOWER(l.title) LIKE LOWER(CONCAT('%', :search, '%')) " +
            "OR LOWER(l.neighborhood) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<RentalListing> adminSearch(@Param("status") ListingStatus status,
                                    @Param("search") String search,
                                    Pageable pageable);

    @Query("SELECT l.city.id, COUNT(l) FROM RentalListing l WHERE " +
            "l.status = server.sassedo.listing.common.ListingStatus.ACTIVE AND l.city IS NOT NULL " +
            "GROUP BY l.city.id")
    List<Object[]> countActiveByCity();
}
