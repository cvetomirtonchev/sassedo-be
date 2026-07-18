package server.sassedo.report.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import server.sassedo.listing.rental.repository.RentalListingRepository;
import server.sassedo.listing.roommate.repository.RoommateListingRepository;
import server.sassedo.listing.search.repository.ApartmentSearchRepository;
import server.sassedo.model.GenericException;
import server.sassedo.model.GenericExceptionCode;
import server.sassedo.promotion.common.ListingType;
import server.sassedo.report.data.dto.ListingReport;
import server.sassedo.report.data.dto.ReportStatus;
import server.sassedo.report.data.network.request.CreateListingReportRequest;
import server.sassedo.report.repository.ListingReportRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ListingReportServiceImpl implements ListingReportService {

    private final ListingReportRepository reportRepository;
    private final RentalListingRepository rentalListingRepository;
    private final RoommateListingRepository roommateListingRepository;
    private final ApartmentSearchRepository apartmentSearchRepository;

    @Override
    @Transactional
    public ListingReport create(Long reporterUserId, CreateListingReportRequest request) throws GenericException {
        if (reporterUserId == null) {
            throw new GenericException(GenericExceptionCode.USER_NOT_FOUND, "User not found");
        }

        ListingType listingType = request.getListingType();
        Long listingId = request.getListingId();

        if (!listingExists(listingType, listingId)) {
            throw new GenericException(GenericExceptionCode.LISTING_NOT_FOUND, "Listing not found");
        }

        if (reportRepository.existsByReporterUserIdAndListingTypeAndListingId(reporterUserId, listingType, listingId)) {
            throw new GenericException(GenericExceptionCode.ALREADY_REPORTED, "You have already reported this listing");
        }

        ListingReport report = new ListingReport();
        report.setReporterUserId(reporterUserId);
        report.setListingType(listingType);
        report.setListingId(listingId);
        report.setReason(request.getReason());
        report.setDetails(request.getDetails());
        report.setStatus(ReportStatus.OPEN);
        return reportRepository.save(report);
    }

    @Override
    public List<ListingReport> getAll() {
        return reportRepository.findAllByOrderByCreatedAtDesc();
    }

    @Override
    public long countOpen() {
        return reportRepository.countByStatus(ReportStatus.OPEN);
    }

    @Override
    @Transactional
    public ListingReport updateStatus(Long id, ReportStatus status) throws GenericException {
        ListingReport report = reportRepository.findById(id)
                .orElseThrow(() -> new GenericException(GenericExceptionCode.REPORT_NOT_FOUND, "Report not found"));
        report.setStatus(status);
        return reportRepository.save(report);
    }

    private boolean listingExists(ListingType listingType, Long listingId) {
        if (listingType == null || listingId == null) {
            return false;
        }
        return switch (listingType) {
            case RENTAL -> rentalListingRepository.existsById(listingId);
            case ROOMMATE -> roommateListingRepository.existsById(listingId);
            case SEARCH -> apartmentSearchRepository.existsById(listingId);
        };
    }
}
