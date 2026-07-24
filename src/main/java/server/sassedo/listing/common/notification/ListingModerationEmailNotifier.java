package server.sassedo.listing.common.notification;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import server.sassedo.user.data.projection.UserEmailRecipient;
import server.sassedo.user.repository.UserRepository;

import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class ListingModerationEmailNotifier {

    private final UserRepository userRepository;
    private final ListingModerationEmailService emailService;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onDecision(ListingModerationDecisionEvent event) {
        try {
            Optional<UserEmailRecipient> recipient =
                    userRepository.findEmailRecipientById(event.ownerId());
            if (recipient.isEmpty()) {
                log.warn("Skipping listing moderation email because owner {} was not found", event.ownerId());
                return;
            }

            UserEmailRecipient owner = recipient.get();
            emailService.sendDecision(
                    owner.getEmail(),
                    owner.getName(),
                    event.listingType(),
                    event.listingId(),
                    event.listingTitle(),
                    event.decision(),
                    event.rejectionReason()
            );
        } catch (Exception exception) {
            log.error(
                    "Failed to send {} moderation email for {} listing {} to owner {}",
                    event.decision(),
                    event.listingType(),
                    event.listingId(),
                    event.ownerId(),
                    exception
            );
        }
    }
}
