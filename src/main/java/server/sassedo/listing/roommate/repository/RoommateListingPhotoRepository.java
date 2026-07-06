package server.sassedo.listing.roommate.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import server.sassedo.listing.roommate.data.dto.RoommateListingPhoto;

@Repository
public interface RoommateListingPhotoRepository extends JpaRepository<RoommateListingPhoto, Long> {

    RoommateListingPhoto findFirstByListingIdAndMainTrue(Long listingId);

    RoommateListingPhoto findFirstByListingIdOrderByIdAsc(Long listingId);
}
