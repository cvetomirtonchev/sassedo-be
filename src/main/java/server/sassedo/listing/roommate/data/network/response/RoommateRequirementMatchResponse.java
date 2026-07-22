package server.sassedo.listing.roommate.data.network.response;

import lombok.Getter;
import lombok.Setter;
import server.sassedo.listing.roommate.matching.RequirementMatchState;
import server.sassedo.user.data.dto.Language;

import java.util.Set;

/**
 * Per-requirement match states for the requesting user against a roommate listing's requirements.
 * Only populated for authenticated requests; {@code null} otherwise.
 */
@Getter
@Setter
public class RoommateRequirementMatchResponse {

    private RequirementMatchState sex;
    private RequirementMatchState age;
    private RequirementMatchState smoking;
    private RequirementMatchState pets;
    private RequirementMatchState employment;
    private RequirementMatchState languages;

    // The specific required languages the viewer also speaks, so the UI can mark only those.
    private Set<Language> matchedLanguages;
}
