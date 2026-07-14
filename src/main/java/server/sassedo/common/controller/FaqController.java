package server.sassedo.common.controller;

import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import server.sassedo.common.data.dto.Faq;
import server.sassedo.common.data.dto.FaqLocale;
import server.sassedo.common.data.network.request.AddFaqRequest;
import server.sassedo.common.data.network.request.UpdateFaqRequest;
import server.sassedo.common.data.network.response.FaqAdminResponse;
import server.sassedo.common.data.network.response.FaqResponse;
import server.sassedo.common.service.faq.FaqService;
import server.sassedo.model.GenericException;

import java.util.List;
import java.util.stream.Collectors;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/faq")
public class FaqController {

    @Autowired
    private FaqService faqService;

    @GetMapping("/all")
    public ResponseEntity<?> getFaqs(@RequestParam(defaultValue = "bg") String locale) {
        try {
            FaqLocale faqLocale = FaqLocale.fromCode(locale);
            List<FaqResponse> faqs = faqService.getAllOrdered().stream()
                    .map(faq -> toResponse(faq, faqLocale))
                    .collect(Collectors.toList());
            return ResponseEntity.ok(faqs);
        } catch (GenericException e) {
            return ResponseEntity.badRequest().body(e.getErrorResponse());
        }
    }

    @GetMapping("/admin/all")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getAllForAdmin() {
        List<FaqAdminResponse> faqs = faqService.getAllOrdered().stream()
                .map(this::toAdminResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(faqs);
    }

    @GetMapping("/admin/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getFaqById(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(toAdminResponse(faqService.getById(id)));
        } catch (GenericException e) {
            return ResponseEntity.badRequest().body(e.getErrorResponse());
        }
    }

    @PostMapping("/admin/add")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> addFaq(@Valid @RequestBody AddFaqRequest addFaqRequest) {
        Faq faq = faqService.add(addFaqRequest);
        return ResponseEntity.ok(toAdminResponse(faq));
    }

    @PutMapping("/admin/update")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> updateFaq(@Valid @RequestBody UpdateFaqRequest updateFaqRequest) {
        try {
            Faq faq = faqService.update(updateFaqRequest);
            return ResponseEntity.ok(toAdminResponse(faq));
        } catch (GenericException e) {
            return ResponseEntity.badRequest().body(e.getErrorResponse());
        }
    }

    @DeleteMapping("/admin/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> deleteFaq(@PathVariable Long id) {
        faqService.delete(id);
        return ResponseEntity.ok().build();
    }

    private FaqResponse toResponse(Faq faq, FaqLocale locale) {
        return new FaqResponse(faq.getId(), faq.getQuestion(locale), faq.getAnswer(locale));
    }

    private FaqAdminResponse toAdminResponse(Faq faq) {
        return new FaqAdminResponse(faq.getId(), faq.getQuestionBg(), faq.getQuestionEn(),
                faq.getAnswerBg(), faq.getAnswerEn(), faq.getSortOrder());
    }
}
