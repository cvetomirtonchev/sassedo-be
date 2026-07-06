package server.sassedo.promotion.data.network.response;

import lombok.Getter;
import lombok.Setter;
import server.sassedo.promotion.common.ListingType;
import server.sassedo.promotion.common.PaymentStatus;

import java.time.LocalDateTime;

@Getter
@Setter
public class PurchaseResponse {

    private Long id;
    private Long buyerId;
    private Long packageId;
    private ListingType listingType;
    private Long listingId;
    private int amountCents;
    private String currency;
    private PaymentStatus status;
    private Long promotionId;
    private LocalDateTime createdAt;
}
