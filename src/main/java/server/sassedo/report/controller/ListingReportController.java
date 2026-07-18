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
import server.sassedo.report.data.network.response.ListingReportResponse;
import server.sassedo.report.service.ListingReportService;
import server.sassedo.security.jwt.JwtUtils;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static server.sassedo.utils.ServerUtils.getUserId;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/listing-reports")
@RequiredArgsConstructor
public class ListingReportController {

    private final ListingReportService reportService;
    private final JwtUtils jwtUtils;

    @PostMapping
    public ResponseEntity<?> create(@Valid @RequestBody CreateListingReportRequest request, HttpServletRequest httpRequest) {
        Long userId = getUserId(httpRequest, jwtUtils);
        try {
            ListingReport report = reportService.create(userId, request);
            return ResponseEntity.ok(convertToResponse(report));
        } catch (GenericException e) {
            return ResponseEntity.badRequest().body(e.getErrorResponse());
        }
    }

    @GetMapping("/admin/all")
    public ResponseEntity<?> getAll() {
        List<ListingReportResponse> reports = reportService.getAll()
                .stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(reports);
    }

    @GetMapping("/admin/open-count")
    public ResponseEntity<?> getOpenCount() {
        return ResponseEntity.ok(Map.of("count", reportService.countOpen()));
    }

    @PatchMapping("/admin/{id}/status")
    public ResponseEntity<?> updateStatus(@PathVariable Long id, @Valid @RequestBody UpdateReportStatusRequest request) {
        try {
            ListingReport report = reportService.updateStatus(id, request.getStatus());
            return ResponseEntity.ok(convertToResponse(report));
        } catch (GenericException e) {
            return ResponseEntity.badRequest().body(e.getErrorResponse());
        }
    }

    private ListingReportResponse convertToResponse(ListingReport report) {
        return new ListingReportResponse(
                report.getId(),
                report.getReporterUserId(),
                report.getListingType(),
                report.getListingId(),
                report.getReason(),
                report.getDetails(),
                report.getStatus(),
                report.getCreatedAt()
        );
    }
}
