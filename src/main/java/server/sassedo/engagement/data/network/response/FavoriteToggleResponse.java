package server.sassedo.engagement.data.network.response;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class FavoriteToggleResponse {
    private boolean favorited;
    private long favoriteCount;

    public FavoriteToggleResponse(boolean favorited, long favoriteCount) {
        this.favorited = favorited;
        this.favoriteCount = favoriteCount;
    }
}
