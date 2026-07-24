package server.sassedo.common.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import server.sassedo.common.data.dto.FaqLocale;
import server.sassedo.common.data.dto.Testimonial;
import server.sassedo.common.data.network.request.AddTestimonialRequest;
import server.sassedo.common.data.network.request.UpdateTestimonialRequest;
import server.sassedo.common.data.network.response.TestimonialAdminResponse;
import server.sassedo.common.data.network.response.TestimonialPublicResponse;
import server.sassedo.common.service.testimonial.TestimonialService;
import server.sassedo.model.GenericException;

import java.util.List;
import java.util.stream.Collectors;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/testimonials")
@RequiredArgsConstructor
public class TestimonialController {

    private final TestimonialService testimonialService;

    @GetMapping("/random")
    public ResponseEntity<?> getRandom(@RequestParam(defaultValue = "bg") String locale) {
        try {
            FaqLocale testimonialLocale = FaqLocale.fromCode(locale);
            List<TestimonialPublicResponse> items = testimonialService.randomEnabled().stream()
                    .map(testimonial -> toPublicResponse(testimonial, testimonialLocale))
                    .collect(Collectors.toList());
            return ResponseEntity.ok(items);
        } catch (GenericException e) {
            return ResponseEntity.badRequest().body(e.getErrorResponse());
        }
    }

    @GetMapping("/admin")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getAllForAdmin() {
        List<TestimonialAdminResponse> items = testimonialService.getAll().stream()
                .map(this::toAdminResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(items);
    }

    @GetMapping("/admin/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getByIdForAdmin(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(toAdminResponse(testimonialService.getById(id)));
        } catch (GenericException e) {
            return ResponseEntity.badRequest().body(e.getErrorResponse());
        }
    }

    @PostMapping("/admin")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> add(@Valid @RequestBody AddTestimonialRequest request) {
        return ResponseEntity.ok(toAdminResponse(testimonialService.add(request)));
    }

    @PutMapping("/admin")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> update(@Valid @RequestBody UpdateTestimonialRequest request) {
        try {
            return ResponseEntity.ok(toAdminResponse(testimonialService.update(request)));
        } catch (GenericException e) {
            return ResponseEntity.badRequest().body(e.getErrorResponse());
        }
    }

    @DeleteMapping("/admin/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> delete(@PathVariable Long id) {
        try {
            testimonialService.delete(id);
            return ResponseEntity.ok().build();
        } catch (GenericException e) {
            return ResponseEntity.badRequest().body(e.getErrorResponse());
        }
    }

    private TestimonialPublicResponse toPublicResponse(Testimonial testimonial, FaqLocale locale) {
        TestimonialPublicResponse response = new TestimonialPublicResponse();
        response.setId(testimonial.getId());
        response.setQuote(testimonial.getQuote(locale));
        response.setAuthor(testimonial.getAuthor(locale));
        response.setRole(testimonial.getRole(locale));
        return response;
    }

    private TestimonialAdminResponse toAdminResponse(Testimonial testimonial) {
        TestimonialAdminResponse response = new TestimonialAdminResponse();
        response.setId(testimonial.getId());
        response.setQuoteBg(testimonial.getQuoteBg());
        response.setQuoteEn(testimonial.getQuoteEn());
        response.setAuthorBg(testimonial.getAuthorBg());
        response.setAuthorEn(testimonial.getAuthorEn());
        response.setRoleBg(testimonial.getRoleBg());
        response.setRoleEn(testimonial.getRoleEn());
        response.setEnabled(testimonial.isEnabled());
        return response;
    }
}
