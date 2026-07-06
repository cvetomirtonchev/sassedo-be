package server.sassedo.promotion.service;

import server.sassedo.promotion.common.PaymentStatus;
import server.sassedo.promotion.data.dto.Payment;
import server.sassedo.promotion.data.dto.Promotion;

/** Result of starting a checkout: the payment attempt, the checkout URL, and the
 * (possibly already-activated) promotion. */
public record CheckoutOutcome(
        Payment payment,
        String checkoutUrl,
        PaymentStatus status,
        Promotion promotion
) {
}
