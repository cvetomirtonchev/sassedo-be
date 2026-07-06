package server.sassedo.promotion.payment;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import server.sassedo.promotion.common.PaymentProviderType;
import server.sassedo.promotion.common.PaymentStatus;
import server.sassedo.promotion.data.dto.Payment;
import server.sassedo.promotion.data.dto.Purchase;

/**
 * MVP payment provider. Simulates an instantly-successful checkout so promotions activate
 * end-to-end without a real gateway. Active by default (and whenever
 * {@code sassedo.payments.provider=mock}).
 */
@Component
@ConditionalOnProperty(name = "sassedo.payments.provider", havingValue = "mock", matchIfMissing = true)
public class MockPaymentProvider implements PaymentProvider {

    @Override
    public PaymentProviderType type() {
        return PaymentProviderType.MOCK;
    }

    @Override
    public CheckoutResult createCheckout(Purchase purchase) {
        String ref = "mock_" + purchase.getId();
        return new CheckoutResult(
                ref,
                "mock://checkout/" + purchase.getId(),
                PaymentStatus.COMPLETED,
                "{\"provider\":\"mock\",\"simulated\":true,\"purchaseId\":" + purchase.getId() + "}"
        );
    }

    @Override
    public WebhookResult handleWebhook(String rawBody, String signature) {
        return new WebhookResult(null, PaymentStatus.COMPLETED, rawBody);
    }

    @Override
    public RefundResult refund(Payment payment) {
        return new RefundResult(true, PaymentStatus.REFUNDED,
                "{\"provider\":\"mock\",\"refunded\":true,\"paymentId\":" + payment.getId() + "}");
    }
}
