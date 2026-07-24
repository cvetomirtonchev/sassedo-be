package server.sassedo.listing.roommate.matching;

import org.junit.jupiter.api.Test;
import server.sassedo.listing.common.PetPolicy;
import server.sassedo.listing.common.RoommateSexPreference;
import server.sassedo.listing.common.SmokerPreference;
import server.sassedo.listing.roommate.data.dto.RoommateListing;
import server.sassedo.user.data.dto.Language;
import server.sassedo.user.data.dto.Occupation;
import server.sassedo.user.data.dto.Sex;
import server.sassedo.user.data.dto.User;

import java.util.LinkedHashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class RoommateMatchScorerTest {

    private final RoommateMatchScorer scorer = new RoommateMatchScorer();

    private User user() {
        return new User();
    }

    private RoommateListing listing() {
        return new RoommateListing();
    }

    @Test
    void nullUserOrListingYieldsNoScoreAndNeutralStates() {
        RoommateMatchResult result = scorer.evaluate(null, listing());

        assertThat(result.getScore()).isNull();
        assertThat(result.getSex()).isEqualTo(RequirementMatchState.NONE);
        assertThat(result.getAge()).isEqualTo(RequirementMatchState.NONE);
        assertThat(result.getLanguages()).isEqualTo(RequirementMatchState.NONE);
    }

    @Test
    void noConstraintsAndNoPreferencesScoresHundred() {
        RoommateMatchResult result = scorer.evaluate(user(), listing());

        assertThat(result.getScore()).isEqualTo(100);
        assertThat(result.getSex()).isEqualTo(RequirementMatchState.NONE);
    }

    @Test
    void constraintsWithNoUserDataYieldsNullScoreAndNeutralStates() {
        RoommateListing listing = listing();
        listing.setPreferredSex(RoommateSexPreference.FEMALE);
        listing.setAgeMin(20);
        listing.setAgeMax(30);

        RoommateMatchResult result = scorer.evaluate(user(), listing);

        assertThat(result.getScore()).isNull();
        assertThat(result.getSex()).isEqualTo(RequirementMatchState.NONE);
        assertThat(result.getAge()).isEqualTo(RequirementMatchState.NONE);
    }

    @Test
    void exactSexMatchIsExactAndMismatchIsNeutral() {
        RoommateListing exact = listing();
        exact.setPreferredSex(RoommateSexPreference.FEMALE);
        User female = user();
        female.setSex(Sex.FEMALE);
        assertThat(scorer.evaluate(female, exact).getSex()).isEqualTo(RequirementMatchState.EXACT);

        RoommateListing mismatchListing = listing();
        mismatchListing.setPreferredSex(RoommateSexPreference.FEMALE);
        User male = user();
        male.setSex(Sex.MALE);
        RoommateMatchResult mismatch = scorer.evaluate(male, mismatchListing);
        assertThat(mismatch.getSex()).isEqualTo(RequirementMatchState.NONE);
        assertThat(mismatch.getScore()).isEqualTo(0);
    }

    @Test
    void noSexPreferenceIsExactForEveryProfileSexWithoutChangingTheScore() {
        for (Sex sex : Sex.values()) {
            RoommateListing listing = listing();
            listing.setPreferredSex(RoommateSexPreference.NO_PREFERENCE);
            listing.setEmploymentStatus(Occupation.WORKING);

            User user = user();
            user.setSex(sex);
            user.setOccupation(Occupation.UNEMPLOYED);

            RoommateMatchResult result = scorer.evaluate(user, listing);

            assertThat(result.getSex()).isEqualTo(RequirementMatchState.EXACT);
            assertThat(result.getEmployment()).isEqualTo(RequirementMatchState.NONE);
            assertThat(result.getScore()).isEqualTo(0);
        }
    }

    @Test
    void noSexPreferenceAloneScoresHundredAndRequiresProfileSexForTheExactState() {
        RoommateListing listing = listing();
        listing.setPreferredSex(RoommateSexPreference.NO_PREFERENCE);

        User userWithSex = user();
        userWithSex.setSex(Sex.OTHER);
        RoommateMatchResult withSex = scorer.evaluate(userWithSex, listing);
        assertThat(withSex.getSex()).isEqualTo(RequirementMatchState.EXACT);
        assertThat(withSex.getScore()).isEqualTo(100);

        RoommateMatchResult withoutSex = scorer.evaluate(user(), listing);
        assertThat(withoutSex.getSex()).isEqualTo(RequirementMatchState.NONE);
        assertThat(withoutSex.getScore()).isEqualTo(100);
    }

    @Test
    void ageInsideRangeIsExactAndNearRangeIsPartial() {
        RoommateListing listing = listing();
        listing.setAgeMin(20);
        listing.setAgeMax(30);

        User inside = user();
        inside.setAge(25);
        assertThat(scorer.evaluate(inside, listing).getAge()).isEqualTo(RequirementMatchState.EXACT);

        // One year outside the range: falloff gives 0 < score < 1 -> PARTIAL.
        User near = user();
        near.setAge(32);
        assertThat(scorer.evaluate(near, listing).getAge()).isEqualTo(RequirementMatchState.PARTIAL);

        // Far outside the falloff window -> neutral.
        User far = user();
        far.setAge(40);
        assertThat(scorer.evaluate(far, listing).getAge()).isEqualTo(RequirementMatchState.NONE);
    }

    @Test
    void noPreferenceSmokingIsNotScoredAndStaysNeutral() {
        RoommateListing listing = listing();
        listing.setSmokingPreference(SmokerPreference.NO_PREFERENCE);
        User user = user();
        user.setSmokingPreference(SmokerPreference.SMOKER);

        RoommateMatchResult result = scorer.evaluate(user, listing);

        // The only field is a NO_PREFERENCE requirement, which is not a constraint at all.
        assertThat(result.getSmoking()).isEqualTo(RequirementMatchState.NONE);
        assertThat(result.getScore()).isEqualTo(100);
    }

    @Test
    void softSmokingMatchIsPartial() {
        RoommateListing listing = listing();
        listing.setSmokingPreference(SmokerPreference.NON_SMOKER);
        User user = user();
        user.setSmokingPreference(SmokerPreference.NO_PREFERENCE);

        assertThat(scorer.evaluate(user, listing).getSmoking())
                .isEqualTo(RequirementMatchState.PARTIAL);
    }

    @Test
    void employmentIsStrictExactOrNone() {
        RoommateListing listing = listing();
        listing.setEmploymentStatus(Occupation.WORKING);

        // Self-employed vs employed is no longer a soft/partial match.
        User selfEmployed = user();
        selfEmployed.setOccupation(Occupation.SELF_EMPLOYED);
        assertThat(scorer.evaluate(selfEmployed, listing).getEmployment())
                .isEqualTo(RequirementMatchState.NONE);

        User employed = user();
        employed.setOccupation(Occupation.WORKING);
        assertThat(scorer.evaluate(employed, listing).getEmployment())
                .isEqualTo(RequirementMatchState.EXACT);
    }

    @Test
    void unemployedDoesNotPartiallyMatchEmployed() {
        RoommateListing listing = listing();
        listing.setEmploymentStatus(Occupation.WORKING);

        User unemployed = user();
        unemployed.setOccupation(Occupation.UNEMPLOYED);

        RoommateMatchResult result = scorer.evaluate(unemployed, listing);
        assertThat(result.getEmployment()).isEqualTo(RequirementMatchState.NONE);
        assertThat(result.getScore()).isEqualTo(0);
    }

    @Test
    void anyLanguageOverlapIsAFullMatchAndReportsOnlyMatchedLanguages() {
        RoommateListing listing = listing();
        listing.setSpokenLanguages(languages(Language.ENGLISH, Language.GERMAN));

        // A single overlapping language now counts as a full (EXACT) match.
        User partial = user();
        partial.setLanguages(languages(Language.ENGLISH, Language.FRENCH));
        RoommateMatchResult partialResult = scorer.evaluate(partial, listing);
        assertThat(partialResult.getLanguages()).isEqualTo(RequirementMatchState.EXACT);
        assertThat(partialResult.getMatchedLanguages()).containsExactly(Language.ENGLISH);

        // No overlap stays neutral with no matched languages.
        User none = user();
        none.setLanguages(languages(Language.FRENCH));
        RoommateMatchResult noneResult = scorer.evaluate(none, listing);
        assertThat(noneResult.getLanguages()).isEqualTo(RequirementMatchState.NONE);
        assertThat(noneResult.getMatchedLanguages()).isEmpty();
    }

    @Test
    void aggregateScoreIsUnchangedForACombinedProfile() {
        RoommateListing listing = listing();
        listing.setPreferredSex(RoommateSexPreference.FEMALE);
        listing.setAgeMin(20);
        listing.setAgeMax(30);
        listing.setEmploymentStatus(Occupation.REMOTE_WORKER);

        User user = user();
        user.setSex(Sex.FEMALE);
        user.setAge(25);
        user.setOccupation(Occupation.REMOTE_WORKER);

        RoommateMatchResult result = scorer.evaluate(user, listing);

        // All three applicable dimensions are exact matches.
        assertThat(result.getScore()).isEqualTo(100);
        assertThat(result.getSex()).isEqualTo(RequirementMatchState.EXACT);
        assertThat(result.getAge()).isEqualTo(RequirementMatchState.EXACT);
        assertThat(result.getEmployment()).isEqualTo(RequirementMatchState.EXACT);

        // The score() convenience method delegates to evaluate().
        assertThat(scorer.score(user, listing)).isEqualTo(result.getScore());
    }

    @Test
    void petAllAllowedRequirementIsAlwaysExact() {
        RoommateListing listing = listing();
        listing.setPetPolicy(PetPolicy.ALL_ALLOWED);
        User user = user();
        user.setPetPolicy(PetPolicy.NOT_ALLOWED);

        assertThat(scorer.evaluate(user, listing).getPets()).isEqualTo(RequirementMatchState.EXACT);
    }

    private Set<Language> languages(Language... langs) {
        return new LinkedHashSet<>(Set.of(langs));
    }
}
