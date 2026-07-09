package server.sassedo.engagement.service;

import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import server.sassedo.engagement.data.dto.ListingView;
import server.sassedo.engagement.repository.ListingViewRepository;
import server.sassedo.promotion.common.ListingType;

@Service
@RequiredArgsConstructor
public class ListingViewService {

    private final ListingViewRepository listingViewRepository;

    /**
     * Records a unique view for a listing and returns the resulting view count.
     * <p>
     * Uniqueness is per viewer: authenticated viewers dedup by user id, anonymous viewers by the
     * client-supplied visitor id. Owner self-views are ignored, and repeat views by the same viewer
     * do not increment the count.
     * <p>
     * Not wrapped in a single transaction on purpose: {@code saveAndFlush} runs in its own
     * transaction, so a losing race on the unique constraint rolls back only that insert without
     * poisoning the follow-up count query.
     */
    public long recordView(ListingType listingType, Long listingId, Long userId, String visitorId, Long ownerId) {
        String viewerKey = resolveViewerKey(userId, visitorId);
        boolean ownerSelfView = userId != null && userId.equals(ownerId);

        if (viewerKey != null && !ownerSelfView
                && !listingViewRepository.existsByListingTypeAndListingIdAndViewerKey(listingType, listingId, viewerKey)) {
            ListingView view = new ListingView();
            view.setListingType(listingType);
            view.setListingId(listingId);
            view.setViewerKey(viewerKey);
            try {
                listingViewRepository.saveAndFlush(view);
            } catch (DataIntegrityViolationException ignored) {
                // Concurrent insert raced us to the unique constraint; the view is already counted.
            }
        }
        return count(listingType, listingId);
    }

    @Transactional(readOnly = true)
    public long count(ListingType listingType, Long listingId) {
        return listingViewRepository.countByListingTypeAndListingId(listingType, listingId);
    }

    private String resolveViewerKey(Long userId, String visitorId) {
        if (userId != null) {
            return "U:" + userId;
        }
        if (visitorId != null && !visitorId.isBlank()) {
            return "V:" + visitorId.trim();
        }
        return null;
    }
}
