package server.sassedo.promotion.data.network.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import server.sassedo.promotion.common.ListingType;
import server.sassedo.promotion.common.PromotionType;

@Getter
@Setter
public class GrantPromotionRequest {

    @NotNull
    private ListingType listingType;

    @NotNull
    private Long listingId;

    @NotNull
    private PromotionType type;

    @Min(1)
    private int durationDays;
}
