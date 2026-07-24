package server.sassedo.listing.common.notification;

import jakarta.mail.MessagingException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import server.sassedo.listing.common.ListingStatus;
import server.sassedo.promotion.common.ListingType;
import server.sassedo.user.data.projection.UserEmailRecipient;
import server.sassedo.user.repository.UserRepository;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ListingModerationEmailNotifierTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private ListingModerationEmailService emailService;
    @Mock
    private UserEmailRecipient recipient;

    @Test
    void onDecision_sendsEmailToResolvedOwner() throws Exception {
        ListingModerationDecisionEvent event = event(ListingStatus.REJECTED, "Missing photos");
        when(userRepository.findEmailRecipientById(9L)).thenReturn(Optional.of(recipient));
        when(recipient.getEmail()).thenReturn("owner@example.com");
        when(recipient.getName()).thenReturn("Мария");

        notifier().onDecision(event);

        verify(emailService).sendDecision(
                "owner@example.com",
                "Мария",
                ListingType.RENTAL,
                12L,
                "Sunny apartment",
                ListingStatus.REJECTED,
                "Missing photos"
        );
    }

    @Test
    void onDecision_skipsDeletedOrMissingOwner() {
        when(userRepository.findEmailRecipientById(9L)).thenReturn(Optional.empty());

        notifier().onDecision(event(ListingStatus.ACTIVE, null));

        verifyNoInteractions(emailService);
    }

    @Test
    void onDecision_doesNotPropagateMailFailure() throws Exception {
        ListingModerationDecisionEvent event = event(ListingStatus.ACTIVE, null);
        when(userRepository.findEmailRecipientById(9L)).thenReturn(Optional.of(recipient));
        when(recipient.getEmail()).thenReturn("owner@example.com");
        when(recipient.getName()).thenReturn("Мария");
        org.mockito.Mockito.doThrow(new MessagingException("SMTP unavailable"))
                .when(emailService)
                .sendDecision(
                        "owner@example.com",
                        "Мария",
                        ListingType.RENTAL,
                        12L,
                        "Sunny apartment",
                        ListingStatus.ACTIVE,
                        null
                );

        assertThatNoException().isThrownBy(() -> notifier().onDecision(event));

        verify(emailService).sendDecision(
                "owner@example.com",
                "Мария",
                ListingType.RENTAL,
                12L,
                "Sunny apartment",
                ListingStatus.ACTIVE,
                null
        );
        verify(userRepository, never()).findById(9L);
    }

    private ListingModerationEmailNotifier notifier() {
        return new ListingModerationEmailNotifier(userRepository, emailService);
    }

    private static ListingModerationDecisionEvent event(
            ListingStatus decision,
            String rejectionReason
    ) {
        return new ListingModerationDecisionEvent(
                ListingType.RENTAL,
                12L,
                9L,
                "Sunny apartment",
                decision,
                rejectionReason
        );
    }
}
