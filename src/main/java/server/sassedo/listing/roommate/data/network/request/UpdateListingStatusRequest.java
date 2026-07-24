package server.sassedo.listing.roommate.data.network.request;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import server.sassedo.listing.common.ListingStatus;

@Getter
@Setter
public class UpdateListingStatusRequest {

    @NotNull
    private ListingStatus status;

    /** Required when {@code status} is {@link ListingStatus#REJECTED}. Cleared on approval. */
    private String rejectionReason;
}
