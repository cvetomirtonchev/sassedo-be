package server.sassedo.promotion.service;

import server.sassedo.listing.common.ListingStatus;
import server.sassedo.promotion.common.ListingType;
import server.sassedo.promotion.common.PromotionType;
import server.sassedo.model.GenericException;

import java.time.LocalDateTime;

/**
 * Abstracts the three concrete listing tables (rental / roommate / search) behind a single
 * polymorphic API so the promotion system does not need to know about each market. Writes
 * the denormalized {@code PromotionState} columns used by the browse ordering.
 */
public interface PromotableListingService {

    Long getOwnerId(ListingType type, Long listingId) throws GenericException;

    ListingStatus getListingStatus(ListingType type, Long listingId) throws GenericException;

    void applyPromotion(ListingType type, Long listingId, PromotionType promotionType, Long promotionId,
                        LocalDateTime activatedAt, LocalDateTime until) throws GenericException;

    void clearPromotion(ListingType type, Long listingId);

    Long getActivePromotionId(ListingType type, Long listingId) throws GenericException;

    /** Downgrade listing tier only when {@code promotionId} is the current active promotion. */
    void clearPromotionIfActive(ListingType type, Long listingId, Long promotionId);
}
