package server.sassedo.listing.rental.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import server.sassedo.listing.rental.data.dto.RentalListingPhoto;
import server.sassedo.listing.rental.data.projection.RentalPhotoMeta;

import java.util.List;

@Repository
public interface RentalListingPhotoRepository extends JpaRepository<RentalListingPhoto, Long> {

    RentalListingPhoto findFirstByListingIdAndMainTrue(Long listingId);

    RentalListingPhoto findFirstByListingIdOrderByIdAsc(Long listingId);

    /**
     * Photo id + main flag only (never the MEDIUMBLOB data), for building list responses cheaply.
     */
    @Query("select p.id as id, p.main as main from RentalListingPhoto p where p.listing.id = :listingId order by p.id")
    List<RentalPhotoMeta> findMetaByListingId(@Param("listingId") Long listingId);
}
