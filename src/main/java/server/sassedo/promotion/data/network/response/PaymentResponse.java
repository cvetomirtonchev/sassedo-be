package server.sassedo.promotion.data.network.response;

import lombok.Getter;
import lombok.Setter;
import server.sassedo.promotion.common.PaymentProviderType;
import server.sassedo.promotion.common.PaymentStatus;

import java.time.LocalDateTime;

@Getter
@Setter
public class PaymentResponse {

    private Long id;
    private Long purchaseId;
    private PaymentProviderType provider;
    private String providerRef;
    private PaymentStatus status;
    private int amountCents;
    private String currency;
    private LocalDateTime createdAt;
}
