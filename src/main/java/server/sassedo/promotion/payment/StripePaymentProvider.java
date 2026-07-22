package server.sassedo.promotion.payment;

import com.stripe.StripeClient;
import com.stripe.exception.EventDataObjectDeserializationException;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.exception.StripeException;
import com.stripe.model.Event;
import com.stripe.model.StripeObject;
import com.stripe.model.checkout.Session;
import com.stripe.net.RequestOptions;
import com.stripe.param.checkout.SessionCreateParams;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;
import server.sassedo.promotion.common.PaymentProviderType;
import server.sassedo.promotion.common.PaymentStatus;
import server.sassedo.promotion.data.dto.Payment;
import server.sassedo.promotion.data.dto.Purchase;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Locale;
import java.util.Map;

/**
 * Stripe-hosted Checkout provider for one-time promotion purchases.
 */
@Component
@ConditionalOnProperty(name = "sassedo.payments.provider", havingValue = "stripe")
@EnableConfigurationProperties(StripePaymentProperties.class)
public class StripePaymentProvider implements PaymentProvider {

    static final String METADATA_SOURCE = "sassedo-listing-promotion";
    // Stripe requires expires_at to be at least 30 minutes in the future. The extra
    // minute prevents network and clock delay from putting the request below that limit.
    private static final long CHECKOUT_EXPIRY_MINUTES = 31;

    private final StripePaymentProperties properties;
    private final StripeClient stripeClient;

    @Autowired
    public StripePaymentProvider(StripePaymentProperties properties) {
        this(properties, StripeClient.builder()
                .setApiKey(properties.secretKey())
                .setMaxNetworkRetries(2)
                .build());
    }

    StripePaymentProvider(StripePaymentProperties properties, StripeClient stripeClient) {
        this.properties = properties;
        this.stripeClient = stripeClient;
    }

    @Override
    public PaymentProviderType type() {
        return PaymentProviderType.STRIPE;
    }

    @Override
    public CheckoutResult createCheckout(Purchase purchase) {
        String purchaseId = purchase.getId().toString();
        String clientUrl = normalizedClientUrl();
        SessionCreateParams params = SessionCreateParams.builder()
                .setMode(SessionCreateParams.Mode.PAYMENT)
                .addPaymentMethodType(SessionCreateParams.PaymentMethodType.CARD)
                .setClientReferenceId(purchaseId)
                .setSuccessUrl(clientUrl + "/dashboard/promotions?checkout=success&purchaseId="
                        + purchaseId + "&session_id={CHECKOUT_SESSION_ID}")
                .setCancelUrl(clientUrl + "/dashboard/promotions?checkout=cancelled&purchaseId="
                        + purchaseId)
                .setExpiresAt(Instant.now().plus(CHECKOUT_EXPIRY_MINUTES, ChronoUnit.MINUTES)
                        .getEpochSecond())
                .putMetadata("source", METADATA_SOURCE)
                .putMetadata("purchaseId", purchaseId)
                .putMetadata("promotionId", String.valueOf(purchase.getPromotionId()))
                .putMetadata("listingType", purchase.getListingType().name())
                .putMetadata("listingId", purchase.getListingId().toString())
                .addLineItem(SessionCreateParams.LineItem.builder()
                        .setQuantity(1L)
                        .setPriceData(SessionCreateParams.LineItem.PriceData.builder()
                                .setCurrency(purchase.getCurrency().toLowerCase(Locale.ROOT))
                                .setUnitAmount((long) purchase.getAmountCents())
                                .setProductData(SessionCreateParams.LineItem.PriceData.ProductData.builder()
                                        .setName("Sassedo listing promotion")
                                        .setDescription("Promotion package #" + purchase.getPackageId()
                                                + " for " + purchase.getListingType().name().toLowerCase(Locale.ROOT)
                                                + " listing #" + purchase.getListingId())
                                        .build())
                                .build())
                        .build())
                .build();
        RequestOptions requestOptions = RequestOptions.builder()
                .setIdempotencyKey("sassedo-promotion-purchase-" + purchaseId)
                .build();

        try {
            Session session = stripeClient.checkout().sessions().create(params, requestOptions);
            if (session.getId() == null || session.getUrl() == null) {
                throw new IllegalStateException("Stripe created a Checkout Session without an id or URL");
            }
            return new CheckoutResult(
                    session.getId(),
                    session.getUrl(),
                    PaymentStatus.PENDING,
                    session.toJson()
            );
        } catch (StripeException e) {
            throw new IllegalStateException("Could not create Stripe Checkout Session", e);
        }
    }

    @Override
    public WebhookResult handleWebhook(String rawBody, String signature) {
        if (rawBody == null || rawBody.isBlank() || signature == null || signature.isBlank()) {
            throw new InvalidPaymentWebhookException("Missing Stripe webhook body or signature");
        }

        final Event event;
        try {
            event = stripeClient.constructEvent(rawBody, signature, properties.webhookSecret());
        } catch (SignatureVerificationException e) {
            throw new InvalidPaymentWebhookException("Invalid Stripe webhook signature", e);
        } catch (RuntimeException e) {
            throw new InvalidPaymentWebhookException("Invalid Stripe webhook payload", e);
        }

        if (event.getType() == null) {
            throw new InvalidPaymentWebhookException("Stripe webhook is missing an event type");
        }
        PaymentStatus status = switch (event.getType()) {
            case "checkout.session.completed", "checkout.session.async_payment_succeeded" ->
                    PaymentStatus.COMPLETED;
            case "checkout.session.expired" -> PaymentStatus.CANCELLED;
            case "checkout.session.async_payment_failed" -> PaymentStatus.FAILED;
            default -> null;
        };
        if (status == null) {
            return new WebhookResult(null, PaymentStatus.PENDING, rawBody);
        }

        Session session = deserializeSession(event);
        Map<String, String> metadata = session.getMetadata();
        if (metadata == null || !METADATA_SOURCE.equals(metadata.get("source"))) {
            return new WebhookResult(null, PaymentStatus.PENDING, rawBody);
        }
        if (status == PaymentStatus.COMPLETED && !"paid".equals(session.getPaymentStatus())) {
            return new WebhookResult(null, PaymentStatus.PENDING, rawBody);
        }
        return new WebhookResult(session.getId(), status, rawBody);
    }

    @Override
    public RefundResult refund(Payment payment) {
        throw new UnsupportedOperationException("Stripe refunds are not supported yet");
    }

    private Session deserializeSession(Event event) {
        StripeObject object = event.getDataObjectDeserializer().getObject().orElseGet(() -> {
            try {
                return event.getDataObjectDeserializer().deserializeUnsafe();
            } catch (EventDataObjectDeserializationException e) {
                throw new InvalidPaymentWebhookException("Invalid Stripe Checkout Session payload", e);
            }
        });
        if (!(object instanceof Session session) || session.getId() == null) {
            throw new InvalidPaymentWebhookException("Stripe event does not contain a Checkout Session");
        }
        return session;
    }

    private String normalizedClientUrl() {
        return properties.clientUrl().replaceFirst("/+$", "");
    }
}
