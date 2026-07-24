package server.sassedo.listing.rental.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageRequest;
import server.sassedo.listing.common.ListingOwnerEditPolicy;
import server.sassedo.listing.common.ListingStatus;
import server.sassedo.listing.common.notification.ListingModerationDecisionEvent;
import server.sassedo.listing.rental.data.dto.RentalListing;
import server.sassedo.listing.rental.data.network.request.RentalListingRequest;
import server.sassedo.listing.rental.repository.RentalListingPhotoRepository;
import server.sassedo.listing.rental.repository.RentalListingRepository;
import server.sassedo.location.repository.CityRepository;
import server.sassedo.location.repository.CountryRepository;
import server.sassedo.model.GenericException;
import server.sassedo.model.GenericExceptionCode;
import server.sassedo.promotion.common.ListingType;
import server.sassedo.promotion.service.PromotionService;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RentalListingServiceImplTest {

    @Mock
    private RentalListingRepository listingRepository;
    @Mock
    private RentalListingPhotoRepository photoRepository;
    @Mock
    private CountryRepository countryRepository;
    @Mock
    private CityRepository cityRepository;
    @Mock
    private PromotionService promotionService;
    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private RentalListingServiceImpl service;

    @Test
    void adminSearchParsesNumericIdAndForwardsCityFilter() {
        PageRequest pageable = PageRequest.of(0, 40);

        service.adminSearch(ListingStatus.PENDING, " 123 ", 9L, pageable);

        verify(listingRepository).adminSearch(
                ListingStatus.PENDING, "123", 123L, 9L, pageable);
    }

    @Test
    void adminSearchKeepsTextSearchWithoutListingId() {
        PageRequest pageable = PageRequest.of(1, 40);

        service.adminSearch(null, " Downtown ", null, pageable);

        verify(listingRepository).adminSearch(
                null, "Downtown", null, null, pageable);
    }

    @Test
    void ownerUpdateIncrementsEditCountAndSetsPending() throws Exception {
        RentalListing listing = ownedListing(1L, 7L, ListingStatus.ACTIVE, 2);
        when(listingRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(listing));
        when(listingRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        RentalListingRequest request = new RentalListingRequest();
        request.setTitle("Updated");

        RentalListing saved = service.update(1L, 7L, false, request);

        assertThat(saved.getOwnerEditCount()).isEqualTo(3);
        assertThat(saved.getStatus()).isEqualTo(ListingStatus.PENDING);
    }

    @Test
    void fifthOwnerUpdateSucceeds() throws Exception {
        RentalListing listing = ownedListing(1L, 7L, ListingStatus.ACTIVE, 4);
        when(listingRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(listing));
        when(listingRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.update(1L, 7L, false, new RentalListingRequest());

        assertThat(listing.getOwnerEditCount()).isEqualTo(5);
        assertThat(ListingOwnerEditPolicy.remainingEdits(listing.getOwnerEditCount())).isZero();
    }

    @Test
    void sixthOwnerUpdateFailsWithoutSave() {
        RentalListing listing = ownedListing(1L, 7L, ListingStatus.ACTIVE, 5);
        when(listingRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(listing));

        assertThatThrownBy(() -> service.update(1L, 7L, false, new RentalListingRequest()))
                .isInstanceOf(GenericException.class)
                .hasFieldOrPropertyWithValue("code", GenericExceptionCode.LISTING_EDIT_LIMIT_REACHED);

        verify(listingRepository, never()).save(any());
    }

    @Test
    void adminUpdateDoesNotIncrementEditCount() throws Exception {
        RentalListing listing = ownedListing(1L, 7L, ListingStatus.ACTIVE, 3);
        when(listingRepository.findById(1L)).thenReturn(Optional.of(listing));
        when(listingRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.update(1L, null, true, new RentalListingRequest());

        assertThat(listing.getOwnerEditCount()).isEqualTo(3);
        assertThat(listing.getStatus()).isEqualTo(ListingStatus.ACTIVE);
        verify(listingRepository, never()).findByIdForUpdate(any());
    }

    @Test
    void ownerUpdatePreservesRejectedStatus() throws Exception {
        RentalListing listing = ownedListing(1L, 7L, ListingStatus.REJECTED, 0);
        listing.setRejectionReason("Incomplete photos");
        when(listingRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(listing));
        when(listingRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.update(1L, 7L, false, new RentalListingRequest());

        assertThat(listing.getStatus()).isEqualTo(ListingStatus.REJECTED);
        assertThat(listing.getRejectionReason()).isEqualTo("Incomplete photos");
    }

    @Test
    void ownerUpdatePreservesExpiredStatus() throws Exception {
        RentalListing listing = ownedListing(1L, 7L, ListingStatus.EXPIRED, 0);
        when(listingRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(listing));
        when(listingRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.update(1L, 7L, false, new RentalListingRequest());

        assertThat(listing.getStatus()).isEqualTo(ListingStatus.EXPIRED);
    }

    @Test
    void rejectRequiresReason() {
        assertThatThrownBy(() -> service.setStatus(1L, ListingStatus.REJECTED, "  "))
                .isInstanceOf(GenericException.class)
                .hasFieldOrPropertyWithValue("code", GenericExceptionCode.MODERATION_REASON_REQUIRED);

        verify(listingRepository, never()).save(any());
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    void rejectAndApprovePublishDecisionEventsWithCurrentListingData() throws Exception {
        RentalListing listing = ownedListing(1L, 7L, ListingStatus.PENDING, 0);
        listing.setTitle("Sunny apartment");
        when(listingRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(listing));
        when(listingRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.setStatus(1L, ListingStatus.REJECTED, "  Policy violation  ");
        assertThat(listing.getRejectionReason()).isEqualTo("Policy violation");

        service.setStatus(1L, ListingStatus.ACTIVE, null);
        assertThat(listing.getRejectionReason()).isNull();

        ArgumentCaptor<Object> eventCaptor = ArgumentCaptor.forClass(Object.class);
        verify(eventPublisher, times(2)).publishEvent(eventCaptor.capture());
        assertThat(eventCaptor.getAllValues())
                .containsExactly(
                        new ListingModerationDecisionEvent(
                                ListingType.RENTAL,
                                1L,
                                7L,
                                "Sunny apartment",
                                ListingStatus.REJECTED,
                                "Policy violation"
                        ),
                        new ListingModerationDecisionEvent(
                                ListingType.RENTAL,
                                1L,
                                7L,
                                "Sunny apartment",
                                ListingStatus.ACTIVE,
                                null
                        )
                );
    }

    @Test
    void setStatusDoesNotPublishForUnchangedOrNonDecisionStatus() throws Exception {
        RentalListing listing = ownedListing(1L, 7L, ListingStatus.ACTIVE, 0);
        when(listingRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(listing));
        when(listingRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.setStatus(1L, ListingStatus.ACTIVE, null);
        service.setStatus(1L, ListingStatus.INACTIVE, null);

        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    void resubmitClearsReasonAndSetsPending() throws Exception {
        RentalListing listing = ownedListing(1L, 7L, ListingStatus.REJECTED, 2);
        listing.setRejectionReason("Fix title");
        when(listingRepository.findById(1L)).thenReturn(Optional.of(listing));
        when(listingRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        RentalListing saved = service.resubmit(1L, 7L);

        assertThat(saved.getStatus()).isEqualTo(ListingStatus.PENDING);
        assertThat(saved.getRejectionReason()).isNull();
        assertThat(saved.getOwnerEditCount()).isEqualTo(2);
    }

    @Test
    void resubmitRequiresOwnerAndRejectedState() {
        RentalListing listing = ownedListing(1L, 7L, ListingStatus.PENDING, 0);
        when(listingRepository.findById(1L)).thenReturn(Optional.of(listing));

        assertThatThrownBy(() -> service.resubmit(1L, 7L))
                .isInstanceOf(GenericException.class)
                .hasFieldOrPropertyWithValue("code", GenericExceptionCode.INVALID_LISTING_STATUS);

        assertThatThrownBy(() -> service.resubmit(1L, 99L))
                .isInstanceOf(GenericException.class)
                .hasFieldOrPropertyWithValue("code", GenericExceptionCode.NOT_LISTING_OWNER);
    }

    @Test
    void ownerDeleteOnlyWhenExpired() throws Exception {
        RentalListing listing = ownedListing(1L, 7L, ListingStatus.EXPIRED, 0);
        when(listingRepository.findById(1L)).thenReturn(Optional.of(listing));

        service.deleteByOwner(1L, 7L);

        verify(promotionService).cancelDeferredForListing(any(), eq(1L));
        verify(listingRepository).delete(listing);
    }

    @Test
    void ownerDeleteRejectedWhenNotExpired() {
        RentalListing listing = ownedListing(1L, 7L, ListingStatus.REJECTED, 0);
        when(listingRepository.findById(1L)).thenReturn(Optional.of(listing));

        assertThatThrownBy(() -> service.deleteByOwner(1L, 7L))
                .isInstanceOf(GenericException.class)
                .hasFieldOrPropertyWithValue("code", GenericExceptionCode.INVALID_LISTING_STATUS);

        verify(listingRepository, never()).delete(any(RentalListing.class));
    }

    private static RentalListing ownedListing(long id, long ownerId, ListingStatus status, int editCount) {
        RentalListing listing = new RentalListing();
        listing.setId(id);
        listing.setOwnerId(ownerId);
        listing.setStatus(status);
        listing.setOwnerEditCount(editCount);
        return listing;
    }
}
