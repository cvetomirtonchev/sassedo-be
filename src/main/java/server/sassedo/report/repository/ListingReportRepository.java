package server.sassedo.report.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import server.sassedo.promotion.common.ListingType;
import server.sassedo.report.data.dto.ListingReport;
import server.sassedo.report.data.dto.ReportStatus;

import java.util.List;

public interface ListingReportRepository extends JpaRepository<ListingReport, Long> {
    List<ListingReport> findAllByOrderByCreatedAtDesc();

    long countByStatus(ReportStatus status);

    boolean existsByReporterUserIdAndListingTypeAndListingId(Long reporterUserId, ListingType listingType, Long listingId);
}
