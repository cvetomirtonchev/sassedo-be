package server.sassedo.listing.rental.data.projection;

/**
 * Lightweight projection of a rental listing photo that excludes the {@code data} blob.
 * Used by list mappings so building photo URLs never loads the (MEDIUMBLOB) image bytes.
 */
public interface RentalPhotoMeta {
    Long getId();

    boolean isMain();
}
