package server.sassedo.listing.common.notification;

import jakarta.mail.Message;
import jakarta.mail.Multipart;
import jakarta.mail.Part;
import jakarta.mail.Session;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.javamail.JavaMailSender;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;
import server.sassedo.common.service.email.BrandedEmailSender;
import server.sassedo.listing.common.ListingStatus;
import server.sassedo.promotion.common.ListingType;

import java.nio.charset.StandardCharsets;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ListingModerationEmailServiceTest {

    private static final String SENDER_EMAIL = "no-reply@sassedo.test";

    @Mock
    private JavaMailSender mailSender;

    private MimeMessage mimeMessage;
    private ListingModerationEmailService emailService;

    @BeforeEach
    void setUp() {
        mimeMessage = new MimeMessage(Session.getInstance(new Properties()));
        lenient().when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
        BrandedEmailSender emailSender =
                new BrandedEmailSender(mailSender, createTemplateEngine(), SENDER_EMAIL);
        emailService = new ListingModerationEmailService(emailSender, "https://sassedo.test/");
    }

    @Test
    void sendDecision_rendersApprovedRentalEmailWithPublicLinkAndEscapedValues() throws Exception {
        emailService.sendDecision(
                "owner@example.com",
                "Иван <script>alert('x')</script>",
                ListingType.RENTAL,
                42L,
                "Светъл дом <b>център</b>",
                ListingStatus.ACTIVE,
                null
        );

        mimeMessage.saveChanges();
        verify(mailSender).send(mimeMessage);
        assertEnvelope("owner@example.com", "Обявата ви е одобрена");

        String plainText = findTextPart(mimeMessage, "text/plain");
        String html = findTextPart(mimeMessage, "text/html");

        assertThat(plainText)
                .contains("беше одобрена и вече е активна")
                .contains("https://sassedo.test/listing/42");
        assertThat(html)
                .contains("Иван &lt;script&gt;alert(&#39;x&#39;)&lt;/script&gt;")
                .contains("Светъл дом &lt;b&gt;център&lt;/b&gt;")
                .doesNotContain("<script>")
                .contains("href=\"https://sassedo.test/listing/42\"")
                .contains("cid:sassedoLogo");
        assertLogoPart();
    }

    @Test
    void sendDecision_rendersRejectedRoommateEmailWithReasonAndEditLink() throws Exception {
        emailService.sendDecision(
                "owner@example.com",
                "Мария",
                ListingType.ROOMMATE,
                7L,
                "Търся съквартирант",
                ListingStatus.REJECTED,
                "Добавете снимки <img src=x onerror=alert(1)>"
        );

        mimeMessage.saveChanges();
        verify(mailSender).send(mimeMessage);
        assertEnvelope("owner@example.com", "Обявата ви не беше одобрена");

        String plainText = findTextPart(mimeMessage, "text/plain");
        String html = findTextPart(mimeMessage, "text/html");

        assertThat(plainText)
                .contains("Причина: Добавете снимки <img src=x onerror=alert(1)>")
                .contains("https://sassedo.test/dashboard/listings/roommate/7/edit");
        assertThat(html)
                .contains("Добавете снимки &lt;img src=x onerror=alert(1)&gt;")
                .doesNotContain("<img src=x onerror=alert(1)>")
                .contains("href=\"https://sassedo.test/dashboard/listings/roommate/7/edit\"")
                .contains("Редактирайте обявата");
        assertLogoPart();
    }

    @Test
    void sendDecision_rejectsNonModerationStatusWithoutSending() {
        assertThatThrownBy(() -> emailService.sendDecision(
                "owner@example.com",
                "Мария",
                ListingType.RENTAL,
                1L,
                "Обява",
                ListingStatus.PENDING,
                null
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("PENDING");

        verify(mailSender, never()).send(mimeMessage);
    }

    private void assertEnvelope(String recipient, String subject) throws Exception {
        InternetAddress from = (InternetAddress) mimeMessage.getFrom()[0];
        InternetAddress to = (InternetAddress) mimeMessage
                .getRecipients(Message.RecipientType.TO)[0];

        assertThat(from.getAddress()).isEqualTo(SENDER_EMAIL);
        assertThat(from.getPersonal()).isEqualTo("Sassedo");
        assertThat(to.getAddress()).isEqualTo(recipient);
        assertThat(mimeMessage.getSubject()).isEqualTo(subject);
        assertThat(mimeMessage.getContentType()).startsWith("multipart/mixed");
    }

    private void assertLogoPart() throws Exception {
        Part logoPart = findPartByContentId(mimeMessage, "<sassedoLogo>");

        assertThat(logoPart).isNotNull();
        assertThat(logoPart.getContentType()).startsWith("image/png");
        assertThat(logoPart.getDisposition()).isEqualTo(Part.INLINE);
    }

    private static String findTextPart(Part part, String mimeType) throws Exception {
        if (part.isMimeType(mimeType)) {
            return (String) part.getContent();
        }

        if (part.isMimeType("multipart/*")) {
            Multipart multipart = (Multipart) part.getContent();
            for (int index = 0; index < multipart.getCount(); index++) {
                String content = findTextPart(multipart.getBodyPart(index), mimeType);
                if (content != null) {
                    return content;
                }
            }
        }

        return null;
    }

    private static Part findPartByContentId(Part part, String contentId) throws Exception {
        String[] contentIds = part.getHeader("Content-ID");
        if (contentIds != null && contentIds.length > 0 && contentId.equals(contentIds[0])) {
            return part;
        }

        if (part.isMimeType("multipart/*")) {
            Multipart multipart = (Multipart) part.getContent();
            for (int index = 0; index < multipart.getCount(); index++) {
                Part match = findPartByContentId(multipart.getBodyPart(index), contentId);
                if (match != null) {
                    return match;
                }
            }
        }

        return null;
    }

    private static SpringTemplateEngine createTemplateEngine() {
        ClassLoaderTemplateResolver resolver = new ClassLoaderTemplateResolver();
        resolver.setPrefix("templates/");
        resolver.setSuffix(".html");
        resolver.setTemplateMode(TemplateMode.HTML);
        resolver.setCharacterEncoding(StandardCharsets.UTF_8.name());
        resolver.setCacheable(false);

        SpringTemplateEngine templateEngine = new SpringTemplateEngine();
        templateEngine.setTemplateResolver(resolver);
        return templateEngine;
    }
}
