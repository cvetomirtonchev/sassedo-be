package server.sassedo.report.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import server.sassedo.report.data.dto.ListingReport;
import server.sassedo.report.data.network.response.ListingReportResponse;
import server.sassedo.user.data.projection.UserReportIdentity;
import server.sassedo.user.repository.UserRepository;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Maps listing reports and batch-enriches them with the reporting user's admin-visible identity.
 */
@Component
@RequiredArgsConstructor
public class ListingReportMapper {

    private final UserRepository userRepository;

    public ListingReportResponse map(ListingReport report) {
        if (report == null) {
            return null;
        }
        return map(report, null);
    }

    public ListingReportResponse mapAdmin(ListingReport report) {
        if (report == null) {
            return null;
        }
        return mapAdmin(List.of(report)).get(0);
    }

    public List<ListingReportResponse> mapAdmin(List<ListingReport> reports) {
        if (reports == null || reports.isEmpty()) {
            return List.of();
        }

        Set<Long> reporterIds = reports.stream()
                .map(ListingReport::getReporterUserId)
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        Map<Long, UserReportIdentity> reportersById = reporterIds.isEmpty()
                ? Collections.emptyMap()
                : userRepository.findReportIdentitiesByIdIn(reporterIds).stream()
                        .collect(Collectors.toMap(UserReportIdentity::getId, Function.identity()));

        return reports.stream()
                .map(report -> map(report, reportersById.get(report.getReporterUserId())))
                .toList();
    }

    private ListingReportResponse map(ListingReport report, UserReportIdentity reporter) {
        return new ListingReportResponse(
                report.getId(),
                report.getReporterUserId(),
                reporter != null ? reporter.getName() : null,
                reporter != null ? reporter.getEmail() : null,
                report.getListingType(),
                report.getListingId(),
                report.getReason(),
                report.getDetails(),
                report.getStatus(),
                report.getCreatedAt()
        );
    }
}
