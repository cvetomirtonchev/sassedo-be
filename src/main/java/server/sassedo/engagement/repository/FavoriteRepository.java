package server.sassedo.engagement.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import server.sassedo.engagement.data.dto.Favorite;
import server.sassedo.promotion.common.ListingType;

import java.util.Collection;
import java.util.List;

@Repository
public interface FavoriteRepository extends JpaRepository<Favorite, Long> {

    boolean existsByUserIdAndListingTypeAndListingId(Long userId, ListingType listingType, Long listingId);

    long deleteByUserIdAndListingTypeAndListingId(Long userId, ListingType listingType, Long listingId);

    List<Favorite> findByUserIdAndListingTypeOrderByCreatedAtDesc(Long userId, ListingType listingType);

    long countByListingTypeAndListingId(ListingType listingType, Long listingId);

    @Query("SELECT f.listingId, COUNT(f) FROM Favorite f " +
            "WHERE f.listingType = :listingType AND f.listingId IN :listingIds " +
            "GROUP BY f.listingId")
    List<Object[]> countByListingIds(@Param("listingType") ListingType listingType,
                                     @Param("listingIds") Collection<Long> listingIds);

    @Query("SELECT f.listingId FROM Favorite f " +
            "WHERE f.userId = :userId AND f.listingType = :listingType AND f.listingId IN :listingIds")
    List<Long> findFavoritedListingIds(@Param("userId") Long userId,
                                       @Param("listingType") ListingType listingType,
                                       @Param("listingIds") Collection<Long> listingIds);
}
