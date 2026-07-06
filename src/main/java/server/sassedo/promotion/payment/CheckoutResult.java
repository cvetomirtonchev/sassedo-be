package server.sassedo.promotion.payment;

import server.sassedo.promotion.common.PaymentStatus;

/**
 * Provider-agnostic result of starting a checkout. In the MVP mock provider the payment is
 * already {@code COMPLETED}; under Stripe it would be {@code PENDING} with a real
 * {@code checkoutUrl} and only confirmed later via webhook.
 */
public record CheckoutResult(
        String providerRef,
        String checkoutUrl,
        PaymentStatus status,
        String rawPayload
) {
}
