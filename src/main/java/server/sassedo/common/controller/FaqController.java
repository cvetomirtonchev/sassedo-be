package server.sassedo.common.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import server.sassedo.common.data.dto.Faq;
import server.sassedo.common.data.network.request.AddFaqRequest;
import server.sassedo.common.data.network.request.UpdateFaqRequest;
import server.sassedo.common.data.network.response.FaqResponse;
import server.sassedo.common.service.faq.FaqService;
import server.sassedo.model.GenericException;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/faq")
public class FaqController {

    @Autowired
    private FaqService faqService;

    @GetMapping("/all")
    public ResponseEntity<?> getFaqs() {
        List<Faq> faqs = faqService.getAll();
        return ResponseEntity.ok(faqs.stream().map(this::convertFaqToResponse).collect(Collectors.toList()));
    }

    @GetMapping
    public ResponseEntity<?> getFaqById(@RequestParam Long id) {
        try {
            Faq faq = faqService.getById(id);
            return ResponseEntity.ok(convertFaqToResponse(faq));
        } catch (GenericException e) {
            return ResponseEntity.badRequest().body(e.getErrorResponse());
        }
    }

    @PostMapping("admin/add")
    public ResponseEntity<?> addFaq(@RequestBody AddFaqRequest addFaqRequest) {
        Faq faq = faqService.add(addFaqRequest);
        return ResponseEntity.ok(convertFaqToResponse(faq));
    }

    @PutMapping("admin/update")
    public ResponseEntity<?> updateFaq(@RequestBody UpdateFaqRequest updateFaqRequest) {
        try {
            Faq faq = faqService.update(updateFaqRequest);
            return ResponseEntity.ok(convertFaqToResponse(faq));
        } catch (GenericException e) {
            return ResponseEntity.badRequest().body(e.getErrorResponse());
        }
    }

    @DeleteMapping("admin/{id}")
    public ResponseEntity<?> deleteFaq(@PathVariable Long id) {
        faqService.delete(id);
        return ResponseEntity.ok().build();
    }

    private FaqResponse convertFaqToResponse(Faq faq) {
        return new FaqResponse(faq.getId(), faq.getQuestion(), faq.getAnswer());
    }
}
