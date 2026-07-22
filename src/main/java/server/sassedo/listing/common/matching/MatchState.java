package server.sassedo.listing.common.matching;

/**
 * Generic per-dimension match outcome shared across listing types. Serialized to the frontend so it
 * can highlight matching values.
 *
 * <ul>
 *     <li>{@link #EXACT} - the viewer fully satisfies the dimension.</li>
 *     <li>{@link #PARTIAL} - a soft/partial match.</li>
 *     <li>{@link #NONE} - neutral: mismatch, missing data, or not applicable.</li>
 * </ul>
 */
public enum MatchState {
    EXACT,
    PARTIAL,
    NONE
}
