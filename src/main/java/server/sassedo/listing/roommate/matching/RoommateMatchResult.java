package server.sassedo.listing.roommate.matching;

import server.sassedo.user.data.dto.Language;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Outcome of scoring a user against a roommate listing: the aggregate 0-100 compatibility score
 * (or {@code null} when it could not be computed) together with a per-requirement match state for
 * each scored roommate requirement. Field states are always non-null and default to
 * {@link RequirementMatchState#NONE}.
 */
public class RoommateMatchResult {

    private final Integer score;
    private final RequirementMatchState sex;
    private final RequirementMatchState age;
    private final RequirementMatchState smoking;
    private final RequirementMatchState pets;
    private final RequirementMatchState employment;
    private final RequirementMatchState languages;
    private final Set<Language> matchedLanguages;

    private RoommateMatchResult(Builder builder) {
        this.score = builder.score;
        this.sex = builder.sex;
        this.age = builder.age;
        this.smoking = builder.smoking;
        this.pets = builder.pets;
        this.employment = builder.employment;
        this.languages = builder.languages;
        this.matchedLanguages = builder.matchedLanguages;
    }

    public Integer getScore() {
        return score;
    }

    public RequirementMatchState getSex() {
        return sex;
    }

    public RequirementMatchState getAge() {
        return age;
    }

    public RequirementMatchState getSmoking() {
        return smoking;
    }

    public RequirementMatchState getPets() {
        return pets;
    }

    public RequirementMatchState getEmployment() {
        return employment;
    }

    public RequirementMatchState getLanguages() {
        return languages;
    }

    public Set<Language> getMatchedLanguages() {
        return matchedLanguages;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Integer score;
        private RequirementMatchState sex = RequirementMatchState.NONE;
        private RequirementMatchState age = RequirementMatchState.NONE;
        private RequirementMatchState smoking = RequirementMatchState.NONE;
        private RequirementMatchState pets = RequirementMatchState.NONE;
        private RequirementMatchState employment = RequirementMatchState.NONE;
        private RequirementMatchState languages = RequirementMatchState.NONE;
        private Set<Language> matchedLanguages = new LinkedHashSet<>();

        public Builder score(Integer score) {
            this.score = score;
            return this;
        }

        public Builder sex(RequirementMatchState sex) {
            this.sex = sex;
            return this;
        }

        public Builder age(RequirementMatchState age) {
            this.age = age;
            return this;
        }

        public Builder smoking(RequirementMatchState smoking) {
            this.smoking = smoking;
            return this;
        }

        public Builder pets(RequirementMatchState pets) {
            this.pets = pets;
            return this;
        }

        public Builder employment(RequirementMatchState employment) {
            this.employment = employment;
            return this;
        }

        public Builder languages(RequirementMatchState languages) {
            this.languages = languages;
            return this;
        }

        public Builder matchedLanguages(Set<Language> matchedLanguages) {
            this.matchedLanguages = matchedLanguages;
            return this;
        }

        public RoommateMatchResult build() {
            return new RoommateMatchResult(this);
        }
    }
}
