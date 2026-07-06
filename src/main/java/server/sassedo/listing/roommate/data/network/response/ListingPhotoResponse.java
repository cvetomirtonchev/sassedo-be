package server.sassedo.listing.roommate.data.network.response;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ListingPhotoResponse {
    private Long id;
    private String url;
    private boolean main;

    public ListingPhotoResponse(Long id, String url, boolean main) {
        this.id = id;
        this.url = url;
        this.main = main;
    }
}
