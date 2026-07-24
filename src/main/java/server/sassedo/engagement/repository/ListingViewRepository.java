package server.sassedo.engagement.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import server.sassedo.engagement.data.dto.ListingView;
import server.sassedo.promotion.common.ListingType;

import java.util.Collection;
import java.util.List;

@Repository
public interface ListingViewRepository extends JpaRepository<ListingView, Long> {

    boolean existsByListingTypeAndListingIdAndViewerKey(ListingType listingType, Long listingId, String viewerKey);

    long countByListingTypeAndListingId(ListingType listingType, Long listingId);

    @Query("SELECT v.listingId, COUNT(v) FROM ListingView v " +
            "WHERE v.listingType = :listingType AND v.listingId IN :listingIds " +
            "GROUP BY v.listingId")
    List<Object[]> countByListingIds(@Param("listingType") ListingType listingType,
                                     @Param("listingIds") Collection<Long> listingIds);
}
