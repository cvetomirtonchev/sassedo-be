package server.sassedo.listing.common;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ListingContactResponse {

    private String name;
    private String phone;

    public ListingContactResponse(String name, String phone) {
        this.name = name;
        this.phone = phone;
    }
}
