package server.sassedo.promotion.payment;

import com.stripe.Stripe;
import com.stripe.StripeClient;
import com.stripe.model.checkout.Session;
import com.stripe.net.RequestOptions;
import com.stripe.net.Webhook;
import com.stripe.param.checkout.SessionCreateParams;
import com.stripe.service.CheckoutService;
import com.stripe.service.checkout.SessionService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import server.sassedo.promotion.common.ListingType;
import server.sassedo.promotion.common.PaymentStatus;
import server.sassedo.promotion.data.dto.Purchase;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class StripePaymentProviderTest {

    private static final String WEBHOOK_SECRET = "whsec_test";

    @Test
    void stripeEnabledContext_selectsProductionConstructor() {
        new ApplicationContextRunner()
                .withUserConfiguration(StripeProviderTestConfiguration.class)
                .withPropertyValues(
                        "sassedo.payments.provider=stripe",
                        "sassedo.payments.stripe.secret-key=not-a-real-stripe-key",
                        "sassedo.payments.stripe.webhook-secret=" + WEBHOOK_SECRET,
                        "sassedo.payments.stripe.client-url=https://sassedo-fe.vercel.app"
                )
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).hasSingleBean(StripePaymentProvider.class);
                });
    }

    @Test
    void createCheckout_usesTrustedPurchaseDataAndIdempotency() throws Exception {
        StripeClient stripeClient = mock(StripeClient.class);
        CheckoutService checkoutService = mock(CheckoutService.class);
        SessionService sessionService = mock(SessionService.class);
        when(stripeClient.checkout()).thenReturn(checkoutService);
        when(checkoutService.sessions()).thenReturn(sessionService);

        Session session = new Session();
        session.setId("cs_test_123");
        session.setUrl("https://checkout.stripe.com/c/pay/cs_test_123");
        when(sessionService.create(any(SessionCreateParams.class), any(RequestOptions.class)))
                .thenReturn(session);

        StripePaymentProvider provider = new StripePaymentProvider(properties(), stripeClient);
        CheckoutResult result = provider.createCheckout(purchase());

        assertThat(result.providerRef()).isEqualTo("cs_test_123");
        assertThat(result.checkoutUrl()).isEqualTo("https://checkout.stripe.com/c/pay/cs_test_123");
        assertThat(result.status()).isEqualTo(PaymentStatus.PENDING);

        ArgumentCaptor<SessionCreateParams> paramsCaptor =
                ArgumentCaptor.forClass(SessionCreateParams.class);
        ArgumentCaptor<RequestOptions> optionsCaptor = ArgumentCaptor.forClass(RequestOptions.class);
        verify(sessionService).create(paramsCaptor.capture(), optionsCaptor.capture());

        SessionCreateParams params = paramsCaptor.getValue();
        assertThat(params.getMode()).isEqualTo(SessionCreateParams.Mode.PAYMENT);
        assertThat(params.getPaymentMethodTypes())
                .containsExactly(SessionCreateParams.PaymentMethodType.CARD);
        assertThat(params.getClientReferenceId()).isEqualTo("42");
        assertThat(params.getSuccessUrl()).isEqualTo(
                "https://sassedo-fe.vercel.app/dashboard/promotions"
                        + "?checkout=success&purchaseId=42&session_id={CHECKOUT_SESSION_ID}");
        assertThat(params.getCancelUrl()).isEqualTo(
                "https://sassedo-fe.vercel.app/dashboard/promotions"
                        + "?checkout=cancelled&purchaseId=42");
        assertThat(params.getMetadata())
                .containsEntry("source", StripePaymentProvider.METADATA_SOURCE)
                .containsEntry("purchaseId", "42")
                .containsEntry("promotionId", "8")
                .containsEntry("listingType", "RENTAL")
                .containsEntry("listingId", "11");
        assertThat(params.getLineItems()).hasSize(1);
        SessionCreateParams.LineItem lineItem = params.getLineItems().get(0);
        assertThat(lineItem.getQuantity()).isEqualTo(1L);
        assertThat(lineItem.getPriceData().getCurrency()).isEqualTo("eur");
        assertThat(lineItem.getPriceData().getUnitAmount()).isEqualTo(1299L);
        assertThat(params.getExpiresAt())
                .isBetween(Instant.now().plusSeconds(30 * 60).getEpochSecond(),
                        Instant.now().plusSeconds(32 * 60).getEpochSecond());
        assertThat(optionsCaptor.getValue().getIdempotencyKey())
                .isEqualTo("sassedo-promotion-purchase-42");
    }

    @ParameterizedTest
    @CsvSource({
            "checkout.session.completed, paid, COMPLETED",
            "checkout.session.async_payment_succeeded, paid, COMPLETED",
            "checkout.session.expired, unpaid, CANCELLED",
            "checkout.session.async_payment_failed, unpaid, FAILED"
    })
    void handleWebhook_verifiesAndMapsCheckoutEvents(
            String eventType, String paymentStatus, PaymentStatus expected) throws Exception {
        String payload = eventPayload(eventType, paymentStatus, StripePaymentProvider.METADATA_SOURCE);
        StripePaymentProvider provider = realProvider();

        WebhookResult result = provider.handleWebhook(payload, signature(payload));

        assertThat(result.providerRef()).isEqualTo("cs_test_123");
        assertThat(result.status()).isEqualTo(expected);
        assertThat(result.rawPayload()).isEqualTo(payload);
    }

    @Test
    void handleWebhook_rejectsInvalidSignature() {
        String payload = eventPayload(
                "checkout.session.completed", "paid", StripePaymentProvider.METADATA_SOURCE);

        assertThatThrownBy(() -> realProvider().handleWebhook(payload, "t=1,v1=invalid"))
                .isInstanceOf(InvalidPaymentWebhookException.class)
                .hasMessageContaining("signature");
    }

    @Test
    void handleWebhook_rejectsMalformedPayload() {
        assertThatThrownBy(() -> realProvider().handleWebhook("not-json", "invalid"))
                .isInstanceOf(InvalidPaymentWebhookException.class)
                .hasMessageContaining("payload");
    }

    @Test
    void handleWebhook_ignoresCheckoutFromAnotherIntegration() throws Exception {
        String payload = eventPayload("checkout.session.completed", "paid", "another-app");

        WebhookResult result = realProvider().handleWebhook(payload, signature(payload));

        assertThat(result.providerRef()).isNull();
    }

    private StripePaymentProvider realProvider() {
        return new StripePaymentProvider(properties(), new StripeClient("not-a-real-stripe-key"));
    }

    private StripePaymentProperties properties() {
        return new StripePaymentProperties(
                "not-a-real-stripe-key",
                WEBHOOK_SECRET,
                "https://sassedo-fe.vercel.app/"
        );
    }

    private Purchase purchase() {
        Purchase purchase = new Purchase();
        purchase.setId(42L);
        purchase.setBuyerId(7L);
        purchase.setPackageId(5L);
        purchase.setPromotionId(8L);
        purchase.setListingType(ListingType.RENTAL);
        purchase.setListingId(11L);
        purchase.setAmountCents(1299);
        purchase.setCurrency("EUR");
        return purchase;
    }

    private String eventPayload(String eventType, String paymentStatus, String source) {
        return """
                {
                  "id": "evt_test_123",
                  "object": "event",
                  "api_version": "%s",
                  "type": "%s",
                  "data": {
                    "object": {
                      "id": "cs_test_123",
                      "object": "checkout.session",
                      "payment_status": "%s",
                      "metadata": {
                        "source": "%s",
                        "purchaseId": "42"
                      }
                    }
                  }
                }
                """.formatted(Stripe.API_VERSION, eventType, paymentStatus, source);
    }

    private String signature(String payload) throws Exception {
        long timestamp = Webhook.Util.getTimeNow();
        String digest = Webhook.Util.computeHmacSha256(
                WEBHOOK_SECRET, timestamp + "." + payload);
        return "t=" + timestamp + ",v1=" + digest;
    }

    @Configuration(proxyBeanMethods = false)
    @Import(StripePaymentProvider.class)
    static class StripeProviderTestConfiguration {
    }
}
