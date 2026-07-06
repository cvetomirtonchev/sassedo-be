package server.sassedo.listing.rental.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import server.sassedo.listing.rental.data.dto.RentalListingPhoto;

@Repository
public interface RentalListingPhotoRepository extends JpaRepository<RentalListingPhoto, Long> {

    RentalListingPhoto findFirstByListingIdAndMainTrue(Long listingId);

    RentalListingPhoto findFirstByListingIdOrderByIdAsc(Long listingId);
}
