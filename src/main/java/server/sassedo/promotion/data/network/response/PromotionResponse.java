package server.sassedo.promotion.data.network.response;

import lombok.Getter;
import lombok.Setter;
import server.sassedo.promotion.common.ListingType;
import server.sassedo.promotion.common.PromotionSource;
import server.sassedo.promotion.common.PromotionStatus;
import server.sassedo.promotion.common.PromotionType;

import java.time.LocalDateTime;

@Getter
@Setter
public class PromotionResponse {

    private Long id;
    private ListingType listingType;
    private Long listingId;
    private Long ownerId;
    private Long packageId;
    private PromotionType type;
    private PromotionStatus status;
    private PromotionSource source;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private Long purchaseId;
    private LocalDateTime createdAt;

    /** Whole days remaining until expiration, floored at 0. Null when not active. */
    private Long remainingDays;
}
