package server.sassedo.listing.roommate.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import server.sassedo.listing.roommate.data.dto.RoommateListingPhoto;
import server.sassedo.listing.roommate.data.projection.RoommatePhotoMeta;

import java.util.List;

@Repository
public interface RoommateListingPhotoRepository extends JpaRepository<RoommateListingPhoto, Long> {

    RoommateListingPhoto findFirstByListingIdAndMainTrue(Long listingId);

    RoommateListingPhoto findFirstByListingIdOrderByIdAsc(Long listingId);

    /**
     * Photo id + main flag only (never the MEDIUMBLOB data), for building list responses cheaply.
     */
    @Query("select p.id as id, p.main as main from RoommateListingPhoto p where p.listing.id = :listingId order by p.id")
    List<RoommatePhotoMeta> findMetaByListingId(@Param("listingId") Long listingId);
}
