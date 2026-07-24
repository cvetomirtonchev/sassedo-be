package server.sassedo.report.controller;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import server.sassedo.report.data.dto.ListingReport;
import server.sassedo.report.data.network.response.ListingReportResponse;
import server.sassedo.user.data.projection.UserReportIdentity;
import server.sassedo.user.repository.UserRepository;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ListingReportMapperTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private ListingReportMapper mapper;

    @Test
    void mapAllBatchLoadsReporterIdentity() {
        ListingReport first = report(1L, 7L);
        ListingReport second = report(2L, 8L);
        UserReportIdentity firstReporter = identity(7L, "Alice Smith", "alice@example.com");
        UserReportIdentity secondReporter = identity(8L, "Bob Jones", "bob@example.com");
        when(userRepository.findReportIdentitiesByIdIn(Set.of(7L, 8L)))
                .thenReturn(List.of(firstReporter, secondReporter));

        List<ListingReportResponse> responses = mapper.mapAdmin(List.of(first, second));

        assertThat(responses).hasSize(2);
        assertThat(responses.get(0).getReporterUserId()).isEqualTo(7L);
        assertThat(responses.get(0).getReporterName()).isEqualTo("Alice Smith");
        assertThat(responses.get(0).getReporterEmail()).isEqualTo("alice@example.com");
        assertThat(responses.get(1).getReporterUserId()).isEqualTo(8L);
        assertThat(responses.get(1).getReporterName()).isEqualTo("Bob Jones");
        assertThat(responses.get(1).getReporterEmail()).isEqualTo("bob@example.com");
        verify(userRepository).findReportIdentitiesByIdIn(Set.of(7L, 8L));
    }

    @Test
    void mapRetainsReporterIdWhenUserIsUnavailable() {
        ListingReport report = report(1L, 99L);
        when(userRepository.findReportIdentitiesByIdIn(Set.of(99L))).thenReturn(List.of());

        ListingReportResponse response = mapper.mapAdmin(report);

        assertThat(response.getReporterUserId()).isEqualTo(99L);
        assertThat(response.getReporterName()).isNull();
        assertThat(response.getReporterEmail()).isNull();
    }

    @Test
    void mapDoesNotExposeIdentityOnTheReporterSubmitResponse() {
        ListingReportResponse response = mapper.map(report(1L, 7L));

        assertThat(response.getReporterUserId()).isEqualTo(7L);
        assertThat(response.getReporterName()).isNull();
        assertThat(response.getReporterEmail()).isNull();
        verifyNoInteractions(userRepository);
    }

    private ListingReport report(Long id, Long reporterUserId) {
        ListingReport report = new ListingReport();
        report.setId(id);
        report.setReporterUserId(reporterUserId);
        return report;
    }

    private UserReportIdentity identity(Long id, String name, String email) {
        UserReportIdentity identity = mock(UserReportIdentity.class);
        when(identity.getId()).thenReturn(id);
        when(identity.getName()).thenReturn(name);
        when(identity.getEmail()).thenReturn(email);
        return identity;
    }
}
