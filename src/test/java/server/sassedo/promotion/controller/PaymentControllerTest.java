package server.sassedo.promotion.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import server.sassedo.promotion.payment.InvalidPaymentWebhookException;
import server.sassedo.promotion.service.PaymentService;
import server.sassedo.security.jwt.JwtUtils;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class PaymentControllerTest {

    private PaymentService paymentService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        paymentService = mock(PaymentService.class);
        PaymentController controller = new PaymentController(paymentService, mock(JwtUtils.class));
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    void webhook_forwardsExactPayloadAndSignature() throws Exception {
        String payload = "{\"id\":\"evt_123\", \"type\":\"checkout.session.completed\"}";

        mockMvc.perform(post("/api/payments/webhook/stripe")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Stripe-Signature", "t=123,v1=signed")
                        .content(payload))
                .andExpect(status().isOk());

        verify(paymentService).handleWebhook("stripe", payload, "t=123,v1=signed");
    }

    @Test
    void webhook_returnsBadRequestForInvalidSignature() throws Exception {
        String payload = "{\"id\":\"evt_123\"}";
        doThrow(new InvalidPaymentWebhookException("Invalid Stripe webhook signature"))
                .when(paymentService).handleWebhook("stripe", payload, "invalid");

        mockMvc.perform(post("/api/payments/webhook/stripe")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Stripe-Signature", "invalid")
                        .content(payload))
                .andExpect(status().isBadRequest());
    }
}
