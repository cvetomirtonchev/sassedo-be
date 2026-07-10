package server.sassedo.user.data.projection;

/**
 * Spring Data projection exposing only the fields the messaging inbox needs about the other
 * participant, without loading the profile-photo blob.
 */
public interface UserParticipantSummary {

    Long getId();

    String getName();

    boolean getHasPhoto();
}
