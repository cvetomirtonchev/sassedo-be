package server.sassedo.report.data.network.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import server.sassedo.promotion.common.ListingType;
import server.sassedo.report.data.dto.ReportReason;
import server.sassedo.report.data.dto.ReportStatus;

import java.time.LocalDateTime;

@Getter
@Setter
@AllArgsConstructor
public class ListingReportResponse {
    private Long id;
    private Long reporterUserId;
    private ListingType listingType;
    private Long listingId;
    private ReportReason reason;
    private String details;
    private ReportStatus status;
    private LocalDateTime createdAt;
}
