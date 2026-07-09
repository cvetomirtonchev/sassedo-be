package server.sassedo.engagement.service;

/**
 * Implemented by listing response DTOs so their engagement counters can be populated uniformly
 * by {@link EngagementEnricher}, regardless of listing type.
 */
public interface EngagementAware {

    Long getId();

    void setFavoriteCount(long favoriteCount);

    void setViewCount(long viewCount);

    void setFavoritedByMe(boolean favoritedByMe);
}
