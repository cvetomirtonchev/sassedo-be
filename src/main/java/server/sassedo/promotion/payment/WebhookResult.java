package server.sassedo.promotion.payment;

import server.sassedo.promotion.common.PaymentStatus;

/**
 * Provider-agnostic outcome of a verified webhook. {@code providerRef} identifies the
 * payment (checkout session / payment intent) so it can be matched to a stored Payment.
 */
public record WebhookResult(
        String providerRef,
        PaymentStatus status,
        String rawPayload
) {
}
