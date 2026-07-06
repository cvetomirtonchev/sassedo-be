package server.sassedo.listing.common;

import org.springframework.data.domain.Sort;

/**
 * Client-selectable ordering for public listing browse. Promotion tier always takes precedence
 * (featured/promoted first), then the user-selected key is applied as a tie-breaker so paid
 * placement is preserved regardless of the chosen sort.
 */
public enum ListingSort {
    NEWEST,
    OLDEST,
    PRICE_ASC,
    PRICE_DESC,
    RECENTLY_UPDATED;

    /**
     * Builds the full sort: promotion tier first, then the user-selected key.
     *
     * @param priceField entity price property to sort on ("rent" for rental/roommate,
     *                   "budgetMax" for apartment searches)
     */
    public Sort toSort(String priceField) {
        Sort base = Sort.by(
                Sort.Order.desc("promotionState.promotionPriority"),
                Sort.Order.desc("promotionState.promotionActivatedAt"));
        Sort user = switch (this) {
            case OLDEST -> Sort.by(Sort.Order.asc("createdAt"));
            case PRICE_ASC -> Sort.by(Sort.Order.asc(priceField).nullsLast());
            case PRICE_DESC -> Sort.by(Sort.Order.desc(priceField).nullsLast());
            case RECENTLY_UPDATED -> Sort.by(Sort.Order.desc("updatedAt"));
            case NEWEST -> Sort.by(Sort.Order.desc("createdAt"));
        };
        return base.and(user);
    }
}
