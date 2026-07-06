package server.sassedo.promotion.data.network.response;

import lombok.Getter;
import lombok.Setter;
import server.sassedo.promotion.common.PaymentStatus;

/**
 * Returned from POST /api/purchases. In the MVP the mock provider auto-completes, so
 * {@code status} is COMPLETED and {@code promotion} is already ACTIVE. Under Stripe this
 * would return PENDING with a real {@code checkoutUrl}, and the promotion activates later
 * via webhook.
 */
@Getter
@Setter
public class CheckoutResponse {

    private Long purchaseId;
    private PaymentStatus status;
    private String checkoutUrl;
    private PaymentResponse payment;
    private PromotionResponse promotion;
}
