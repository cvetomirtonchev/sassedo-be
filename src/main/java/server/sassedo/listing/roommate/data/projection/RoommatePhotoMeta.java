package server.sassedo.listing.roommate.data.projection;

/**
 * Lightweight projection of a roommate listing photo that excludes the {@code data} blob.
 * Used by list mappings so building photo URLs never loads the (MEDIUMBLOB) image bytes.
 */
public interface RoommatePhotoMeta {
    Long getId();

    boolean isMain();
}
