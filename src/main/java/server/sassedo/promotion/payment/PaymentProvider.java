package server.sassedo.promotion.payment;

import server.sassedo.promotion.common.PaymentProviderType;
import server.sassedo.promotion.data.dto.Payment;
import server.sassedo.promotion.data.dto.Purchase;

/**
 * The single seam between the promotion system and any payment gateway. Everything above
 * this interface (PurchaseService, PromotionService, the scheduler) is provider-agnostic
 * and never references gateway-specific types. Swapping to Stripe is implementing this
 * interface + flipping {@code sassedo.payments.provider=stripe}; no other code changes.
 */
public interface PaymentProvider {

    PaymentProviderType type();

    /** Start a checkout for a purchase (Stripe Checkout Session / Payment Intent). */
    CheckoutResult createCheckout(Purchase purchase);

    /** Verify and parse an inbound webhook into a provider-agnostic result. */
    WebhookResult handleWebhook(String rawBody, String signature);

    /** Refund a completed payment. */
    RefundResult refund(Payment payment);
}
