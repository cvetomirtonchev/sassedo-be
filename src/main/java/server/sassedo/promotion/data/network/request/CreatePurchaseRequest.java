package server.sassedo.promotion.data.network.request;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import server.sassedo.promotion.common.ListingType;

@Getter
@Setter
public class CreatePurchaseRequest {

    @NotNull
    private Long packageId;

    @NotNull
    private ListingType listingType;

    @NotNull
    private Long listingId;
}
