package server.sassedo.common.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import server.sassedo.common.data.dto.HelperText;
import server.sassedo.common.data.dto.HelperTextType;
import server.sassedo.common.data.network.request.UpdateHelperText;
import server.sassedo.common.data.network.response.HelperTextResponse;
import server.sassedo.common.service.helpertexts.HelperTextsService;
import server.sassedo.model.GenericException;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/common")
public class CommonsController {

    @Autowired
    private HelperTextsService helperTextsService;

    @GetMapping("/helper-texts")
    public ResponseEntity<?> getHelperTexts(@RequestParam(required = false) Long id) {
        try {
            List<HelperText> helperTexts = helperTextsService.getHelperTexts(id);
            return ResponseEntity.ok(helperTexts.stream().map(this::convertToResponse).collect(Collectors.toList()));
        } catch (GenericException e) {
            return ResponseEntity.badRequest().body(e.getErrorResponse());
        }
    }

    @PutMapping("/admin/helper-texts/update")
    public ResponseEntity<?> updateHelperText(@RequestBody UpdateHelperText updateHelperText) {
        try {
            HelperText helperText = helperTextsService.updateHelperText(updateHelperText.getId(), updateHelperText.getValue());
            return ResponseEntity.ok(convertToResponse(helperText));
        } catch (GenericException e) {
            return ResponseEntity.badRequest().body(e.getErrorResponse());
        }
    }

    private HelperTextResponse convertToResponse(HelperText helperText) {
        return new HelperTextResponse(helperText.getId(), HelperTextType.fromId(helperText.getId()).name(), helperText.getValue());
    }
}
