package server.sassedo.listing.common;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import server.sassedo.listing.rental.repository.RentalListingRepository;
import server.sassedo.listing.roommate.repository.RoommateListingRepository;
import server.sassedo.listing.search.repository.ApartmentSearchRepository;

import java.time.LocalDateTime;

/**
 * Expires active listings whose expiration date has passed, unless they still have a live
 * promotion (guarded by {@code promotedUntil}). Runs every {@code sassedo.listings.sweep-ms}
 * (default 1h). Also backfills a fresh expiration window for pre-existing active listings that
 * predate the expiration feature, so they don't hang around forever without an expiry date.
 */
@Component
@Log4j2
@RequiredArgsConstructor
public class ListingExpirationScheduler {

    private final RentalListingRepository rentalRepository;
    private final RoommateListingRepository roommateRepository;
    private final ApartmentSearchRepository searchRepository;

    @Value("${sassedo.listings.ttl-days:30}")
    private long listingTtlDays;

    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void backfillMissingExpiry() {
        LocalDateTime expiresAt = LocalDateTime.now().plusDays(listingTtlDays);
        int rental = rentalRepository.backfillMissingExpiry(expiresAt);
        int roommate = roommateRepository.backfillMissingExpiry(expiresAt);
        int search = searchRepository.backfillMissingExpiry(expiresAt);
        if (rental > 0 || roommate > 0 || search > 0) {
            log.info("Listing expiry backfill: rental={}, roommate={}, search={}", rental, roommate, search);
        }
    }

    @Scheduled(fixedDelayString = "${sassedo.listings.sweep-ms:3600000}",
            initialDelayString = "${sassedo.listings.sweep-initial-ms:45000}")
    @Transactional
    public void sweep() {
        LocalDateTime now = LocalDateTime.now();
        int rental = rentalRepository.expireOverdue(now);
        int roommate = roommateRepository.expireOverdue(now);
        int search = searchRepository.expireOverdue(now);
        if (rental > 0 || roommate > 0 || search > 0) {
            log.info("Listing expiration sweep: rental={}, roommate={}, search={}", rental, roommate, search);
        }
    }
}
