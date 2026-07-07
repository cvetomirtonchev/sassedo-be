package server.sassedo.messaging.data.network.request;

import lombok.Getter;
import lombok.Setter;
import server.sassedo.promotion.common.ListingType;

@Getter
@Setter
public class StartConversationRequest {
    private ListingType listingType;
    private Long listingId;
}
