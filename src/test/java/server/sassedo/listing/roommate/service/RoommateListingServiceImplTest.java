package server.sassedo.listing.roommate.service;

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
import server.sassedo.listing.roommate.data.dto.RoommateListing;
import server.sassedo.listing.roommate.data.network.request.RoommateListingRequest;
import server.sassedo.listing.roommate.repository.RoommateListingPhotoRepository;
import server.sassedo.listing.roommate.repository.RoommateListingRepository;
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
class RoommateListingServiceImplTest {

    @Mock
    private RoommateListingRepository listingRepository;
    @Mock
    private RoommateListingPhotoRepository photoRepository;
    @Mock
    private CountryRepository countryRepository;
    @Mock
    private CityRepository cityRepository;
    @Mock
    private PromotionService promotionService;
    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private RoommateListingServiceImpl service;

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
    void createNormalizesLegacyZeroOccupantsToOwnerOnly() throws Exception {
        RoommateListing saved = createAndCapture(true, 0);

        assertThat(saved.getPeopleInProperty()).isEqualTo(1);
    }

    @Test
    void createPreservesPositiveTotalOccupants() throws Exception {
        RoommateListing saved = createAndCapture(true, 3);

        assertThat(saved.getPeopleInProperty()).isEqualTo(3);
    }

    @Test
    void createClearsOccupantsForListingWithoutProperty() throws Exception {
        RoommateListing saved = createAndCapture(false, 3);

        assertThat(saved.getPeopleInProperty()).isNull();
    }

    @Test
    void createRejectsNegativeOccupants() {
        RoommateListingRequest request = request(true, -1);

        assertThatThrownBy(() -> service.create(7L, request))
                .hasMessage("People in property cannot be negative");
        verify(listingRepository, never()).save(any());
    }

