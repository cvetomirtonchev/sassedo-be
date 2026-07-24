package server.sassedo.report.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import server.sassedo.model.GenericException;
import server.sassedo.report.data.dto.ListingReport;
import server.sassedo.report.data.network.request.CreateListingReportRequest;
import server.sassedo.report.data.network.request.UpdateReportStatusRequest;
import server.sassedo.report.service.ListingReportService;
import server.sassedo.security.jwt.JwtUtils;

import java.util.Map;

import static server.sassedo.utils.ServerUtils.getUserId;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/listing-reports")
@RequiredArgsConstructor
public class ListingReportController {

    private final ListingReportService reportService;
    private final ListingReportMapper reportMapper;
    private final JwtUtils jwtUtils;

    @PostMapping
    public ResponseEntity<?> create(@Valid @RequestBody CreateListingReportRequest request, HttpServletRequest httpRequest) {
        Long userId = getUserId(httpRequest, jwtUtils);
        try {
            ListingReport report = reportService.create(userId, request);
            return ResponseEntity.ok(reportMapper.map(report));
        } catch (GenericException e) {
            return ResponseEntity.badRequest().body(e.getErrorResponse());
        }
    }

    @GetMapping("/admin/all")
    public ResponseEntity<?> getAll() {
        return ResponseEntity.ok(reportMapper.mapAdmin(reportService.getAll()));
    }

    @GetMapping("/admin/open-count")
    public ResponseEntity<?> getOpenCount() {
        return ResponseEntity.ok(Map.of("count", reportService.countOpen()));
    }

    @PatchMapping("/admin/{id}/status")
    public ResponseEntity<?> updateStatus(@PathVariable Long id, @Valid @RequestBody UpdateReportStatusRequest request) {
        try {
            ListingReport report = reportService.updateStatus(id, request.getStatus());
            return ResponseEntity.ok(reportMapper.mapAdmin(report));
        } catch (GenericException e) {
            return ResponseEntity.badRequest().body(e.getErrorResponse());
        }
    }
}
