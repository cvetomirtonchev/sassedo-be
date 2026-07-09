package server.sassedo.engagement.data.network.request;

import lombok.Getter;
import lombok.Setter;
import server.sassedo.promotion.common.ListingType;

@Getter
@Setter
public class FavoriteRequest {
    private ListingType listingType;
    private Long listingId;
}
