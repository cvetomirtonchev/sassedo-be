package server.sassedo.report.data.network.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import server.sassedo.promotion.common.ListingType;
import server.sassedo.report.data.dto.ReportReason;

@Getter
@Setter
public class CreateListingReportRequest {

    @NotNull
    private ListingType listingType;

    @NotNull
    private Long listingId;

    @NotNull
    private ReportReason reason;

    @Size(max = 2000)
    private String details;
}
