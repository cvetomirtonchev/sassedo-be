package server.sassedo.engagement.data.network.response;

import lombok.Getter;
import lombok.Setter;
import server.sassedo.listing.rental.data.network.response.RentalListingResponse;
import server.sassedo.listing.roommate.data.network.response.RoommateListingResponse;
import server.sassedo.listing.search.data.network.response.ApartmentSearchResponse;

import java.util.List;

@Getter
@Setter
public class MyFavoritesResponse {
    private List<RentalListingResponse> rentals;
    private List<RoommateListingResponse> roommates;
    private List<ApartmentSearchResponse> searches;

    public MyFavoritesResponse(List<RentalListingResponse> rentals,
                               List<RoommateListingResponse> roommates,
                               List<ApartmentSearchResponse> searches) {
        this.rentals = rentals;
        this.roommates = roommates;
        this.searches = searches;
    }
}
