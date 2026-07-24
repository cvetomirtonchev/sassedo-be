package server.sassedo.listing.common.notification;

import jakarta.mail.MessagingException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import server.sassedo.common.service.email.BrandedEmailSender;
import server.sassedo.listing.common.ListingStatus;
import server.sassedo.promotion.common.ListingType;

import java.io.UnsupportedEncodingException;
import java.util.Map;

@Service
public class ListingModerationEmailService {

    private static final String APPROVED_SUBJECT = "Обявата ви е одобрена";
    private static final String REJECTED_SUBJECT = "Обявата ви не беше одобрена";

    private final BrandedEmailSender emailSender;
    private final String clientUrl;

    public ListingModerationEmailService(
            BrandedEmailSender emailSender,
            @Value("${sassedo.app.client-url}") String clientUrl
    ) {
        this.emailSender = emailSender;
        this.clientUrl = clientUrl.replaceFirst("/+$", "");
    }

    public void sendDecision(
            String email,
            String name,
            ListingType listingType,
            Long listingId,
            String listingTitle,
            ListingStatus decision,
            String rejectionReason
    ) throws MessagingException, UnsupportedEncodingException {
        if (decision == ListingStatus.ACTIVE) {
            sendApproved(email, name, listingType, listingId, listingTitle);
            return;
        }
        if (decision == ListingStatus.REJECTED) {
            sendRejected(email, name, listingType, listingId, listingTitle, rejectionReason);
            return;
        }
        throw new IllegalArgumentException("Unsupported moderation decision: " + decision);
    }

    private void sendApproved(
            String email,
            String name,
            ListingType listingType,
            Long listingId,
            String listingTitle
    ) throws MessagingException, UnsupportedEncodingException {
        String listingUrl = clientUrl + publicPath(listingType, listingId);
        String plainTextContent = """
                Здравейте, %s,

                Обявата ви „%s“ беше одобрена и вече е активна.

                Разгледайте обявата: %s

                Поздрави,
                Екипът на Sassedo
                """.formatted(name, listingTitle, listingUrl);

        emailSender.send(
                email,
                APPROVED_SUBJECT,
                plainTextContent,
                "email/listing-approved",
                Map.of(
                        "name", name,
                        "listingTitle", listingTitle,
                        "listingUrl", listingUrl
                )
        );
    }

    private void sendRejected(
            String email,
            String name,
            ListingType listingType,
            Long listingId,
            String listingTitle,
            String rejectionReason
    ) throws MessagingException, UnsupportedEncodingException {
        String editUrl = clientUrl + editPath(listingType, listingId);
        String plainTextContent = """
                Здравейте, %s,

                Обявата ви „%s“ не беше одобрена.

                Причина: %s

                Редактирайте обявата и я изпратете отново за преглед: %s

                Поздрави,
                Екипът на Sassedo
                """.formatted(name, listingTitle, rejectionReason, editUrl);

        emailSender.send(
                email,
                REJECTED_SUBJECT,
                plainTextContent,
                "email/listing-rejected",
                Map.of(
                        "name", name,
                        "listingTitle", listingTitle,
                        "rejectionReason", rejectionReason,
                        "editUrl", editUrl
                )
        );
    }

    private static String publicPath(ListingType listingType, Long listingId) {
        return switch (listingType) {
            case RENTAL -> "/listing/" + listingId;
            case ROOMMATE -> "/roommate/" + listingId;
        };
    }

    private static String editPath(ListingType listingType, Long listingId) {
        return switch (listingType) {
            case RENTAL -> "/dashboard/listings/rental/" + listingId + "/edit";
            case ROOMMATE -> "/dashboard/listings/roommate/" + listingId + "/edit";
        };
    }
}
