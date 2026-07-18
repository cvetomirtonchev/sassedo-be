package server.sassedo.report.service;

import server.sassedo.model.GenericException;
import server.sassedo.report.data.dto.ListingReport;
import server.sassedo.report.data.dto.ReportStatus;
import server.sassedo.report.data.network.request.CreateListingReportRequest;

import java.util.List;

public interface ListingReportService {
    ListingReport create(Long reporterUserId, CreateListingReportRequest request) throws GenericException;

    List<ListingReport> getAll();

    long countOpen();

    ListingReport updateStatus(Long id, ReportStatus status) throws GenericException;
}
