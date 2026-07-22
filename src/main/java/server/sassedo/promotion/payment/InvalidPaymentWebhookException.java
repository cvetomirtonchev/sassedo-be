package server.sassedo.promotion.payment;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class InvalidPaymentWebhookException extends RuntimeException {

    public InvalidPaymentWebhookException(String message) {
        super(message);
    }

    public InvalidPaymentWebhookException(String message, Throwable cause) {
        super(message, cause);
    }
}
