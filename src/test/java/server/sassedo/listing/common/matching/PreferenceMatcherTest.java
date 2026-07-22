package server.sassedo.listing.common.matching;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import server.sassedo.listing.common.NearbyAmenity;
import server.sassedo.listing.common.PropertyType;
import server.sassedo.listing.common.RoomAmenity;
import server.sassedo.location.data.dto.City;
import server.sassedo.location.data.dto.Country;
import server.sassedo.user.data.dto.UserPreferencesDto;

import java.math.BigDecimal;
import java.util.LinkedHashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class PreferenceMatcherTest {

    private static City city(long id, Country country) {
        City c = new City("City" + id, "Град" + id, country);
        ReflectionTestUtils.setField(c, "id", id);
        return c;
    }

    private static Country country(long id) {
        Country c = new Country("Country" + id, "Държава" + id, "C" + id);
        ReflectionTestUtils.setField(c, "id", id);
        return c;
    }

    @Test
    void nullPreferencesYieldNeutralResult() {
        PreferenceMatchResponse result = PreferenceMatcher.evaluate(null,
                BigDecimal.valueOf(500), PropertyType.APARTMENT, true, true, 3, 2,
                city(1, country(1)), country(1),
                Set.of(RoomAmenity.BALCONY), Set.of(NearbyAmenity.PUBLIC_TRANSPORT));

        assertThat(result.getBudget()).isEqualTo(MatchState.NONE);
        assertThat(result.getPropertyType()).isEqualTo(MatchState.NONE);
        assertThat(result.getMatchedRoomAmenities()).isEmpty();
        assertThat(result.getMatchedNearbyAmenities()).isEmpty();
    }

    @Test
    void budgetIsExactWhenRentWithinBudgetAndNoneWhenOver() {
        UserPreferencesDto prefs = new UserPreferencesDto();
        prefs.setPreferredMaxBudget(BigDecimal.valueOf(600));

        assertThat(evaluateRent(prefs, BigDecimal.valueOf(500)).getBudget())
                .isEqualTo(MatchState.EXACT);
        assertThat(evaluateRent(prefs, BigDecimal.valueOf(600)).getBudget())
                .isEqualTo(MatchState.EXACT);
        // Over budget is a soft (falloff) match, which we do not highlight.
        assertThat(evaluateRent(prefs, BigDecimal.valueOf(650)).getBudget())
                .isEqualTo(MatchState.NONE);
    }

    @Test
    void propertyTypeExactOnlyOnEnumMatch() {
        UserPreferencesDto prefs = new UserPreferencesDto();
        prefs.setPreferredPropertyType(PropertyType.APARTMENT);

        assertThat(evaluateType(prefs, PropertyType.APARTMENT).getPropertyType())
                .isEqualTo(MatchState.EXACT);
        assertThat(evaluateType(prefs, PropertyType.HOUSE).getPropertyType())
                .isEqualTo(MatchState.NONE);
    }

    @Test
    void furnishedAndPetsOnlyMatchWhenPreferredAndAvailable() {
        UserPreferencesDto prefs = new UserPreferencesDto();
        prefs.setPreferredFurnished(true);
        prefs.setPreferredPetsAllowed(true);

        PreferenceMatchResponse matched = PreferenceMatcher.evaluate(prefs,
                null, null, true, true, null, null, null, null, null, null);
        assertThat(matched.getFurnished()).isEqualTo(MatchState.EXACT);
        assertThat(matched.getPetsAllowed()).isEqualTo(MatchState.EXACT);

        PreferenceMatchResponse notAvailable = PreferenceMatcher.evaluate(prefs,
                null, null, false, false, null, null, null, null, null, null);
        assertThat(notAvailable.getFurnished()).isEqualTo(MatchState.NONE);
        assertThat(notAvailable.getPetsAllowed()).isEqualTo(MatchState.NONE);
    }

    @Test
    void bedroomsAndBathroomsMatchWhenAtOrAboveMinimum() {
        UserPreferencesDto prefs = new UserPreferencesDto();
        prefs.setPreferredMinBedrooms(2);
        prefs.setPreferredMinBathrooms(2);

        PreferenceMatchResponse enough = PreferenceMatcher.evaluate(prefs,
                null, null, null, null, 3, 2, null, null, null, null);
        assertThat(enough.getBedrooms()).isEqualTo(MatchState.EXACT);
        assertThat(enough.getBathrooms()).isEqualTo(MatchState.EXACT);

        PreferenceMatchResponse tooFew = PreferenceMatcher.evaluate(prefs,
                null, null, null, null, 1, 1, null, null, null, null);
        assertThat(tooFew.getBedrooms()).isEqualTo(MatchState.NONE);
        assertThat(tooFew.getBathrooms()).isEqualTo(MatchState.NONE);
    }

    @Test
    void locationIsExactForSameCityButNotForCountryOnly() {
        Country bg = country(10);
        City sofia = city(11, bg);
        City plovdiv = city(12, bg);

        UserPreferencesDto prefs = new UserPreferencesDto();
        prefs.setPreferredCity(sofia);
        prefs.setPreferredCountry(bg);

        PreferenceMatchResponse sameCity = PreferenceMatcher.evaluate(prefs,
                null, null, null, null, null, null, sofia, bg, null, null);
        assertThat(sameCity.getLocation()).isEqualTo(MatchState.EXACT);

        // Same country, different city is only a partial (0.5) match, which we do not highlight.
        PreferenceMatchResponse sameCountry = PreferenceMatcher.evaluate(prefs,
                null, null, null, null, null, null, plovdiv, bg, null, null);
        assertThat(sameCountry.getLocation()).isEqualTo(MatchState.NONE);
    }

    @Test
    void amenitiesReportOnlyThePreferredOnesTheListingOffers() {
        UserPreferencesDto prefs = new UserPreferencesDto();
        prefs.setPreferredRoomAmenities(new LinkedHashSet<>(
                Set.of(RoomAmenity.BALCONY, RoomAmenity.FURNISHED)));
        prefs.setPreferredNearbyAmenities(new LinkedHashSet<>(
                Set.of(NearbyAmenity.PUBLIC_TRANSPORT)));

        PreferenceMatchResponse result = PreferenceMatcher.evaluate(prefs,
                null, null, null, null, null, null, null, null,
                Set.of(RoomAmenity.BALCONY),
                Set.of(NearbyAmenity.PUBLIC_TRANSPORT, NearbyAmenity.AIRPORT));

        assertThat(result.getMatchedRoomAmenities()).containsExactly(RoomAmenity.BALCONY);
        assertThat(result.getMatchedNearbyAmenities()).containsExactly(NearbyAmenity.PUBLIC_TRANSPORT);
    }

    private PreferenceMatchResponse evaluateRent(UserPreferencesDto prefs, BigDecimal rent) {
        return PreferenceMatcher.evaluate(prefs, rent, null, null, null, null, null,
                null, null, null, null);
    }

    private PreferenceMatchResponse evaluateType(UserPreferencesDto prefs, PropertyType type) {
        return PreferenceMatcher.evaluate(prefs, null, type, null, null, null, null,
                null, null, null, null);
    }
}
