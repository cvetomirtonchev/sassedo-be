package server.sassedo.engagement.data.network.response;

import lombok.Getter;
import lombok.Setter;
import server.sassedo.listing.rental.data.network.response.RentalListingResponse;
import server.sassedo.listing.roommate.data.network.response.RoommateListingResponse;

import java.util.List;

@Getter
@Setter
public class MyFavoritesResponse {
    private List<RentalListingResponse> rentals;
    private List<RoommateListingResponse> roommates;

    public MyFavoritesResponse(List<RentalListingResponse> rentals,
                               List<RoommateListingResponse> roommates) {
        this.rentals = rentals;
        this.roommates = roommates;
    }
}
