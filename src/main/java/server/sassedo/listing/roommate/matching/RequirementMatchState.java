package server.sassedo.listing.roommate.matching;

/**
 * Per-requirement match outcome exposed to the frontend so it can highlight how well the viewer's
 * profile satisfies each roommate requirement.
 *
 * <ul>
 *     <li>{@link #EXACT} - the viewer fully satisfies the requirement (dimension score of 1.0).</li>
 *     <li>{@link #PARTIAL} - the viewer partially satisfies it (soft match, 0 &lt; score &lt; 1).</li>
 *     <li>{@link #NONE} - neutral: mismatch, missing viewer data, or a non-scoring requirement.</li>
 * </ul>
 */
public enum RequirementMatchState {
    EXACT,
    PARTIAL,
    NONE
}
