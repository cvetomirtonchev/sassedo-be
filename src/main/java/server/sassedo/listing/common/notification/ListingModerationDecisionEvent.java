package server.sassedo.listing.common.notification;

import server.sassedo.listing.common.ListingStatus;
import server.sassedo.promotion.common.ListingType;

/**
 * Published while an admin moderation transaction is active and delivered only after commit.
 */
public record ListingModerationDecisionEvent(
        ListingType listingType,
        Long listingId,
        Long ownerId,
        String listingTitle,
        ListingStatus decision,
        String rejectionReason
) {
}
