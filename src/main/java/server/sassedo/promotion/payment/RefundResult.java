package server.sassedo.promotion.payment;

import server.sassedo.promotion.common.PaymentStatus;

public record RefundResult(
        boolean success,
        PaymentStatus status,
        String rawPayload
) {
}
