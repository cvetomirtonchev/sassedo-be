package server.sassedo.engagement.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import server.sassedo.promotion.common.ListingType;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Populates favorite/view counters on listing response DTOs. Uses batch queries so browse and
 * "My Favorites" lists avoid N+1 lookups.
 */
@Component
@RequiredArgsConstructor
public class EngagementEnricher {

    private final FavoriteService favoriteService;
    private final ListingViewService listingViewService;

    /** Convenience for a single response (e.g. a detail page). */
    public void enrich(ListingType listingType, EngagementAware response, Long userId, boolean includeViewCount) {
        if (response == null) {
            return;
        }
        enrich(listingType, List.of(response), userId, includeViewCount);
    }

    /**
     * Sets {@code favoriteCount} and {@code favoritedByMe} on every response, plus {@code viewCount}
     * when {@code includeViewCount} is true (detail views only, to avoid a per-row count on browse).
     */
    public void enrich(ListingType listingType, List<? extends EngagementAware> responses, Long userId,
                       boolean includeViewCount) {
        if (responses == null || responses.isEmpty()) {
            return;
        }
        List<Long> ids = responses.stream()
                .map(EngagementAware::getId)
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toList());

        Map<Long, Long> favoriteCounts = favoriteService.counts(listingType, ids);
        Set<Long> favoritedByMe = favoriteService.favoritedIds(userId, listingType, ids);
        Map<Long, Long> viewCounts = includeViewCount ? listingViewService.counts(listingType, ids) : Map.of();

        for (EngagementAware response : responses) {
            Long id = response.getId();
            response.setFavoriteCount(favoriteCounts.getOrDefault(id, 0L));
            response.setFavoritedByMe(favoritedByMe.contains(id));
            if (includeViewCount) {
                response.setViewCount(viewCounts.getOrDefault(id, 0L));
            }
        }
    }
}
