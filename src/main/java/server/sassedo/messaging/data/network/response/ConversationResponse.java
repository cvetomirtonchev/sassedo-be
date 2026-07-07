package server.sassedo.messaging.data.network.response;

import lombok.Getter;
import lombok.Setter;
import server.sassedo.promotion.common.ListingType;

import java.time.LocalDateTime;

@Getter
@Setter
public class ConversationResponse {
    private Long id;
    private ListingType listingType;
    private Long listingId;
    private String title;

    private Long otherParticipantId;
    private String otherParticipantName;
    private String otherParticipantPhotoUrl;

    private String lastMessagePreview;
    private LocalDateTime lastMessageAt;
    private long unreadCount;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
