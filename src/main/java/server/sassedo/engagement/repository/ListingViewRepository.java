package server.sassedo.engagement.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import server.sassedo.engagement.data.dto.ListingView;
import server.sassedo.promotion.common.ListingType;

@Repository
public interface ListingViewRepository extends JpaRepository<ListingView, Long> {

    boolean existsByListingTypeAndListingIdAndViewerKey(ListingType listingType, Long listingId, String viewerKey);

    long countByListingTypeAndListingId(ListingType listingType, Long listingId);
}
