package server.sassedo.engagement.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import server.sassedo.listing.rental.data.network.response.RentalListingResponse;
import server.sassedo.promotion.common.ListingType;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EngagementEnricherTest {

    @Mock
    private FavoriteService favoriteService;
    @Mock
    private ListingViewService listingViewService;

    @InjectMocks
    private EngagementEnricher enricher;

    @Test
    void enrichBatchLoadsViewCountsForMineLists() {
        RentalListingResponse first = new RentalListingResponse();
        first.setId(10L);
        RentalListingResponse second = new RentalListingResponse();
        second.setId(20L);

        when(favoriteService.counts(ListingType.RENTAL, List.of(10L, 20L))).thenReturn(Map.of(10L, 2L));
        when(favoriteService.favoritedIds(5L, ListingType.RENTAL, List.of(10L, 20L))).thenReturn(java.util.Set.of(20L));
        when(listingViewService.counts(eq(ListingType.RENTAL), eq(List.of(10L, 20L))))
                .thenReturn(Map.of(10L, 7L, 20L, 3L));

        enricher.enrich(ListingType.RENTAL, List.of(first, second), 5L, true);

        assertThat(first.getViewCount()).isEqualTo(7L);
        assertThat(second.getViewCount()).isEqualTo(3L);
        verify(listingViewService).counts(ListingType.RENTAL, List.of(10L, 20L));
    }
}
