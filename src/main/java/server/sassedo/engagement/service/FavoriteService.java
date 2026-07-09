package server.sassedo.engagement.service;

import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import server.sassedo.engagement.data.dto.Favorite;
import server.sassedo.engagement.repository.FavoriteRepository;
import server.sassedo.promotion.common.ListingType;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class FavoriteService {

    private final FavoriteRepository favoriteRepository;

    /**
     * Adds a favorite for the given user. Idempotent: if it already exists (including on a
     * concurrent insert) the existing row is kept. Returns the resulting favorite count.
     * <p>
     * Not wrapped in a single transaction on purpose: {@code saveAndFlush} runs in its own
     * transaction, so a losing race on the unique constraint rolls back only that insert without
     * poisoning the follow-up count query.
     */
    public long add(Long userId, ListingType listingType, Long listingId) {
        if (!favoriteRepository.existsByUserIdAndListingTypeAndListingId(userId, listingType, listingId)) {
            Favorite favorite = new Favorite();
            favorite.setUserId(userId);
            favorite.setListingType(listingType);
            favorite.setListingId(listingId);
            try {
                favoriteRepository.saveAndFlush(favorite);
            } catch (DataIntegrityViolationException ignored) {
                // Concurrent insert raced us to the unique constraint; the favorite already exists.
            }
        }
        return count(listingType, listingId);
    }

    /** Removes the user's favorite if present. Returns the resulting favorite count. */
    @Transactional
    public long remove(Long userId, ListingType listingType, Long listingId) {
        favoriteRepository.deleteByUserIdAndListingTypeAndListingId(userId, listingType, listingId);
        return count(listingType, listingId);
    }

    @Transactional(readOnly = true)
    public List<Favorite> listMine(Long userId, ListingType listingType) {
        return favoriteRepository.findByUserIdAndListingTypeOrderByCreatedAtDesc(userId, listingType);
    }

    @Transactional(readOnly = true)
    public long count(ListingType listingType, Long listingId) {
        return favoriteRepository.countByListingTypeAndListingId(listingType, listingId);
    }

    /** Batch favorite counts keyed by listing id (missing ids imply zero). */
    @Transactional(readOnly = true)
    public Map<Long, Long> counts(ListingType listingType, Collection<Long> listingIds) {
        Map<Long, Long> result = new HashMap<>();
        if (listingIds == null || listingIds.isEmpty()) {
            return result;
        }
        for (Object[] row : favoriteRepository.countByListingIds(listingType, listingIds)) {
            result.put((Long) row[0], (Long) row[1]);
        }
        return result;
    }

    /** The subset of the given listing ids that the user has favorited. */
    @Transactional(readOnly = true)
    public Set<Long> favoritedIds(Long userId, ListingType listingType, Collection<Long> listingIds) {
        if (userId == null || listingIds == null || listingIds.isEmpty()) {
            return Set.of();
        }
        return new HashSet<>(favoriteRepository.findFavoritedListingIds(userId, listingType, listingIds));
    }
}
