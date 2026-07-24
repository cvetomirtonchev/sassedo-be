package server.sassedo.listing.common;

import server.sassedo.model.GenericException;
import server.sassedo.model.GenericExceptionCode;

/**
 * Lifetime cap on successful owner content edits ({@code PUT /{id}}) for roommate and rental listings.
 */
public final class ListingOwnerEditPolicy {

    public static final int MAX_OWNER_EDITS = 5;

    private ListingOwnerEditPolicy() {
    }

    public static int remainingEdits(int ownerEditCount) {
        return Math.max(0, MAX_OWNER_EDITS - ownerEditCount);
    }

    public static void assertCanOwnerEdit(int ownerEditCount) throws GenericException {
        if (ownerEditCount >= MAX_OWNER_EDITS) {
            throw new GenericException(GenericExceptionCode.LISTING_EDIT_LIMIT_REACHED,
                    "You have reached the maximum number of edits for this listing");
        }
    }

    /** Status after a successful owner update; rejected and expired listings stay in place. */
    public static ListingStatus statusAfterOwnerEdit(ListingStatus current) {
        if (current == ListingStatus.REJECTED || current == ListingStatus.EXPIRED) {
            return current;
        }
        return ListingStatus.PENDING;
    }
}
