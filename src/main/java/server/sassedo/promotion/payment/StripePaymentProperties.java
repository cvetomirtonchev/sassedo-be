package server.sassedo.promotion.payment;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "sassedo.payments.stripe")
public record StripePaymentProperties(
        @NotBlank String secretKey,
        @NotBlank String webhookSecret,
        @NotBlank String clientUrl
) {
}
