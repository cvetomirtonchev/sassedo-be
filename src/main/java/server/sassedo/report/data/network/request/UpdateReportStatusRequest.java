package server.sassedo.report.data.network.request;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import server.sassedo.report.data.dto.ReportStatus;

@Getter
@Setter
public class UpdateReportStatusRequest {

    @NotNull
    private ReportStatus status;
}
