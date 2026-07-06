package server.sassedo.promotion.payment;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import server.sassedo.promotion.common.PaymentProviderType;
import server.sassedo.promotion.data.dto.Payment;
import server.sassedo.promotion.data.dto.Purchase;

/**
 * Stripe provider stub. Intentionally NOT implemented in the MVP and NOT wired unless
 * {@code sassedo.payments.provider=stripe}. It documents exactly where Stripe logic plugs
 * in so the integration is additive.
 *
 * To integrate Stripe later:
 *   1. Add the {@code com.stripe:stripe-java} dependency.
 *   2. createCheckout  -> create a Stripe Checkout Session (or PaymentIntent); return its
 *      id as providerRef, its url as checkoutUrl, status PENDING.
 *   3. handleWebhook   -> verify the signature with the webhook secret, parse
 *      {@code checkout.session.completed} / {@code charge.refunded} events, map to
 *      PaymentStatus. Return the session/intent id as providerRef.
 *   4. refund          -> call Stripe Refunds API.
 *   5. Set sassedo.payments.provider=stripe. No changes to the promotion system.
 */
@Component
@ConditionalOnProperty(name = "sassedo.payments.provider", havingValue = "stripe")
public class StripePaymentProvider implements PaymentProvider {

    @Override
    public PaymentProviderType type() {
        return PaymentProviderType.STRIPE;
    }

    @Override
    public CheckoutResult createCheckout(Purchase purchase) {
        // TODO(stripe): create a Checkout Session / PaymentIntent and return PENDING.
        throw new UnsupportedOperationException("Stripe integration is not implemented yet");
    }

    @Override
    public WebhookResult handleWebhook(String rawBody, String signature) {
        // TODO(stripe): verify signature, parse event, map to PaymentStatus.
        throw new UnsupportedOperationException("Stripe integration is not implemented yet");
    }

    @Override
    public RefundResult refund(Payment payment) {
        // TODO(stripe): call the Stripe Refunds API.
        throw new UnsupportedOperationException("Stripe integration is not implemented yet");
    }
}
