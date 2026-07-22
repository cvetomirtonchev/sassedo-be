package server.sassedo.listing.common.matching;

import lombok.Getter;
import lombok.Setter;
import server.sassedo.listing.common.NearbyAmenity;
import server.sassedo.listing.common.RoomAmenity;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * How well a listing's property details match the requesting user's saved preferences (the
 * "preferences vs property" matching family). Each field is a full-match flag: only exact matches
 * are reported ({@link MatchState#EXACT}); everything else stays {@link MatchState#NONE}. The
 * matched amenity sets contain only the preferred amenities the listing actually offers, so the UI
 * can mark just those badges. Only populated for authenticated requests.
 */
@Getter
@Setter
public class PreferenceMatchResponse {

    private MatchState budget = MatchState.NONE;
    private MatchState propertyType = MatchState.NONE;
    private MatchState furnished = MatchState.NONE;
    private MatchState petsAllowed = MatchState.NONE;
    private MatchState bedrooms = MatchState.NONE;
    private MatchState bathrooms = MatchState.NONE;
    private MatchState location = MatchState.NONE;

    private Set<RoomAmenity> matchedRoomAmenities = new LinkedHashSet<>();
    private Set<NearbyAmenity> matchedNearbyAmenities = new LinkedHashSet<>();
}
