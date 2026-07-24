package server.sassedo.user.data.projection;

/**
 * Lightweight recipient data for transactional emails, without loading profile-photo data.
 */
public interface UserEmailRecipient {

    String getEmail();

    String getName();
}
