package server.sassedo.promotion.controller;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import server.sassedo.common.data.network.response.PageMeta;
import server.sassedo.common.data.network.response.PagedResponse;
import server.sassedo.model.GenericResponse;
import server.sassedo.promotion.data.dto.Payment;
import server.sassedo.promotion.data.network.response.PaymentResponse;
import server.sassedo.promotion.service.PaymentService;
import server.sassedo.security.jwt.JwtUtils;

import java.util.List;
import java.util.stream.Collectors;

import static server.sassedo.utils.ServerUtils.getUserId;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;
    private final JwtUtils jwtUtils;

    /** Public provider webhook sink (Stripe-ready). Verified inside the active provider. */
    @PostMapping("/webhook/{provider}")
    public ResponseEntity<?> webhook(@PathVariable String provider,
                                     @RequestHeader(value = "Stripe-Signature", required = false) String signature,
                                     @RequestBody(required = false) String body) {
        paymentService.handleWebhook(provider, body, signature);
        return ResponseEntity.ok(new GenericResponse("received"));
    }

    @GetMapping("/mine")
    public ResponseEntity<?> mine(HttpServletRequest httpRequest) {
        Long userId = getUserId(httpRequest, jwtUtils);
        List<PaymentResponse> content = paymentService.getByBuyer(userId).stream()
                .map(PromotionMapper::toPaymentResponse).collect(Collectors.toList());
        return ResponseEntity.ok(content);
    }

    @GetMapping("/admin/all")
    public ResponseEntity<?> adminAll(@RequestParam(defaultValue = "0") int page,
                                      @RequestParam(defaultValue = "40") int size) {
        Page<Payment> payments = paymentService.adminAll(PageRequest.of(page, size));
        List<PaymentResponse> content = payments.getContent().stream()
                .map(PromotionMapper::toPaymentResponse).collect(Collectors.toList());
        PageMeta meta = new PageMeta(payments.getNumber(), payments.getTotalPages(), payments.getTotalElements());
        return ResponseEntity.ok(new PagedResponse<>(content, meta));
    }
}