    @Test
    void fifthOwnerUpdateSucceeds() throws Exception {
        RoommateListing listing = ownedListing(1L, 7L, ListingStatus.ACTIVE, 4);
        when(listingRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(listing));
        when(listingRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.update(1L, 7L, false, request(true, 2));

        assertThat(listing.getOwnerEditCount()).isEqualTo(5);
        assertThat(ListingOwnerEditPolicy.remainingEdits(listing.getOwnerEditCount())).isZero();
    }

    @Test
    void sixthOwnerUpdateFailsWithoutSave() {
        RoommateListing listing = ownedListing(1L, 7L, ListingStatus.ACTIVE, 5);
        when(listingRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(listing));

        assertThatThrownBy(() -> service.update(1L, 7L, false, request(true, 2)))
                .isInstanceOf(GenericException.class)
                .hasFieldOrPropertyWithValue("code", GenericExceptionCode.LISTING_EDIT_LIMIT_REACHED);

        verify(listingRepository, never()).save(any());
    }

    @Test
    void adminUpdateDoesNotIncrementEditCount() throws Exception {
        RoommateListing listing = ownedListing(1L, 7L, ListingStatus.ACTIVE, 3);
        when(listingRepository.findById(1L)).thenReturn(Optional.of(listing));
        when(listingRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.update(1L, null, true, request(true, 2));

        assertThat(listing.getOwnerEditCount()).isEqualTo(3);
        verify(listingRepository, never()).findByIdForUpdate(any());
    }

    @Test
    void ownerUpdatePreservesRejectedAndExpiredStatus() throws Exception {
        RoommateListing rejected = ownedListing(1L, 7L, ListingStatus.REJECTED, 0);
        rejected.setRejectionReason("Missing info");
        when(listingRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(rejected));
        when(listingRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.update(1L, 7L, false, request(true, 2));
        assertThat(rejected.getStatus()).isEqualTo(ListingStatus.REJECTED);

        RoommateListing expired = ownedListing(2L, 7L, ListingStatus.EXPIRED, 0);
        when(listingRepository.findByIdForUpdate(2L)).thenReturn(Optional.of(expired));

        service.update(2L, 7L, false, request(true, 2));
        assertThat(expired.getStatus()).isEqualTo(ListingStatus.EXPIRED);
    }

    @Test
    void rejectAndApprovePublishDecisionEventsWithCurrentListingData() throws Exception {
        RoommateListing listing = ownedListing(1L, 7L, ListingStatus.PENDING, 0);
        listing.setTitle("Room near the park");
        when(listingRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(listing));
        when(listingRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.setStatus(1L, ListingStatus.REJECTED, "  Add more details  ");
        assertThat(listing.getRejectionReason()).isEqualTo("Add more details");

        service.setStatus(1L, ListingStatus.ACTIVE, null);
        assertThat(listing.getRejectionReason()).isNull();

        ArgumentCaptor<Object> eventCaptor = ArgumentCaptor.forClass(Object.class);
        verify(eventPublisher, times(2)).publishEvent(eventCaptor.capture());
        assertThat(eventCaptor.getAllValues())
                .containsExactly(
                        new ListingModerationDecisionEvent(
                                ListingType.ROOMMATE,
                                1L,
                                7L,
                                "Room near the park",
                                ListingStatus.REJECTED,
                                "Add more details"
                        ),
                        new ListingModerationDecisionEvent(
                                ListingType.ROOMMATE,
                                1L,
                                7L,
                                "Room near the park",
                                ListingStatus.ACTIVE,
                                null
                        )
                );
    }

    @Test
    void setStatusDoesNotPublishForUnchangedOrNonDecisionStatus() throws Exception {
        RoommateListing listing = ownedListing(1L, 7L, ListingStatus.ACTIVE, 0);
        when(listingRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(listing));
        when(listingRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.setStatus(1L, ListingStatus.ACTIVE, null);
        service.setStatus(1L, ListingStatus.INACTIVE, null);

        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    void rejectRequiresReasonWithoutPublishing() {
        assertThatThrownBy(() -> service.setStatus(1L, ListingStatus.REJECTED, "  "))
                .isInstanceOf(GenericException.class)
                .hasFieldOrPropertyWithValue("code", GenericExceptionCode.MODERATION_REASON_REQUIRED);

        verify(listingRepository, never()).save(any());
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    void resubmitClearsRejectionReason() throws Exception {
        RoommateListing listing = ownedListing(1L, 7L, ListingStatus.REJECTED, 1);
        listing.setRejectionReason("Needs work");
        when(listingRepository.findById(1L)).thenReturn(Optional.of(listing));
        when(listingRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        RoommateListing saved = service.resubmit(1L, 7L);

        assertThat(saved.getStatus()).isEqualTo(ListingStatus.PENDING);
        assertThat(saved.getRejectionReason()).isNull();
    }

    @Test
    void ownerDeleteOnlyWhenExpired() throws Exception {
        RoommateListing listing = ownedListing(1L, 7L, ListingStatus.EXPIRED, 0);
        when(listingRepository.findById(1L)).thenReturn(Optional.of(listing));

        service.deleteByOwner(1L, 7L);

        verify(promotionService).cancelDeferredForListing(any(), eq(1L));
        verify(listingRepository).delete(listing);
    }

    private RoommateListing ownedListing(long id, long ownerId, ListingStatus status, int editCount) {
        RoommateListing listing = new RoommateListing();
        listing.setId(id);
        listing.setOwnerId(ownerId);
        listing.setStatus(status);
        listing.setOwnerEditCount(editCount);
        return listing;
    }

    private RoommateListing createAndCapture(boolean hasProperty, Integer peopleInProperty) throws Exception {
        service.create(7L, request(hasProperty, peopleInProperty));

        ArgumentCaptor<RoommateListing> captor = ArgumentCaptor.forClass(RoommateListing.class);
        verify(listingRepository).save(captor.capture());
        return captor.getValue();
    }

    private RoommateListingRequest request(boolean hasProperty, Integer peopleInProperty) {
        RoommateListingRequest request = new RoommateListingRequest();
        request.setHasProperty(hasProperty);
        request.setPeopleInProperty(peopleInProperty);
        return request;
    }
}
